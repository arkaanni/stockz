package dev.arkaan.repository.db

import dev.arkaan.domain.MetadataResult
import dev.arkaan.domain.StockImage
import dev.arkaan.domain.StockImageWithMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.useHandleUnchecked
import org.jdbi.v3.core.kotlin.withHandleUnchecked
import kotlin.jvm.optionals.getOrDefault

class MetadataJdbi(
    private val jdbi: Jdbi
) {
    fun insertResult(): Int = jdbi.withHandleUnchecked {
        it.createUpdate("insert into generate_result values (null)")
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Int::class.java)
            .one()
    }

    fun getLastResultId(): Int? = jdbi.withHandleUnchecked {
        it.createQuery(
            """
            select id from generate_result
            order by id desc
            limit 1
        """.trimIndent()
        )
            .mapTo(Int::class.java)
            .findOne()
            .orElse(null)
    }

    fun insert(images: Sequence<StockImageWithMetadata>, resultId: Int) {
        jdbi.open().use {
            val batch = it.prepareBatch(
                """
                insert into image_metadata (generate_id, file_name, title, description, first_category, 
                second_category, keywords, preview_id) values 
                (:generate_id, :file_name, :title, :description, :first_category, :second_category, :keywords, :preview_id)
            """.trimIndent()
            )
            images.forEach { img ->
                batch.apply {
                    bind("generate_id", resultId)
                    bind("file_name", img.data.fileName)
                    bind("title", img.data.title)
                    bind("description", img.data.description)
                    bind("first_category", img.data.firstCategory)
                    bind("second_category", img.data.secondaryCategory)
                    bind("keywords", img.data.keywords)
                    bind("preview_id", img.previewId)
                    add()
                }
            }
            batch.execute()
        }
    }

    fun getNextResultId(currentId: Int, isGreater: Boolean): Int {
        val order = if (isGreater) "asc" else "desc"
        val whereOperator = if (isGreater) ">" else "<"
        return jdbi.withHandleUnchecked {
            it.createQuery(
                """
                select id from generate_result
                where id $whereOperator $currentId
                order by id $order limit 1
            """.trimIndent()
            )
                .mapTo(Int::class.java)
                .findOne()
                .getOrDefault(currentId)
        }
    }

    fun getResultForExport(id: Int) = channelFlow {
        jdbi.open().use {
            val resultStream = it.createQuery("select * from image_metadata where generate_id = :id")
                .bind("id", id)
                .map { rs, _ ->
                    StockImage(
                        fileName = rs.getString("file_name"),
                        title = rs.getString("title"),
                        description = rs.getString("description"),
                        firstCategory = rs.getString("first_category"),
                        secondaryCategory = rs.getString("second_category"),
                        keywords = rs.getString("keywords")
                    )
                }
                .stream()
            resultStream.use { result ->
                result.iterator().run {
                    while (hasNext()) {
                        send(next())
                    }
                }
            }
        }
    }

    suspend fun getResult(id: Int): MetadataResult = withContext(Dispatchers.IO) {
        val nextId = async { getNextResultId(id, false) }
        val prevId = async { getNextResultId(id, true) }
        val images = channelFlow {
            jdbi.open().use {
                val resultStream = it.createQuery(
                    """
                            select m.*, p.id as preview_id 
                            from image_metadata m 
                            inner join previews p on m.preview_id = p.id 
                            where m.generate_id = :generate_id
                        """.trimIndent()
                )
                    .bind("generate_id", id)
                    .map { rs, _ ->
                        StockImageWithMetadata(
                            data = StockImage(
                                fileName = rs.getString("file_name"),
                                title = rs.getString("title"),
                                description = rs.getString("description"),
                                firstCategory = rs.getString("first_category"),
                                secondaryCategory = rs.getString("second_category"),
                                keywords = rs.getString("keywords")
                            ),
                            previewId = rs.getInt("preview_id")
                        )
                    }.stream()

                resultStream.use { result ->
                    val resultIter = result.iterator()
                    while (resultIter.hasNext()) {
                        send(resultIter.next())
                    }
                }
            }
        }
        MetadataResult(
            images = images, resultId = id,
            nextId = nextId.await(), previousId = prevId.await(),
        )
    }

    suspend fun delete(id: Int): Int = withContext(Dispatchers.IO) {
        jdbi.useHandleUnchecked {
            it.createUpdate("delete from generate_result where id = :id")
                .bind("id", id)
                .execute()
        }
        val nextIdAsync = async { getNextResultId(id, false) }
        val prevIdAsync = async { getNextResultId(id, true) }
        val nextId = nextIdAsync.await()
        val prevId = prevIdAsync.await()
        if (prevId != id) prevId else nextId
    }
}
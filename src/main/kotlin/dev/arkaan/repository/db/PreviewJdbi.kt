package dev.arkaan.repository.db

import dev.arkaan.domain.ImagePreview
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.withHandleUnchecked

class PreviewJdbi(
    private val jdbi: Jdbi,
) {
    fun getPreviewBlob(id: Int): ByteArray? = jdbi.withHandleUnchecked {
        it.createQuery("select blob from previews where id = :id")
            .bind("id", id)
            .mapTo(ByteArray::class.java)
            .findOne()
            .orElse(null)
    }

    fun getPreview(id: Int): ImagePreview? = jdbi.withHandleUnchecked {
        it.createQuery("select * from previews where id = :id")
            .bind("id", id)
            .map { rs, _ ->
                ImagePreview(
                    id,
                    rs.getBytes("blob") ?: ByteArray(0),
                    rs.getString("extension")
                )
            }
            .findOne()
            .orElse(null)
    }

    fun checkIfPreviewExist(filePath: String): Pair<Int, ByteArray?>? = jdbi.withHandleUnchecked {
        it.createQuery("select id, blob from previews where path = :path")
            .bind("path", filePath)
            .map { rs, _ -> Pair(rs.getInt("id"), rs.getBytes("blob")) }
            .findOne()
            .orElse(null)
    }

    fun insertPreview(filePath: String, blob: ByteArray?, extension: String): Int = jdbi.withHandleUnchecked {
        it.createUpdate(
            """
                insert into previews (path, blob, extension)
                values (:path, :blob, :extension)
                """.trimIndent()
        )
            .bind("path", filePath)
            .bind("blob", blob)
            .bind("extension", extension)
            .executeAndReturnGeneratedKeys("id")
            .mapTo(Int::class.java)
            .one()
    }

    fun updatePreview(id: Int, blob: ByteArray?) = jdbi.withHandleUnchecked {
        it.createUpdate(
            """
            update previews set blob = :blob
            where id = :id
        """.trimIndent()
        )
            .bind("id", id)
            .bind("blob", blob)
            .execute()

    }
}
@echo off

if exist dist rmdir /s /q dist

:: 1. Build the Fat JAR with Maven
call mvn clean package

:: 2. Package the app with JPackage
echo Building executable...
jpackage ^
  --type app-image ^
  --name Stockz ^
  --input target\fat ^
  --main-jar stockz-1.0.0-fat.jar ^
  --main-class dev.arkaan.Launcher ^
  --dest dist

echo gemini.api.key= > dist\Stockz\app.properties

echo Build Complete! Output located in dist\
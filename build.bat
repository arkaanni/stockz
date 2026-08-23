@echo off

if exist dist rmdir /s /q dist

:: 1. Build the Fat JAR with Maven
call mvn clean package

:: 2. Package the app with JPackage
jpackage ^
  --type app-image ^
  --name Stockz ^
  --input target ^
  --main-jar stockz-1.0.0-fat.jar ^
  --main-class dev.arkaan.Launcher ^
  --dest dist

echo Build Complete! Output located in dist/
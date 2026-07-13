#!/bin/bash
set -e

STS="E:/SteamLibrary/steamapps/common/SlayTheSpire"
WS="E:/SteamLibrary/steamapps/workshop/content/646570"
BUILD_DIR="build"
CLASSES_DIR="${BUILD_DIR}/classes"
JAR_DIR="${BUILD_DIR}/jar"

echo "=== StatTracker Build ==="

rm -rf "${CLASSES_DIR}" "${JAR_DIR}" "${BUILD_DIR}/StatTracker.jar"
mkdir -p "${CLASSES_DIR}" "${JAR_DIR}/META-INF"

JDK8="/tmp/jdk8_extract/jdk8u482-b08"
echo "[1/3] Compiling..."
CP="${STS}/desktop-1.0.jar;${WS}/1605060445/ModTheSpire.jar;${WS}/1605833019/BaseMod.jar;${WS}/1609158507/StSLib.jar;${WS}/2384072973/TogetherInSpire.jar"
find src/main/java -name "*.java" > "${BUILD_DIR}/sources.txt"
"${JDK8}/bin/javac" -g -encoding UTF-8 -source 1.8 -target 1.8 -d "${CLASSES_DIR}" -cp "${CP}" @"${BUILD_DIR}/sources.txt"

echo "[2/3] Packaging..."
echo "Manifest-Version: 1.0" > "${JAR_DIR}/META-INF/MANIFEST.MF"
cp -r "${CLASSES_DIR}"/* "${JAR_DIR}/"
cp src/main/resources/ModTheSpire.json "${JAR_DIR}/"
powershell -Command "Compress-Archive -Path '${JAR_DIR}/*' -DestinationPath '${BUILD_DIR}/StatTracker.zip' -Force"
mv "${BUILD_DIR}/StatTracker.zip" "${BUILD_DIR}/StatTracker.jar"

echo "[3/3] Done!"
ls -la "${BUILD_DIR}/StatTracker.jar"
echo "=== Build complete ==="

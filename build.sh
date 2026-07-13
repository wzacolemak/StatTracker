#!/bin/bash
set -e

# 从 .env 读本地路径(不会自动读,bash 需手动 source)
# .env 不在仓库里,见 .env.example。
ENV_FILE="$(dirname "$0")/.env"
if [ -f "$ENV_FILE" ]; then
    set -a
    . "$ENV_FILE"
    set +a
fi

# 各路径有默认值,没配 .env 也能试(路径不对编译会报错)
STS="${STS_GAME:-E:/SteamLibrary/steamapps/common/SlayTheSpire}"
WS="${STS_WORKSHOP:-E:/SteamLibrary/steamapps/workshop/content/646570}"
JDK8="${STS_JDK8:-/tmp/jdk8_extract/jdk8u482-b08}"

if [ ! -x "${JDK8}/bin/javac.exe" ] && [ ! -x "${JDK8}/bin/javac" ]; then
    echo "找不到 JDK8 的 javac: ${JDK8}"
    echo "请在 .env 里配置 STS_JDK8(参考 .env.example)"
    exit 1
fi
JAVAC_BIN="javac"
[ -x "${JDK8}/bin/javac.exe" ] && JAVAC_BIN="${JDK8}/bin/javac.exe"
[ -x "${JDK8}/bin/javac" ] && JAVAC_BIN="${JDK8}/bin/javac"

BUILD_DIR="build"
CLASSES_DIR="${BUILD_DIR}/classes"
JAR_DIR="${BUILD_DIR}/jar"

echo "=== StatTracker Build ==="
echo "  JDK8:     ${JDK8}"
echo "  游戏:      ${STS}"
echo "  创意工坊: ${WS}"

rm -rf "${CLASSES_DIR}" "${JAR_DIR}" "${BUILD_DIR}/StatTracker.jar"
mkdir -p "${CLASSES_DIR}" "${JAR_DIR}/META-INF"

echo "[1/3] Compiling..."
CP="${STS}/desktop-1.0.jar;${WS}/1605060445/ModTheSpire.jar;${WS}/1605833019/BaseMod.jar;${WS}/1609158507/StSLib.jar;${WS}/2384072973/TogetherInSpire.jar"
find src/main/java -name "*.java" > "${BUILD_DIR}/sources.txt"
"${JAVAC_BIN}" -g -encoding UTF-8 -source 1.8 -target 1.8 -d "${CLASSES_DIR}" -cp "${CP}" @"${BUILD_DIR}/sources.txt"

echo "[2/3] Packaging..."
echo "Manifest-Version: 1.0" > "${JAR_DIR}/META-INF/MANIFEST.MF"
cp -r "${CLASSES_DIR}"/* "${JAR_DIR}/"
cp src/main/resources/ModTheSpire.json "${JAR_DIR}/"
powershell -Command "Compress-Archive -Path '${JAR_DIR}/*' -DestinationPath '${BUILD_DIR}/StatTracker.zip' -Force"
mv "${BUILD_DIR}/StatTracker.zip" "${BUILD_DIR}/StatTracker.jar"

echo "[3/3] Done!"
ls -la "${BUILD_DIR}/StatTracker.jar"
echo "=== Build complete ==="

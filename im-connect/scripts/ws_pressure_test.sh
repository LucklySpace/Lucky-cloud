#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAR_PATH="${ROOT_DIR}/im-connect-pressure/target/im-connect-pressure.jar"
BUILD_FIRST="false"

print_help() {
  cat <<'EOF'
用法:
  ./scripts/ws_pressure_test.sh [--build] [压测参数...]

说明:
  - 默认直接运行已打包好的 jar
  - 加 --build 会先执行 Maven 打包
  - 除 --build 外，其它参数会原样透传给压测程序
  - 可通过环境变量 JAVA_OPTS 覆盖 JVM 参数

示例:
  ./scripts/ws_pressure_test.sh --build \
    --url=ws://127.0.0.1:19000/im \
    --protocol=proto \
    --authMode=url \
    --token=YOUR_JWT \
    --connections=200 \
    --connectRate=50 \
    --durationSeconds=180 \
    --heartbeatIntervalMs=25000

  ./scripts/ws_pressure_test.sh \
    --protocol=json \
    --authMode=header \
    --tokenFile=/absolute/path/tokens.txt \
    --connections=500 \
    --connectRate=120 \
    --durationSeconds=600

  JAVA_OPTS="-Xms3g -Xmx3g -XX:+UseG1GC -XX:+AlwaysPreTouch" \
  ./scripts/ws_pressure_test.sh --connections=30000 --connectRate=3000
EOF
}

if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  print_help
  exit 0
fi

ARGS=()
for arg in "$@"; do
  if [[ "$arg" == "--build" ]]; then
    BUILD_FIRST="true"
  else
    ARGS+=("$arg")
  fi
done

if [[ "$BUILD_FIRST" == "true" ]]; then
  mvn -pl im-connect/im-connect-pressure -am -DskipTests package
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "未找到压测包: $JAR_PATH"
  echo "请先执行: ./scripts/ws_pressure_test.sh --build [参数...]"
  exit 1
fi

JAVA_OPTS_VALUE="${JAVA_OPTS:--Xms2g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+AlwaysPreTouch}"
java ${JAVA_OPTS_VALUE} -jar "$JAR_PATH" "${ARGS[@]}"

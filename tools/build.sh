#!/usr/bin/env bash
# Build the plugin jar without installing Java: runs the Gradle wrapper in a JDK 17 container.
# Output: build/libs/vme-insight-plugin-<version>.jar
#
#   tools/build.sh            # test + build
#   tools/build.sh shadowJar  # any other Gradle tasks
set -euo pipefail
cd "$(dirname "$0")/.."
IMAGE="${BUILD_IMAGE:-eclipse-temurin:17-jdk}"
tasks=("$@")
[ ${#tasks[@]} -eq 0 ] && tasks=(clean test shadowJar)
# Gradle's cache lives in a named volume; Docker creates it root-owned, so hand it to us first.
docker run --rm -u 0 -v vme-insight-plugin-gradle:/cache "$IMAGE" chown "$(id -u):$(id -g)" /cache
docker run --rm \
  -u "$(id -u):$(id -g)" \
  -e GRADLE_USER_HOME=/cache \
  -v vme-insight-plugin-gradle:/cache \
  -v "$PWD":/src -w /src \
  "$IMAGE" ./gradlew --no-daemon -q "${tasks[@]}"
ls -l build/libs/

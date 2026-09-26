#!/bin/bash
#
# Builds a test module in production mode and stages it, with a Dockerfile,
# as the context of the pull request preview image.
#
# Flow itself has to be installed in the local repository already. The module
# is packaged by invoking the packaging goal directly rather than through the
# package phase, and in the same Maven session as build-frontend: the build
# info file that switches the application to production mode is deleted when
# that session ends, and the package lifecycle would run prepare-frontend
# again after build-frontend. A war is staged exploded for Jetty, a jar with
# its runtime dependencies as the class path of a Spring Boot application.
#
# Usage: scripts/previewPackage.sh <module> <context path> <output dir>

set -euo pipefail

module=${1:-}
contextPath=${2:-}
out=${3:-}

if [ -z "$module" ] || [ -z "$out" ]
then
	echo "Usage: $0 <module> <context path> <output dir>"
	exit 1
fi

evaluate() {
	mvn -B -q -f "$module/pom.xml" help:evaluate -Dexpression="$1" -DforceStdout
}

target="$module/target"
finalName=$(evaluate project.build.finalName)
build="mvn -B -ntp -f $module/pom.xml -DskipTests compile com.vaadin:flow-maven-plugin:build-frontend"

rm -rf "$out" "$target/preview-lib"
mkdir -p "$out"

if grep -q '<packaging>war</packaging>' "$module/pom.xml"
then
	$build war:exploded
	jettyVersion=$(evaluate jetty.version)
	# Jetty deploys webapps/ROOT at / and webapps/<name> at /<name>
	webapp="ROOT"
	[ -n "$contextPath" ] && webapp="${contextPath#/}"
	mkdir -p "$out/webapps"
	cp -R "$target/$finalName" "$out/webapps/$webapp"
	cat > "$out/Dockerfile" <<EOF
FROM jetty:$jettyVersion-jdk21-alpine
# The JVM would otherwise take a quarter of the small preview machine
ENV JAVA_OPTIONS=-XX:MaxRAMPercentage=75
RUN java -jar "\$JETTY_HOME/start.jar" \\
    --add-modules=ee10-deploy,ee10-annotations,ee10-websocket-jakarta
COPY --chown=jetty:jetty webapps/ webapps/
EOF
else
	mainClass=$(grep -rl '@SpringBootApplication' "$module/src/main/java" \
		| head -1 | sed -e "s|^$module/src/main/java/||" -e 's|\.java$||' -e 's|/|.|g')
	if [ -z "$mainClass" ]
	then
		echo "No @SpringBootApplication class found in $module"
		exit 1
	fi
	# flow-tests adds slf4j-simple for Jetty, which clashes with the Logback
	# that Spring Boot brings
	$build jar:jar dependency:copy-dependencies -DincludeScope=runtime \
		-DexcludeArtifactIds=slf4j-simple -DoutputDirectory="$PWD/$target/preview-lib"
	mkdir -p "$out/app"
	cp "$target/$finalName.jar" "$out/app/app.jar"
	cp -R "$target/preview-lib" "$out/app/lib"
	cat > "$out/Dockerfile" <<EOF
FROM eclipse-temurin:21-jre-alpine
COPY app/ /app/
EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=75", "-cp", "/app/app.jar:/app/lib/*", \\
    "$mainClass", "--server.port=8080"]
EOF
fi

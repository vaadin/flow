#!/bin/bash
# Proxy authentication workaround for Claude Code environments
# This script configures Maven and Gradle to use proxy credentials
# from environment variables when running in Claude Code

# Only run if we're in Claude Code environment
if [ "$CLAUDECODE" != "1" ]; then
    exit 0
fi

# Get proxy URL from environment
PROXY_URL="${https_proxy:-$HTTPS_PROXY}"

# Exit if no proxy is configured
if [ -z "$PROXY_URL" ]; then
    exit 0
fi

# Parse proxy URL to extract components
# Format: http://username:password@host:port
parse_proxy() {
    local url="$1"
    # Remove protocol prefix
    url="${url#http://}"
    url="${url#https://}"

    # Extract credentials if present
    if [[ "$url" == *"@"* ]]; then
        local credentials="${url%@*}"
        local hostport="${url##*@}"
        PROXY_USER="${credentials%:*}"
        PROXY_PASS="${credentials#*:}"
    else
        local hostport="$url"
        PROXY_USER=""
        PROXY_PASS=""
    fi

    # Extract host and port
    PROXY_HOST="${hostport%:*}"
    PROXY_PORT="${hostport##*:}"
}

parse_proxy "$PROXY_URL"

# Configure Maven settings.xml
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <proxies>
    <proxy>
      <id>https-proxy</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>${PROXY_HOST}</host>
      <port>${PROXY_PORT}</port>
      <username>${PROXY_USER}</username>
      <password>${PROXY_PASS}</password>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
    <proxy>
      <id>http-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>${PROXY_HOST}</host>
      <port>${PROXY_PORT}</port>
      <username>${PROXY_USER}</username>
      <password>${PROXY_PASS}</password>
      <nonProxyHosts>localhost|127.0.0.1</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
EOF

# Configure Maven 3.9+ compatibility
cat > ~/.mavenrc << 'EOF'
MAVEN_OPTS="$MAVEN_OPTS -Dmaven.resolver.transport=wagon"
EOF

# Configure Gradle
mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << EOF
systemProp.https.proxyHost=${PROXY_HOST}
systemProp.https.proxyPort=${PROXY_PORT}
systemProp.https.proxyUser=${PROXY_USER}
systemProp.https.proxyPassword=${PROXY_PASS}
systemProp.http.proxyHost=${PROXY_HOST}
systemProp.http.proxyPort=${PROXY_PORT}
systemProp.http.proxyUser=${PROXY_USER}
systemProp.http.proxyPassword=${PROXY_PASS}
EOF

echo "Proxy authentication configured for Maven and Gradle" >&2

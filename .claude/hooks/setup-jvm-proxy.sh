#!/bin/bash
# Auto-configure Maven/Gradle proxy auth in Claude Code environments.
# Runs as a SessionStart hook. No-ops when not needed.

[ "$CLAUDECODE" = "1" ] || exit 0

proxy="${https_proxy:-$HTTPS_PROXY}"
[ -z "$proxy" ] && exit 0
echo "$proxy" | grep -q '@' || exit 0
[ -f ~/.m2/settings.xml ] && exit 0

rest="${proxy#*://}"
userpass="${rest%@*}"
hostport="${rest##*@}"
user="${userpass%%:*}"
pass="${userpass#*:}"
host="${hostport%%:*}"
port="${hostport##*:}"
port="${port%/}"

mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<settings>
  <proxies>
    <proxy>
      <id>ccw</id><active>true</active><protocol>https</protocol>
      <host>$host</host><port>$port</port>
      <username>$user</username>
      <password><![CDATA[$pass]]></password>
    </proxy>
  </proxies>
</settings>
EOF

mkdir -p ~/.gradle
cat > ~/.gradle/gradle.properties << EOF
systemProp.https.proxyHost=$host
systemProp.https.proxyPort=$port
systemProp.https.proxyUser=$user
systemProp.https.proxyPassword=$pass
systemProp.http.proxyHost=$host
systemProp.http.proxyPort=$port
systemProp.http.proxyUser=$user
systemProp.http.proxyPassword=$pass
EOF

echo "Configured Maven/Gradle proxy from HTTPS_PROXY" >&2

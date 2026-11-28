#!/bin/sh
set -eu
./mvnw -B -ntp test-compile dependency:build-classpath -Dmdep.outputFile=target/test-classpath -DincludeScope=test
java -cp "target/test-classes:target/classes:$(cat target/test-classpath)" org.whitesborobandboosters.rallyboro.PilotUiServer > target/ui-server.log 2>&1 &
server_pid=$!
trap 'kill "$server_pid" 2>/dev/null || true' EXIT HUP INT TERM
ready=false
for attempt in $(seq 1 60); do
    if curl -fsS http://127.0.0.1:18080/actuator/health/readiness > /dev/null 2>&1; then ready=true;break;fi
    kill -0 "$server_pid" 2>/dev/null || { echo 'UI test server failed; inspect target/ui-server.log locally.' >&2;exit 1; }
    sleep 1
done
[ "$ready" = true ] || { echo 'UI test server timed out.' >&2;exit 1; }
export PATH="$(pwd)/target/frontend-tools/node:$PATH"
cd frontend
RALLYBORO_TEST_BASE_URL=http://127.0.0.1:18080 RALLYBORO_TEST_CREDENTIALS_FILE=../target/ui-credentials npm run test:e2e

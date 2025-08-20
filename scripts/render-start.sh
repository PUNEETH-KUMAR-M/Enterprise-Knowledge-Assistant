#!/usr/bin/env sh
set -eu

# If SPRING_DATASOURCE_URL not set but DATABASE_URL provided by Render,
# parse and export JDBC-compatible variables.
if [ -z "${SPRING_DATASOURCE_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  # Expected: postgres://user:pass@host:port/db
  proto="$(printf "%s" "$DATABASE_URL" | sed -E 's#^(.*)://.*#\1#')"
  rest="$(printf "%s" "$DATABASE_URL" | sed -E 's#^[^:]+://##')"
  creds="$(printf "%s" "$rest" | cut -d@ -f1)"
  hostpart="$(printf "%s" "$rest" | cut -d@ -f2)"
  user="$(printf "%s" "$creds" | cut -d: -f1)"
  pass="$(printf "%s" "$creds" | cut -d: -f2)"
  host="$(printf "%s" "$hostpart" | cut -d: -f1)"
  port_db="$(printf "%s" "$hostpart" | cut -d: -f2)"
  port="$(printf "%s" "$port_db" | cut -d/ -f1)"
  db="$(printf "%s" "$port_db" | cut -d/ -f2)"

  if [ "$proto" = "postgres" ] || [ "$proto" = "postgresql" ]; then
    export SPRING_DATASOURCE_URL="jdbc:postgresql://$host:$port/$db"
    export SPRING_DATASOURCE_USERNAME="$user"
    export SPRING_DATASOURCE_PASSWORD="$pass"
  fi
fi

PORT="${PORT:-8080}"
JAVA_OPTS="${JAVA_OPTS:-}"

exec sh -c "java $JAVA_OPTS -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar --server.port=${PORT}"

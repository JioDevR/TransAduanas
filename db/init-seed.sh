#!/bin/bash
set -euo pipefail
# El hash se suministra externamente. No imprimirlo ni activar trazas de shell.
if ! printf '%s' "${SGA_SEED_PASSWORD_HASH:-}" | grep -Eq '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$'; then
    echo 'Configure SGA_SEED_PASSWORD_HASH con un hash BCrypt válido.' >&2
    exit 1
fi
# El formato validado no contiene comillas ni metacaracteres SQL.
{
    printf "SET @SGA_SEED_PASSWORD_HASH = '%s';\n" "$SGA_SEED_PASSWORD_HASH"
    cat /opt/sga/seed.sql
} | MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot "$MYSQL_DATABASE"

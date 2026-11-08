#!/usr/bin/env bash
# Respaldo de la base (RNF14). Copia la base con pg_dump, prueba restaurarla en una base vacía, compara las filas de
# cada tabla y deja la copia cifrada con AES-256. Si cualquier paso falla, termina con error y no deja archivo.
# Lo usa .github/workflows/respaldo.yml; también corre en local con Git Bash.
#
# Variables:
#   ORIGEN_URL      base a respaldar (postgresql://usuario:contraseña@host/base?sslmode=require)
#   PRUEBA_URL      base vacía donde se prueba la restauración; queda con los datos restaurados
#   RESPALDO_CLAVE  frase con la que se cifra la copia
#   ARCHIVO         archivo cifrado que se genera (.dump.gpg)
#   PG_BIN          opcional: carpeta de pg_dump, pg_restore y psql
set -euo pipefail

: "${ORIGEN_URL:?Falta ORIGEN_URL}" "${PRUEBA_URL:?Falta PRUEBA_URL}"
: "${RESPALDO_CLAVE:?Falta RESPALDO_CLAVE}" "${ARCHIVO:?Falta ARCHIVO}"
bin=${PG_BIN:+$PG_BIN/}

trabajo=$(mktemp -d)
trap 'rm -rf "$trabajo"' EXIT
copia="$trabajo/respaldo.dump"

echo "1/4 Copiando la base"
"${bin}pg_dump" --format=custom --no-owner --no-acl --dbname="$ORIGEN_URL" --file="$copia"

echo "2/4 Restaurando la copia en una base vacía"
"${bin}pg_restore" --no-owner --no-acl --exit-on-error --dbname="$PRUEBA_URL" "$copia"

# Imprime «tabla filas» de cada tabla del esquema public, ordenado. Una sola consulta por base.
filas() {
	printf '%s\n\gexec\n' "select string_agg(format('select %L, count(*) from public.%I', tablename, tablename), ' union all ')
		from pg_tables where schemaname = 'public'" | "${bin}psql" -X -q -At -F ' ' --dbname="$1" | sort
}

echo "3/4 Comparando las filas de cada tabla"
filas "$ORIGEN_URL" > "$trabajo/origen.txt"
filas "$PRUEBA_URL" > "$trabajo/restaurada.txt"
grep -q '^flyway_schema_history [1-9]' "$trabajo/origen.txt" || { echo "La base de origen no tiene el esquema de Flyway." >&2; exit 1; }
if ! diff -q "$trabajo/origen.txt" "$trabajo/restaurada.txt" > /dev/null; then
	echo "La base restaurada no tiene las mismas filas que la de origen." >&2
	exit 1
fi
# No se imprimen los conteos: el registro del workflow es público.
echo "    $(wc -l < "$trabajo/origen.txt" | tr -d ' ') tablas con las mismas filas"

echo "4/4 Cifrando la copia"
cifrar() { gpg --batch --yes --quiet --no-symkey-cache --pinentry-mode loopback --passphrase-fd 3 "$@" 3<<< "$RESPALDO_CLAVE"; }
cifrar --symmetric --cipher-algo AES256 --compress-algo none --output "$ARCHIVO" "$copia"
cifrar --decrypt --output "$trabajo/descifrada.dump" "$ARCHIVO"
cmp -s "$copia" "$trabajo/descifrada.dump" || { rm -f "$ARCHIVO"; echo "La copia cifrada no se pudo descifrar." >&2; exit 1; }

echo "Listo: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"

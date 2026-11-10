#!/bin/bash
# Abrir una incidencia por cada cosa pendiente, en el repositorio que le corresponde.
#
# Por qué existe: lo pendiente que vive en un chat se pierde. Lo que vive en la lista de
# incidencias del repositorio se ve, se puede cerrar y no se olvida. El aviso del 22 % de
# batería o del `LazyColumn` anidado tenía que haber sido una incidencia antes de ser un fallo.
#
# Uso:  ./04-incidencias.sh            (lee incidencias.txt del directorio actual)
#
# Formato de incidencias.txt — una por bloque, separadas por una línea `---`:
#
#   repo: Alex17273747576777/gltools-reborn
#   titulo: La Home no tiene transición entre pestañas
#   etiquetas: diseno, pendiente
#   cuerpo: |
#     Texto largo, con lo que hace falta para reproducirlo o comprobarlo.
#   ---
set -euo pipefail
TOKEN="$(head -1 "$HOME/.gh-tokens")"
FICHERO="${1:-incidencias.txt}"
[ -f "$FICHERO" ] || { echo "falta $FICHERO"; exit 1; }

# Partir el fichero en bloques
awk 'BEGIN{RS="\n---\n"} {print > ("/tmp/inc-" NR ".txt")}' "$FICHERO"
abiertas=0
for bloque in /tmp/inc-*.txt; do
  repo=$(grep -m1 '^repo:' "$bloque" | sed 's/^repo:[[:space:]]*//')
  titulo=$(grep -m1 '^titulo:' "$bloque" | sed 's/^titulo:[[:space:]]*//')
  etiquetas=$(grep -m1 '^etiquetas:' "$bloque" | sed 's/^etiquetas:[[:space:]]*//')
  [ -z "$repo" ] || [ -z "$titulo" ] && { rm -f "$bloque"; continue; }
  # cuerpo: todo lo que va después de `cuerpo: |`, sin el margen de indentación
  cuerpo=$(awk '/^cuerpo: \|/{f=1;next} f{sub(/^  /,"");print}' "$bloque")
  # no duplicar: si ya hay una abierta con el mismo título, se salta
  ya=$(curl -s -H "Authorization: Bearer $TOKEN" \
    "https://api.github.com/repos/$repo/issues?state=open&per_page=100" \
    | python3 -c "import json,sys;print(sum(1 for i in json.load(sys.stdin) if i['title']=='''$titulo'''))" 2>/dev/null || echo 0)
  if [ "${ya:-0}" != "0" ]; then echo "  = ya existe: $titulo"; rm -f "$bloque"; continue; fi
  python3 - "$TOKEN" "$repo" "$titulo" "$cuerpo" "$etiquetas" <<'PY'
import json, sys, urllib.request, urllib.error
tok, repo, titulo, cuerpo, etiquetas = sys.argv[1:6]
et = [e.strip() for e in etiquetas.split(",") if e.strip()]
def post(datos):
    r = urllib.request.Request(f"https://api.github.com/repos/{repo}/issues",
        data=json.dumps(datos).encode(), method="POST",
        headers={"Authorization": "Bearer " + tok, "Accept": "application/vnd.github+json", "User-Agent": "inc"})
    return json.load(urllib.request.urlopen(r))
try:
    i = post({"title": titulo, "body": cuerpo, "labels": et})
except urllib.error.HTTPError:
    # las etiquetas pueden no existir todavía: se abre sin ellas antes que no abrirla
    i = post({"title": titulo, "body": cuerpo})
print(f"  + {repo}#{i['number']}  {titulo}")
PY
  abiertas=$((abiertas+1))
  rm -f "$bloque"
done
echo "$abiertas incidencias abiertas"

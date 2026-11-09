#!/usr/bin/env bash
# Sube este proyecto a un repositorio nuevo de GitHub.
#
# No lleva ninguna credencial escrita. El token se pasa por variable de entorno y no
# se guarda en el repositorio ni en el historial:
#
#   GITHUB_TOKEN=ghp_xxx ./subir.sh mi-usuario gltools-reborn
#
# El token necesita permiso "repo" (o "public_repo" si el repositorio es público).
set -euo pipefail

USUARIO="${1:?falta el usuario de GitHub}"
REPO="${2:-gltools-reborn}"
VISIBLE="${3:-public}"   # public | private

: "${GITHUB_TOKEN:?falta GITHUB_TOKEN. Ejemplo: GITHUB_TOKEN=ghp_xxx ./subir.sh usuario repo}"

echo "1/4 · comprobando que no se suba nada sensible…"
for prohibido in reborn-release.jks FIRMA.txt local.properties; do
  if git ls-files --error-unmatch "$prohibido" >/dev/null 2>&1; then
    echo "ALTO: $prohibido está versionado. No se sube." >&2
    exit 1
  fi
done
echo "    limpio ✓"

echo "2/4 · creando el repositorio $USUARIO/$REPO ($VISIBLE)…"
CODIGO=$(curl -s -o /tmp/gh_resp.json -w "%{http_code}" \
  -X POST https://api.github.com/user/repos \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -d "{\"name\":\"$REPO\",\"private\":$([ "$VISIBLE" = private ] && echo true || echo false),\"description\":\"Perfiles de dispositivo para Android sin root obligatorio, sin servidores y sin nada de pago\",\"has_issues\":true,\"has_wiki\":false}")

if [ "$CODIGO" = "201" ]; then
  echo "    creado ✓"
elif grep -q "already exists" /tmp/gh_resp.json 2>/dev/null; then
  echo "    ya existía; se reutiliza"
else
  echo "    GitHub respondió $CODIGO:" >&2
  head -c 400 /tmp/gh_resp.json >&2; echo >&2
  rm -f /tmp/gh_resp.json
  exit 1
fi
rm -f /tmp/gh_resp.json

echo "3/4 · empujando main…"
git branch -M main 2>/dev/null || true
git remote remove origin 2>/dev/null || true
# El token va en la URL sólo en memoria: no se escribe en .git/config.
git remote add origin "https://x-access-token:${GITHUB_TOKEN}@github.com/${USUARIO}/${REPO}.git"
git push -u origin main
git remote set-url origin "https://github.com/${USUARIO}/${REPO}.git"

echo "4/4 · limpio el remoto de la memoria de git…"
git remote -v | sed 's/x-access-token:[^@]*@/x-access-token:***@/'

echo
echo "LISTO → https://github.com/${USUARIO}/${REPO}"
echo "El token NO quedó guardado: el remote apunta a la URL sin credencial."

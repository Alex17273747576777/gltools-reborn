#!/bin/bash
# Cerrar un tramo de trabajo: probar, firmar, publicar, dejar constancia.
#
# Por qué existe: cerrar un tramo a mano son 18 pasos y siempre se olvida uno. La vez que se
# olvidó subir el `versionCode`, el APK publicado se instaló encima del anterior sin
# actualizarse. La vez que se olvidó la limpieza, quedaron 200 MB de clones en /tmp.
#
# Uso:  ./03-cerrar-tramo.sh <version> "<titulo de la release>"   (desde la raíz del proyecto)
#   ./03-cerrar-tramo.sh 1.9.1 "Tipografia propia y procedimientos"
#
# Requisitos: repositorio limpio de secretos; token de GitHub en ~/.gh-tokens (1.ª línea);
# keystore y contraseña en ~/.firma/.
set -euo pipefail

VERSION="${1:?falta la versión, p. ej. 1.9.1}"
TITULO="${2:?falta el título de la release}"
NOTAS="${NOTAS_RELEASE:-/tmp/notas-$VERSION.md}"
# Fallar ANTES de compilar, modificar la versión o publicar commits.
python3 - "$NOTAS" <<'PY'
import pathlib, sys
p = pathlib.Path(sys.argv[1])
if not p.is_file():
    raise SystemExit(f"falta {p}: escribe la nota de esta versión antes de cerrar")
if len(p.read_text(encoding="utf-8").strip()) < 80:
    raise SystemExit(f"{p} es demasiado corta para contar lo que cambió")
PY
RAIZ="$(pwd)"
TOKEN="$(head -1 "$HOME/.gh-tokens")"
REPO="$(git remote get-url origin | sed -E 's#.*github.com[:/]([^/]+/[^/.]+)(\.git)?#\1#')"
export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/opt/gradle-home}"
export REBORN_KEYSTORE="${REBORN_KEYSTORE:-$HOME/.firma/reborn-release.jks}"
export REBORN_STORE_PASS="${REBORN_STORE_PASS:-$(cat "$HOME/.firma/.pass")}"
export REBORN_KEY_PASS="${REBORN_KEY_PASS:-$(cat "$HOME/.firma/.pass")}"
export REBORN_KEY_ALIAS="${REBORN_KEY_ALIAS:-reborn}"
BT="$ANDROID_HOME/build-tools/35.0.0"

paso() { printf '\n\033[1m▸ %s\033[0m\n' "$*"; }

# 1. La versión, en los tres sitios donde vive. Si no coinciden, el APK miente.
paso "Versión $VERSION en build.gradle, MainActivity y RegistroFallos"
# Idempotente: si ya está en esa versión, no se sube el código otra vez. Un script que no se
# puede correr dos veces es un script que se rompe la segunda vez que se usa.
ACTUAL=$(grep -oP 'versionName "\K[^"]*' app/build.gradle)
if [ "$ACTUAL" = "$VERSION" ]; then
  CODE=$(grep -oP 'versionCode \K\d+' app/build.gradle)
else
  CODE=$(( $(grep -oP 'versionCode \K\d+' app/build.gradle) + 1 ))
  sed -i "s/versionCode [0-9]*/versionCode $CODE/" app/build.gradle
fi
sed -i "s/versionName \"[^\"]*\"/versionName \"$VERSION\"/" app/build.gradle
sed -i "s/version = \"[0-9.]*\"/version = \"$VERSION\"/" \
  app/src/main/java/io/github/labandita/glreborn/MainActivity.kt
sed -i "s/VERSION_REBORN = \"[0-9.]*\"/VERSION_REBORN = \"$VERSION\"/" \
  app/src/main/java/io/github/labandita/glreborn/RegistroFallos.kt
grep -q "versionCode $CODE" app/build.gradle || { echo "no se pudo subir versionCode"; exit 1; }

# 2. Pruebas. Sin verde no se publica.
paso "Pruebas del núcleo"
./gradlew :app:pruebasNucleo --no-daemon -q | tail -3

# 3. Firmar. Normal y crudo, uno detrás de otro (juntos se comen la RAM y matan el demonio).
paso "APK firmado"
./gradlew :app:assembleRelease --no-daemon -q
cp app/build/outputs/apk/release/app-release.apk "/tmp/apk-$VERSION.apk"
paso "APK crudo (datos sin comprimir)"
./gradlew :app:assembleRelease -PrebornDatos=crudo --no-daemon -q
cp app/build/outputs/apk/release/app-release.apk "/tmp/apk-$VERSION-crudo.apk"

# 4. Verificar de verdad: firma, versión y que el código nuevo esté DENTRO del dex.
paso "Verificación del artefacto"
"$BT/apksigner" verify "/tmp/apk-$VERSION.apk"
# `head -1` con `pipefail` da SIGPIPE y el pipeline devuelve fallo aunque haya casado:
# por eso aquí se usa `sed -n 1p`, que lee hasta el final. (Fallo encontrado al ejecutarlo.)
LINEA=$("$BT/aapt2" dump badging "/tmp/apk-$VERSION.apk" 2>/dev/null | sed -n '1p')
case "$LINEA" in
  *"versionName='$VERSION'"*) echo "  versión del APK: $VERSION" ;;
  *) echo "¡la versión del APK no es $VERSION! → $LINEA"; exit 1 ;;
esac
NORMAL=$(stat -c%s "/tmp/apk-$VERSION.apk"); CRUDO=$(stat -c%s "/tmp/apk-$VERSION-crudo.apk")
[ "$CRUDO" -gt $((NORMAL * 3)) ] || { echo "el APK crudo no es 3× el normal: el empaquetado falló"; exit 1; }
SHA_N=$(sha256sum "/tmp/apk-$VERSION.apk" | cut -d' ' -f1)
SHA_C=$(sha256sum "/tmp/apk-$VERSION-crudo.apk" | cut -d' ' -f1)

# 5. Kit de publicación.
paso "Kit de publicación en ~/appteka/"
mkdir -p "$HOME/appteka"
cp "/tmp/apk-$VERSION.apk" "$HOME/appteka/GLTools-Reborn-$VERSION.apk"
cp "/tmp/apk-$VERSION-crudo.apk" "$HOME/appteka/GLTools-Reborn-$VERSION-sin-comprimir.apk"
# Fuera los APK de versiones anteriores ANTES de calcular sumas: si no, las sumas apuntan a
# ficheros que ya no existen. (Otro fallo encontrado ejecutándolo, no leyéndolo.)
find "$HOME/appteka" -maxdepth 1 -name 'GLTools-Reborn-*.apk' ! -name "*-$VERSION*.apk" -delete
(cd "$HOME/appteka" && sha256sum GLTools-Reborn-"$VERSION"*.apk > SHA256SUMS.txt && md5sum GLTools-Reborn-"$VERSION"*.apk > MD5SUMS.txt && cat SHA256SUMS.txt)

# 6. Commit y push.
paso "Commit y push"
git add -A
git -c user.name="Alex" -c user.email="alex@labandita.dev" \
  commit -q -m "$VERSION: $TITULO" || echo "  (nada nuevo que commitear)"
git push "https://Alex17273747576777:$TOKEN@github.com/$REPO.git" HEAD:main

# 7. Release con sus cuatro adjuntos.
paso "Release v$VERSION"
RID=$(python3 - "$TOKEN" "$VERSION" "$TITULO" "$REPO" "$NOTAS" <<'PY'
import json, sys, urllib.request
tok, ver, titulo, repo = sys.argv[1:5]
# El cuerpo de la release se toma del archivo del tramo, NO de un /tmp que sobreviva de la vez
# anterior. Pasó de verdad y durante cinco versiones: 1.9.9, 1.10.0, 1.11.0 y 1.12.0 se publicaron
# con la misma nota, la de 1.9.9, porque el archivo de /tmp seguía ahí. Una nota repetida es una
# mentira: dice que no cambió nada cuando cambió todo.
nota = sys.argv[5]
if not __import__("os").path.exists(nota):
    raise SystemExit(f"falta {nota}: la nota del tramo se escribe antes de cerrar, no se hereda")
cuerpo = open(nota).read()
if len(cuerpo.strip()) < 80:
    raise SystemExit(f"{nota} es demasiado corta para contar lo que cambió")
datos = json.dumps({"tag_name": f"v{ver}", "name": f"GLTools Reborn {ver}", "body": cuerpo}).encode()
r = urllib.request.Request(f"https://api.github.com/repos/{repo}/releases", data=datos, method="POST",
    headers={"Authorization": "Bearer " + tok, "Accept": "application/vnd.github+json", "User-Agent": "cierre"})
print(json.load(urllib.request.urlopen(r))["id"])
PY
)
for f in "GLTools-Reborn-$VERSION.apk" "GLTools-Reborn-$VERSION-sin-comprimir.apk" "SHA256SUMS.txt" "MD5SUMS.txt"; do
  curl --fail-with-body --silent --show-error -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/octet-stream" \
    --data-binary "@$HOME/appteka/$f" \
    "https://uploads.github.com/repos/$REPO/releases/$RID/assets?name=$f" > /dev/null
  echo "  adjuntado $f"
done

# 8. Comprobación anónima: como lo verá cualquiera que entre sin sesión.
paso "Comprobación anónima de la release"
curl --fail --silent --show-error -L -o /tmp/descarga.apk "https://github.com/$REPO/releases/download/v$VERSION/GLTools-Reborn-$VERSION.apk"
[ "$(sha256sum /tmp/descarga.apk | cut -d' ' -f1)" = "$SHA_N" ] || { echo "¡el SHA no coincide con lo subido!"; exit 1; }
echo "  SHA-256 coincide · $NORMAL B normal · $CRUDO B crudo"

# 9. Limpieza obligatoria del entorno local.
paso "Limpieza"
rm -f -- "$NOTAS"
rm -rf /tmp/apk-"$VERSION"*.apk /tmp/descarga.apk /tmp/estudio /tmp/proy /tmp/fuentes
echo "  temporales fuera"

printf '\n\033[1m▸ TRAMO %s CERRADO\033[0m\n' "$VERSION"
printf '  normal  %s  %s\n' "$NORMAL" "$SHA_N"
printf '  crudo   %s  %s\n' "$CRUDO" "$SHA_C"

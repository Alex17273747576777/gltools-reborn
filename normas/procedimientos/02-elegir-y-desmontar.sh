#!/bin/bash
# Elegir apps de las que aprender y desmontarlas.
#
# Por qué existe: mirar capturas no basta. El diseño de una app vive en su Compose, no en
# `res/`, así que `aapt2` solo no lo cuenta. Hace falta el APK y, cuando el código importa,
# un descompilador.
#
# Criterios de elección (en este orden):
#   1. Que resuelva un problema de interfaz que NUESTRA app tenga.
#   2. Que sea de código abierto con repositorio declarado (se puede ir a la fuente).
#   3. Que no repita patrón de otra ya elegida.
#   4. Que su categoría aporte algo distinto (Tools, Root, Communication, Music, Comics...).
#
# Uso:  ./02-elegir-y-desmontar.sh <id-appteka> <nombre-corto> [<id> <nombre> ...]
set -u
EST="${EST:-/tmp/estudio}"
mkdir -p "$EST" && cd "$EST" || exit 1

bajar() { # $1=id  $2=nombre
  local html url
  html=$(curl -s -A "Mozilla/5.0" "https://appteka.store/apps/$1/download")
  url=$(echo "$html" | grep -oE 'https://appteka.store/download/[^"\\]*\.apk[^"\\]*' | head -1 | sed 's/&amp;/\&/g')
  [ -z "$url" ] && { echo "  ✗ $2: la ficha no ofrece descarga"; return 1; }
  curl -s -L -A "Mozilla/5.0" -o "$2.apk" "$url"
  local tipo; tipo=$(file -b --mime-type "$2.apk")
  case "$tipo" in
    application/vnd.android.package-archive|application/java-archive)
      echo "  ✓ $2  $(stat -c%s "$2.apk") B" ;;
    *) echo "  ✗ $2: no es un APK ($tipo)"; rm -f "$2.apk"; return 1 ;;
  esac
}

desmontar() { # $1=nombre
  local APK="$1.apk" BT="${BT:-/opt/android-sdk/build-tools/35.0.0}"
  [ -f "$APK" ] || return 0
  echo "── $1 ──"
  "$BT/aapt2" dump badging "$APK" 2>/dev/null | grep -E "^package:|application-label:|launchable-activity" | cut -c1-120
  # Recursos: lo único que dice de verdad si tiene tema propio o usa el de plantilla.
  "$BT/aapt2" dump resources "$APK" 2>/dev/null \
    | grep -oE "(color|style|dimen|font)/[a-z0-9_]+" \
    | grep -viE "^(color|style|dimen)/(abc|mtrl|material|design|androidx|google|appcompat|cardview|notification)_?" \
    | sort -u > "$1-recursos.txt"
  echo "   recursos propios: $(wc -l < "$1-recursos.txt")"
  # Fuentes empaquetadas: el atajo más rápido para dejar de parecer el sistema.
  unzip -l "$APK" 2>/dev/null | grep -icE "\.(ttf|otf)$" | sed 's/^/   fuentes empaquetadas: /'
  if [ -n "${JADX:-}" ] && [ -x "$JADX" ]; then
    "$JADX" --no-res -d "src_$1" "$APK" > "jadx-$1.log" 2>&1
    echo "   clases java: $(find "src_$1" -name '*.java' 2>/dev/null | wc -l)"
  fi
}

n=0
while [ $# -ge 2 ]; do
  bajar "$1" "$2" && desmontar "$2"
  shift 2; n=$((n+1))
done
echo
echo "Estudiadas $n apps en $EST. Lo aprendido se anota en APRENDIDO-DE-APPS.md, app por app,"
echo "diciendo qué se copia (estructura) y qué no (recursos, marca, tema ajeno)."

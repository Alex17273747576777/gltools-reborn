"""Verificación fail-closed en GitHub Actions; no genera ni distribuye claves."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

CERTIFICADO = "1ff4c6ee90263a73a7ea6097de5b1f31bcd049c805e384904d2087b4c6c9e803"
raiz = Path(sys.argv[1])
sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
herramientas = sorted((sdk / "build-tools").glob("*/apksigner"), key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.parent.name)))
if not herramientas:
    raise SystemExit("No se encontró apksigner oficial")
apksigner = herramientas[-1]
aapt = apksigner.parent / "aapt"
firmado = os.environ.get("FIRMADO") == "true"
archivos = sorted(raiz.glob("*.apk"))
assert len(archivos) == 2, "Se necesitan las dos variantes"
filas = []
contenidos = []
for apk in archivos:
    metadatos = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
    primera = metadatos.splitlines()[0]
    assert "name='io.github.labandita.glreborn'" in primera
    assert "versionCode='25'" in primera and "versionName='1.12.1'" in primera
    firma = subprocess.run([str(apksigner), "verify", "--verbose", "--print-certs", str(apk)], text=True, capture_output=True)
    if firmado:
        assert firma.returncode == 0, "Firma APK inválida"
        # apksigner imprime «V2 Signer: certificate SHA-256 digest: …» desde la API 30, y
        # «Signer #1 certificate SHA-256 digest: …» en versiones antiguas. La expresión
        # original sólo entendía el segundo formato, así que un APK correctamente firmado
        # fallaba la comprobación. Se aceptan los dos, y se exige que haya exactamente uno
        # distinto: dos certificados distintos en el mismo APK también es un error.
        certificados = sorted(set(re.findall(
            r"(?:V\d(?:\.\d)? Signer|Signer #\d+):? certificate SHA-256 digest: ([a-fA-F0-9]+)",
            firma.stdout,
        )))
        # Diagnóstico sin secretos: la huella de un certificado es pública (va en cada APK).
        if certificados != [CERTIFICADO]:
            print("CERT_ENCONTRADOS=" + ",".join(certificados), flush=True)
            print("CERT_ESPERADO=" + CERTIFICADO, flush=True)
            print("APKSIGNER_RC=" + str(firma.returncode), flush=True)
            print("APKSIGNER_SALIDA=" + firma.stdout.replace("\n", " | ")[:800], flush=True)
            print("APKSIGNER_ERR=" + firma.stderr.replace("\n", " | ")[:400], flush=True)
        assert certificados == [CERTIFICADO], "La firma no corresponde a las versiones actuales"
        assert "UNSIGNED" not in apk.name
    else:
        assert firma.returncode != 0 and "UNSIGNED-NO-INSTALAR" in apk.name
    with zipfile.ZipFile(apk) as z:
        contenidos.append({n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if n.startswith("assets/") and not n.endswith("/")})
    filas.append({"nombre": apk.name, "bytes": apk.stat().st_size, "sha256": hashlib.sha256(apk.read_bytes()).hexdigest()})
assert contenidos[0] == contenidos[1], "Los assets de ambas variantes deben ser idénticos"
assert max(a['bytes'] for a in filas) > 3 * min(a['bytes'] for a in filas), "La variante sin comprimir no tiene el tamaño esperado"
(raiz / "SHA256SUMS.txt").write_text("".join(f"{a['sha256']}  {a['nombre']}\n" for a in filas))
datos = {"version": "1.12.1", "version_code": 25, "commit": os.environ["GITHUB_SHA"], "firmado": firmado,
         "certificado_sha256": CERTIFICADO if firmado else None, "apks": filas,
         "limites": "Sin prueba física, arranque ni garantía de compatibilidad del Power HAL."}
(raiz / "verificacion.json").write_text(json.dumps(datos, indent=2) + "\n")
print(json.dumps(datos, indent=2))

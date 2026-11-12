"""Prueba del censo con APKs sintéticos: se construyen ZIPs y se mide.

Por qué con ZIPs y no con el APK real: el APK real sólo existe después de
compilar, y esta prueba corre ANTES en el flujo. Si el censo se equivoca de
eje —confundir almacenado con comprimido, o dar por universal un APK sin x86—
tiene que notarse aquí, no en la release publicada.
"""
import sys
import tempfile
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from censo_apk import UNIVERSAL, censo, comprobar, faltan_para_universal  # noqa: E402

DATO = b'{"dispositivo": "x"}' * 400
NATIVA = b"\x7fELF" + b"\0" * 900


def construir(ruta, abis, nativas_comprimidas, datos_comprimidos, nativas_por_abi=1):
    comp_n = zipfile.ZIP_DEFLATED if nativas_comprimidas else zipfile.ZIP_STORED
    comp_d = zipfile.ZIP_DEFLATED if datos_comprimidos else zipfile.ZIP_STORED
    with zipfile.ZipFile(ruta, "w") as z:
        for abi in abis:
            for k in range(nativas_por_abi):
                z.writestr(zipfile.ZipInfo("lib/%s/libcosa%d.so" % (abi, k)),
                           NATIVA, compress_type=comp_n)
        for nombre in ("assets/datos/a.json", "assets/datos/b.xml", "assets/datos/c.csv"):
            z.writestr(zipfile.ZipInfo(nombre), DATO, compress_type=comp_d)
    return ruta


def falla(fn, *a, **kw):
    try:
        fn(*a, **kw)
    except AssertionError as e:
        return str(e)
    return None


comprobaciones = 0
with tempfile.TemporaryDirectory() as tmp:
    d = Path(tmp)

    # 1 · la variante normal, tal como se publica hoy
    normal = construir(d / "normal.apk", UNIVERSAL, nativas_comprimidas=False,
                       datos_comprimidos=True)
    c = comprobar(normal, crudo=False)
    assert c["abis"] == sorted(UNIVERSAL), c["abis"]
    assert c["nativas"] == 4 and c["nativas_almacenadas"] == 4, c
    assert c["datos"] == 3 and c["datos_comprimidos"] == 3, c
    comprobaciones += 1

    # 2 · la variante cruda: los dos ejes al revés
    cruda = construir(d / "cruda.apk", UNIVERSAL, nativas_comprimidas=True,
                      datos_comprimidos=False)
    c2 = comprobar(cruda, crudo=True)
    assert c2["nativas_comprimidas"] == 4 and c2["datos_almacenados"] == 3, c2
    comprobaciones += 1

    # 3 · sin una ABI no es universal, y se dice cuál falta
    manca = construir(d / "manca.apk", ("arm64-v8a", "armeabi-v7a"),
                      nativas_comprimidas=False, datos_comprimidos=True)
    assert faltan_para_universal(censo(manca)) == ["x86", "x86_64"], censo(manca)
    mensaje = falla(comprobar, manca, crudo=False)
    assert mensaje and "no es universal" in mensaje, mensaje
    comprobaciones += 1

    # 4 · nativas comprimidas en la variante normal: se rechaza
    mal = construir(d / "mal.apk", UNIVERSAL, nativas_comprimidas=True,
                    datos_comprimidos=True)
    mensaje = falla(comprobar, mal, crudo=False)
    assert mensaje and "empaquetado moderno" in mensaje, mensaje
    comprobaciones += 1

    # 5 · datos sin comprimir en la variante normal: se rechaza
    mal2 = construir(d / "mal2.apk", UNIVERSAL, nativas_comprimidas=False,
                     datos_comprimidos=False)
    mensaje = falla(comprobar, mal2, crudo=False)
    assert mensaje and "datos sin comprimir" in mensaje, mensaje
    comprobaciones += 1

    # 6 · una cruda que sí comprime los datos no es cruda
    falsa = construir(d / "falsa.apk", UNIVERSAL, nativas_comprimidas=True,
                      datos_comprimidos=True)
    mensaje = falla(comprobar, falsa, crudo=True)
    assert mensaje and "variante cruda" in mensaje, mensaje
    comprobaciones += 1

    # 7 · ABI descompensadas: una lleva dos nativas y otra una
    torcida = construir(d / "torcida.apk", UNIVERSAL, nativas_comprimidas=False,
                        datos_comprimidos=True)
    with zipfile.ZipFile(torcida, "a") as z:
        z.writestr(zipfile.ZipInfo("lib/x86/libextra.so"), NATIVA,
                   compress_type=zipfile.ZIP_STORED)
    mensaje = falla(comprobar, torcida, crudo=False)
    assert mensaje and "mismas nativas" in mensaje, mensaje
    comprobaciones += 1

print("test_censo_apk: %d comprobaciones, todas en verde" % comprobaciones)

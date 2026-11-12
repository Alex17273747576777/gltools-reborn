"""El censo del APK: qué arquitecturas lleva dentro y cómo va empaquetado.

POR QUÉ EXISTE
--------------
Se prometió «versión universal compatible con cualquier formato de Android» y
«todo comprimido». Medido sobre la 1.16.0 publicada, las dos cosas eran
ciertas, pero nadie las comprobaba: un `abiFilters` escrito a mano, o una
dependencia que dejara de traer x86, habría publicado una release que no se
instala en ciertos aparatos sin que fallara ningún paso del flujo.

Aquí se abre el ZIP construido y se falla si falta algo. Dos ejes, medidos los
dos, porque en este repo las variantes son justo opuestas:

  · La variante normal lleva los DATOS comprimidos (json/xml/csv de assets) y
    las NATIVAS almacenadas. Almacenadas no es un descuido: es el empaquetado
    moderno de AGP (`useLegacyPackaging = false`), que deja las `.so` sin
    comprimir y alineadas para que el sistema las mapee directamente en vez de
    descomprimirlas y duplicarlas en la instalación.
  · La variante cruda (`-PrebornDatos=crudo`) lleva los DATOS sin comprimir y
    las NATIVAS comprimidas, que es el comportamiento heredado.

Ambas tienen que ser universales: las cuatro ABI, con las mismas nativas en
cada una.
"""
import zipfile
from pathlib import Path

UNIVERSAL = ("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
DATOS = ("json", "xml", "csv")


def censo(ruta):
    """Abre el APK y devuelve los hechos, sin interpretar nada."""
    with zipfile.ZipFile(Path(ruta)) as z:
        entradas = z.infolist()
    nativas = [i for i in entradas
               if i.filename.startswith("lib/") and i.filename.endswith(".so")]
    datos = [i for i in entradas
             if i.filename.startswith("assets/")
             and i.filename.rsplit(".", 1)[-1].lower() in DATOS]

    def reparto(lista):
        c = sum(1 for i in lista if i.compress_type == zipfile.ZIP_DEFLATED)
        a = sum(1 for i in lista if i.compress_type == zipfile.ZIP_STORED)
        return c, a

    nc, na = reparto(nativas)
    dc, da = reparto(datos)
    abis = sorted({i.filename.split("/")[1] for i in nativas})
    return {
        "abis": abis,
        "nativas": len(nativas),
        "nativas_por_abi": {a: sum(1 for i in nativas if i.filename.split("/")[1] == a)
                            for a in abis},
        "nativas_comprimidas": nc,
        "nativas_almacenadas": na,
        "datos": len(datos),
        "datos_comprimidos": dc,
        "datos_almacenados": da,
        "entradas": len(entradas),
    }


def faltan_para_universal(c):
    """Qué arquitecturas de las cuatro faltan en este APK."""
    return [a for a in UNIVERSAL if a not in c["abis"]]


def comprobar(ruta, crudo):
    """Fall-closed: devuelve el censo y afirma lo que se prometió.

    `crudo` dice cuál de las dos variantes es, porque la política de
    empaquetado es opuesta en cada una.
    """
    c = censo(ruta)
    nombre = Path(ruta).name

    faltan = faltan_para_universal(c)
    assert not faltan, "%s no es universal: faltan %s" % (nombre, faltan)
    assert c["nativas"] > 0, "%s no lleva nativas: no hay nada que censar" % nombre
    assert c["datos"] > 0, "%s no lleva datos: no hay nada que censar" % nombre
    distintos = set(c["nativas_por_abi"].values())
    assert len(distintos) == 1, (
        "%s: las ABI no llevan las mismas nativas %s" % (nombre, c["nativas_por_abi"]))

    if crudo:
        assert c["datos_comprimidos"] == 0, (
            "%s dice ser la variante cruda y lleva %d datos comprimidos"
            % (nombre, c["datos_comprimidos"]))
        assert c["datos_almacenados"] == c["datos"], (
            "%s: en la cruda todos los datos van sin comprimir" % nombre)
        assert c["nativas_almacenadas"] == 0, (
            "%s: en la variante cruda las nativas van comprimidas, hay %d almacenadas"
            % (nombre, c["nativas_almacenadas"]))
    else:
        assert c["datos_almacenados"] == 0, (
            "%s lleva %d datos sin comprimir" % (nombre, c["datos_almacenados"]))
        assert c["nativas_comprimidas"] == 0, (
            "%s: las nativas van almacenadas (empaquetado moderno, sin extracción) "
            "y hay %d comprimidas" % (nombre, c["nativas_comprimidas"]))
    return c

# 1.16.1 · el APK universal y comprimido se comprueba en cada corte

## Lo que cambia para quien instala

Nada. El APK es el mismo: universal (las cuatro arquitecturas) y con los datos
comprimidos. Esta versión añade la comprobación de que siga siéndolo.

## Lo que se midió antes de escribir nada

Sobre los assets publicados de la 1.16.0:

| variante | bytes | ABI | nativas | datos json/xml/csv |
|---|---|---|---|---|
| `GLTools-Reborn-1.16.0.apk` | 3.431.626 | arm64-v8a, armeabi-v7a, x86, x86_64 | 4 almacenadas | 37 comprimidos |
| `…-sin-comprimir.apk` | 16.457.942 | las mismas cuatro | 4 comprimidas | 37 almacenados |

Las dos variantes son universales y son exactamente opuestas en los dos ejes.

## Por qué las nativas van almacenadas en la variante normal

No es un descuido ni compresión olvidada: es el empaquetado moderno de AGP
(`useLegacyPackaging = false`). Las `.so` van sin comprimir y alineadas para que
el sistema las mapee directamente desde el APK, en vez de descomprimirlas y
guardar una segunda copia en la instalación. Comprimirlas ahorraría unos KB en
la descarga —son 37.392 B entre las cuatro— y costaría esa copia duplicada en
el aparato. La variante cruda invierte los dos ejes a propósito, para quien
necesite leer los datos sin descomprimir.

## Lo que ahora falla si alguien lo rompe

`.github/scripts/censo_apk.py` abre el ZIP construido y afirma:

* las cuatro ABI presentes, y las mismas nativas en cada una;
* variante normal: datos comprimidos, nativas almacenadas;
* variante cruda: datos sin comprimir, nativas comprimidas.

`verificar_version.py` lo llama por variante y escribe el censo en
`verificacion.json`, que viaja con la release. `.github/scripts/test_censo_apk.py`
prueba el censo con ZIPs sintéticos —siete casos, incluidos los dos modos de no
ser universal y el de confundir los ejes— y el flujo lo corre antes de compilar,
como ya hace con `test_preparar_firma.py`.

## Lo que sigue sin probarse

Ninguna de las dos variantes se ha instalado ni arrancado en un aparato. El
censo dice qué hay dentro del ZIP; no dice que la app funcione.

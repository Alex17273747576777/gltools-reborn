# De dónde salen los datos que lleva la app

Ningún dato de esta app está inventado ni generado al azar. Cada bloque viene de una
fuente pública, se puede volver a descargar, y el fichero que lo contiene dice de dónde
salió. Esto es el detalle; el resumen está en el README.

## Lo que se usa

| Bloque | Fichero en el APK | Fuente | Cómo se obtuvo |
|---|---|---|---|
| TOP500 | `assets/datos/top500/*.json` (14) | [top500.org](https://top500.org/lists/top500/) | XML oficial de cada edición (`TOP500_YYYYMM_all.xml`), parseado campo a campo |
| TOP500 original | `assets/datos/top500/fuente/*.xml` (14) | idem | los XML **tal cual**, sin tocar |
| Dispositivos | `assets/datos/dispositivos.json` | [Google Play supported devices](https://storage.googleapis.com/play_public/supported_devices.csv) | el CSV oficial, convertido a JSON compacto |
| Dispositivos original | `assets/datos/dispositivos-fuente.csv` | idem | el CSV **tal cual**, en UTF-8 |
| Fichas | `assets/datos/mobolist.json` | [mobolist.net](https://www.mobolist.net/en) | se leyeron 1.500 fichas una a una; se quedaron las 1.386 con chipset y GPU |
| SoC | `assets/datos/soc.json` | Wikipedia: *List of Qualcomm Snapdragon systems on chips* y *List of MediaTek systems on chips* | tablas de la página, con GPU, MHz y GFLOPS |
| GPU | `assets/datos/gpu.json` | Wikipedia: *Adreno* | tablas, con ALUs y MHz |
| Android | `assets/datos/android.json` | documentación de Android / Wikipedia | versión, API y año |

## Lo que se descartó, y por qué

**versus.com** — su `robots.txt` responde con un desafío anti-bot (HTTP 202 y cuerpo
vacío): **no permite el acceso automatizado**. Se abrió **una** ficha a mano para comparar
(el Tecno Spark 20C: 6,6", 8 GB, Helio G36, 5000 mAh, Android 13) y coincide con la ficha
de mobolist. Es una fuente buena para leer a mano; no lo es para rastrear sin permiso.
Lo que sí se hizo: comprobar contra ella los datos de otros sitios.

**magiskmodule.com** — se leyeron las fichas de los 280 módulos de *AxManager Plugins*
para saber **qué hace** cada uno. No se copió código (no lo publican), ni diseño, ni
binarios. De ahí salió la sección *Rendi.*, reescrita desde cero con comandos documentados
de Android (`cmd package compile`, `am suspend`, `dumpsys deviceidle`, `cmd game`…).

## Cómo se comprueba que los datos son los que digo

```bash
# los XML originales, byte a byte contra los de top500.org
unzip -p GLToolsReborn-1.6.0-release.apk assets/datos/top500/fuente/202511.xml | head -3

# el catálogo de Google, con su cabecera original
unzip -p GLToolsReborn-1.6.0-release.apk assets/datos/dispositivos-fuente.csv | head -3

# tu propio teléfono en el catálogo oficial
unzip -p GLToolsReborn-1.6.0-release.apk assets/datos/dispositivos.json \
  | grep -o '.\{0,40\}TECNO-BG7.\{0,40\}'
```

## Reglas que se aplican a todos los bloques

1. **Un campo que la fuente no trae, se queda vacío.** No se rellena «porque lo sé».
2. **Cada fichero lleva `fuente` y `url`.** Si no los lleva, no entra.
3. **Lo que se infiere, se marca.** El OpenGL ES de una GPU se calcula y se dice que se
   calcula; no se presenta como leído.
4. **Nada de relleno.** Si algún día el APK tiene que pesar más, será porque hay más
   datos reales, no porque se haya engordado el fichero.

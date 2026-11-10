# Revisión de catálogos — 2026-11-10

## Alcance real
Se leyeron completos los directorios/páginas de entrada solicitados: smartGSM `/moviles` (2 fragmentos), Kimovil `/es/todas-las-marcas-de-moviles` (5) y Mobolist `/en` (2). Son directorios y portadas, NO todas las fichas de todos sus modelos. Se contrastó además el Tecno Spark 20C entre los tres. No se realizó un rastreo masivo ni se validó un teléfono físico. La ficha Kimovil tiene más contenido que el fragmento de hardware leído; no se afirma una auditoría exhaustiva del sitio.

## Utilidad y límites
| Fuente | Lo observado | Uso admisible en GLTools |
|---|---|---|
| [smartGSM](https://www.smart-gsm.com/moviles) | Directorio por fabricante y fichas con especificaciones, opiniones y comparaciones | Contrastar características publicadas, no inferir firmware/HAL/root |
| [Kimovil](https://www.kimovil.com/es/todas-las-marcas-de-moviles) | Directorio, variantes por RAM/almacenamiento, bandas, precios y comparaciones | Identificar variante y discrepancias; no confundir ofertas, puntuación Ki ni AnTuTu con medición propia |
| [Mobolist](https://www.mobolist.net/en) | Teléfonos/tablets/relojes, filtros, fichas, precios regionales y comparaciones | Enriquecer campos presentes, mantener vacíos los desconocidos; no asumir GPU donde sólo dice MediaTek |

Los recuentos por marca difieren y no se suman: no se demostró que apliquen el mismo criterio para modelos/variantes. Tampoco el precio regional mostrado es una cotización de República Dominicana.

## Contraste puntual: Tecno Spark 20C
Fuentes leídas:
- https://www.smart-gsm.com/moviles/tecno-spark-20c
- https://www.kimovil.com/es/donde-comprar-tecno-spark-20c-8gb-128gb
- https://www.mobolist.net/en/devices/tecno-spark-20c

| Campo | smartGSM | Kimovil (variante 8/128) | Mobolist |
|---|---|---|---|
| Android publicado | 13 | 13 / HiOS 12.6 | 13 |
| Pantalla | 6,6 / 720×1612 / 90 Hz | 6,56 / 720×1612 / 90 Hz | 6,6 / 720×1612 / 90 Hz |
| CPU/SoC | MediaTek octa-core 2,2 GHz | Helio G36 / Cortex-A53 2,2 GHz | MediaTek, sin modelo preciso |
| GPU | No identificada en la ficha leída | PowerVR GE8320 680 MHz | No identificada |
| RAM | 4/8 GB | 8 GB en variante seleccionada | 4/8 GB |
| Batería | 5000 mAh | 4900 mAh | 5000 mAh |
| Cámara auxiliar | 0,08 MP | 0,3 MP | 0,08 MP |

**No se resuelven por votación** batería/cámara/diagonal ni se supone equivalencia entre BG7/BG7N/variantes regionales. Falta ficha primaria exacta y correspondencia con el equipo. No se modifica la base de datos para introducir una certeza falsa. El dato de Android es el publicado para el modelo, no una lectura de la compilación instalada hoy.

## Reglas de importación y verificación pendientes
1. Por campo conservar fuente, fecha, unidad, variante y confianza; declarar discrepancias.
2. Separar nombre comercial, model, device/codename, SoC y GPU. No convertirlos entre sí por parecido de nombres.
3. Unir variantes sólo con evidencia; distinguir RAM física de expansión virtual.
4. No calcular soporte real OpenGL/Vulkan, permisos SELinux ni fixed-performance a partir de una ficha comercial. Leer capacidades del driver y la ROM del teléfono.
5. No mezclar AnTuTu v9/v10 ni tomar un ranking publicado como mejora causada por cambiar propiedades.
6. Respetar condiciones de acceso/licencia antes de una importación masiva. No se ha obtenido permiso de redistribución de los tres catálogos.

Pendiente: auditoría campo a campo de las bases existentes y su procedencia, más allá de esta muestra; validación en dispositivo, captura real y persistencia tras reinicio. No se cierra ese trabajo con esta nota.

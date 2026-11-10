# Procedimiento · de una pantalla fea a una que no lo es

Esto no es inspiración. Es el orden de operaciones que funcionó, escrito para poder repetirlo.

## Fase 0 · Antes de tocar nada: nombrar el problema

Una pantalla «fea» no está fea por un icono. Casi siempre es una de estas cinco:

| Síntoma que ves | Causa real | Arreglo |
|---|---|---|
| «Todo se ve igual» | Todas las cajas tienen el mismo radio, el mismo borde y el mismo peso | jerarquía: un dato grande, filas densas, cajas sólo cuando hagan falta |
| «Parece una plantilla» | Cinco acentos, degradado por caja, vidrio en el contenido | un acento, tono plano, tres niveles de superficie |
| «No sé dónde mirar» | el dato importante está al mismo tamaño que el secundario | el dato principal ≥ 24 sp, y solo |
| «No se siente una app» | fuente del sistema, sin escala | tipografía propia con licencia libre, y una escala de verdad |
| «Se desplaza raro» | dos contenedores que se desplazan, anidados | uno solo: `Column` + `verticalScroll` |

## Fase 1 · Censar antes de inventar

1. `01-censar-catalogo-appteka.py` — saber cuántas apps hay y cuáles son de código abierto, por
   categoría. Sin esto se acaba mirando siempre las mismas cinco.
2. Elegir **de 6 a 8 apps que resuelvan un problema que tú tengas**, no las más bonitas.
3. `02-elegir-y-desmontar.sh` — bajar el APK y desmontarlo. Recursos con `aapt2`, código con
   `jadx`, capturas a mano.

## Fase 2 · Extraer el patrón, no el recurso

Para cada app, escribir **una frase**: «esta app resuelve *X* con *Y*». Si no cabe en una frase,
no has encontrado el patrón, has encontrado una impresión.

Ejemplos reales de la 1.9.0:

- Hardware Monitor resuelve *muchos grupos de datos que no caben* con *un carrusel de píldoras y
  un panel que cambia*.
- Androoster resuelve *acciones que hay que pulsar rápido* con *baldosas con icono grande*.
- LSPosed resuelve *un dato que es el asunto* con *el dato solo, en grande, sin tarjeta*.
- Scene resuelve *diez ajustes seguidos* con *bloques con rótulo que respiran*.

## Fase 3 · Aplicar, y sólo lo que tiene datos detrás

Un patrón que promete más de lo que hay es peor que no tenerlo. Si el carrusel tiene cinco
píldoras, tiene que haber cinco conjuntos de datos **reales** detrás; si no, se ponen tres.

## Fase 4 · Vigilarlo con una prueba

El patrón se escribe como una comprobación en `Pruebas.kt`. Ejemplos que ya existen:

- `LazyColumn`/`LazyRow` dentro de `Home.kt` → **falla** (fue el cierre de la app).
- `Brush.linearGradient(` en una pantalla → **falla** (diseño de plantilla).
- más de 2 usos de `.vidrio(` → **falla**.
- alguna de las 5 lecturas del teléfono sin `proteger` → **falla**.
- no hay familia tipográfica propia → **falla**.

Sin la prueba, el patrón se pierde en el siguiente tramo. Con ella, el siguiente tramo no puede
reintroducir el error aunque quiera.

## Fase 5 · Decir lo que no se ha probado

`PROBADO.md` no es un adorno: es la lista de lo que se ha comprobado y **con qué**. Lo que no se
ha podido probar se dice con la razón. Una app que miente sobre lo que ha probado no sirve.

## Fase 6 · Limpiar

Antes de cerrar: `/tmp` fuera. Clones, APK descargados, descompilaciones, fuentes sin usar. Si no
se limpia, el siguiente tramo empieza con el disco lleno y con dudas sobre qué es qué.

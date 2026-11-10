# GLTools Reborn 1.13.0

**Perfiles de diagnóstico personalizados, en JSON.** Hasta ahora había **un** perfil, sin nombre,
en formato `clave=valor`, y los umbrales de aviso se enseñaban pero no se podían tocar: un número
que se lee y no se cambia es decoración. Ahora hay varios perfiles con nombre, se crean desde el
que está en uso, se borran, y se guardan en `perfiles-diagnostico.json`.

## Qué se puede hacer ahora

- **Tres perfiles de fábrica** que sirven para cosas distintas: *Rápido* (2 s, tres pruebas),
  *Equilibrado* y *Informe completo* (CPU 1 s, ocho pruebas, umbrales más estrictos).
- **Crear uno nuevo** con nombre: copia los ajustes del que está en uso y queda activo.
- **Umbrales editables de verdad**: memoria (80/85/90/95 %) y batería (38/42/46 °C).
- **Editar un ajuste se guarda** en el perfil activo. Antes cambiaba sólo en memoria y al reabrir
  la app volvía a estar como antes.
- **Migración sin pérdida**: si vienes de la 1.12.x, tu `ajustes-diagnostico.txt` se convierte en
  un perfil llamado «Migrado» la primera vez que abras esta versión.

## El formato

JSON porque se puede llevar de un teléfono a otro y abrir con cualquier herramienta. Se lee con un
lector propio (`MiniJson.kt`): `org.json` es del sistema Android y las pruebas del núcleo corren en
una JVM normal, así que usarlo habría dejado la lectura de perfiles sin probar. El lector **rechaza
con línea y columna** lo que no es JSON válido —comas de más, comillas sin cerrar, comentarios,
saltos de línea dentro de una cadena— en vez de devolver `null` y que el perfil desaparezca sin
explicación. Un fichero roto se aparta a `perfiles-diagnostico.roto.json` y se vuelve a fábrica
diciendo por qué.

## Límites

Compilado y probado en el CI (`pruebasNucleo`). No hay prueba física en un teléfono real desde
aquí. Las pruebas del lector de JSON se diseñaron, además, replicando el algoritmo en otro lenguaje
para comprobar los once casos de error antes de gastar un ciclo de compilación: eso no sustituye a
las pruebas de verdad, las precede.

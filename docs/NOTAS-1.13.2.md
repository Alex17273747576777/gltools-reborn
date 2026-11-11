# GLTools Reborn 1.13.2

**El modo penetración ya no pinta en verde capacidades que no funcionan.**

`Shell.Salida` siempre devolvió el código de salida del comando, pero `ModoPenetracion` no se lo
pasaba al clasificador. Consecuencia: cualquier comando que terminaba mal imprimiendo algo
inocente se clasificaba como **COMPLETO**, porque la regla era «respondió y no dijo ninguna
palabra de error». Es exactamente el fallo que esta app se supone que no comete. Ahora el código
de salida es un parámetro obligatorio de `clasificar` y distinto de cero es **NO**; `-1` —tiempo
agotado o proceso que no llegó a lanzarse— también. El permiso denegado se sigue mirando antes
que el código, porque «te falta el permiso» es útil y «no funciona» no.

**Las sondas que escriben dejan el teléfono como estaba.** La de ajustes globales hacía
`settings put global reborn_prueba 1` y nunca lo quitaba: la clave se quedaba en el sistema para
siempre en el teléfono de cualquiera que pulsara el botón. Ahora las dos sondas que escriben leen
el valor previo, escriben, leen de vuelta y restauran —o borran la clave si antes no existía— en
el mismo comando. El `exit $rc` del final no es adorno: `Shell.correr` envuelve con
`sh -c "… 2>&1"`, y sin él el código sería siempre el del último `echo`. Comprobado con `sh` de
verdad, no supuesto.

**Dos nombres que prometían de más.** «Congelar apps (pm disable-user)» ejecutaba
`pm list packages -d`, que no congela nada: ahora se llama «Listar apps congeladas». Y «Modo
rendimiento fijo» sólo miraba si la *ayuda* de `cmd power` anuncia la orden, que no es lo mismo
que el fabricante la obedezca: el nombre ya lo dice.

Los detalles ahora citan lo que se leyó de vuelta («la lectura posterior dio «1» en vez de «0»»)
y el motivo real de `Shell.Salida` en vez de la primera línea a ciegas.

## Pruebas

`pruebasNucleo` (JVM, sin emulador): 12 comprobaciones del clasificador con códigos explícitos
—incluida la que reproduce el fallo: misma salida inocente, código 1— y 8 sobre las sondas: que
leen el valor previo, que lo restauran, que borran la clave si no existía, que terminan en
`exit $rc`, que ninguna toca `persist.reborn`, y que ningún nombre promete de más.

El APK sale firmado con la clave original `1ff4c6ee…c6c9e803`, comprobada con `apksigner` antes
de publicar. Sin esa huella no se publica.

## Lo que NO se ha hecho

- **Ninguna prueba en teléfono.** La clasificación y las reglas de las sondas se prueban en una
  JVM; que Shizuku ejecute esos comandos y que el teléfono responda como se espera sigue sin
  verificarse en un dispositivo. Está abierto como pendiente 9 en `docs/PENDIENTES.md`.
- No se añade ninguna capacidad nueva: esto quita mentiras, no pone funciones.

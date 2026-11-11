# GLTools Reborn 1.15.1

**Lo destapó un usuario con un teléfono en la mano: el estado térmico «salía raro».**

Un usuario instaló la 1.14.0 y dijo que el estado térmico salía raro o «no medido». Es la primera
evidencia de dispositivo de esta serie, y al mirarlo aparecieron tres defectos del mismo género:
enseñar como número lo que no es un número, y afirmar lo que no se sabe.

## 1 · Un estado no es una cantidad

En 1.14.0 metí el estado térmico en `Medida.valor` como nivel 0-6. La pantalla hizo exactamente lo
que tenía que hacer con un número: dibujó una barra vacía y **«0.00 nivel 0-6»**. Se lee como si no
hubiera dato, cuando el sistema acababa de declarar «sin estrangulamiento térmico». El informe en
Markdown hacía lo mismo, en la tabla y en el resumen.

Ahora `Medida` tiene un campo `texto` para lo que es una **categoría**, y la regla es simple: las
categorías se dicen con palabras y las cantidades con números. El estado térmico se enseña como
«sin estrangulamiento térmico (nivel 0 de 6)», sin barra. El **margen** térmico sigue con número y
barra, porque ése sí es una proporción entre 0 y 1.

## 2 · «El sistema no devolvió nada» era una suposición

El estado y el margen se leían envolviendo la llamada en un `getOrNull()`, que convierte **cualquier**
excepción en un `null`. Y el `null` se explicaba como «el sistema no devolvió estado térmico». O sea:
si el fabricante lanzaba una excepción al preguntar, la app afirmaba que el sistema no había
contestado. Dos cosas distintas, y la segunda esconde que la API falló.

Ahora la excepción se captura por separado, se anota en los errores del diagnóstico (que van en el
informe) y se dice en el motivo: *«getCurrentThermalStatus falló en este teléfono:
UnsupportedOperationException: …»*. Si de verdad no hubo respuesta, se sigue diciendo que no la hubo.

Y `getThermalHeadroom` puede devolver **NaN** en algunos fabricantes. NaN no es un margen: antes
habría salido como número. Ahora se declara «el sistema devolvió NaN: no sabe cuánto margen térmico
queda».

## 3 · No aparecer no es explicar

Desde 1.14.0 el perfil se respeta, y eso tiene un efecto que no se decía: una prueba desmarcada **no
aparece** en el informe. Un informe corto y un informe que no midió se parecían demasiado. Ahora
`DiagnosticoCompleto` lleva `sinEjecutar` y el informe termina con:

```text
no ejecutado porque el perfil no lo marca:
  · Red
  · Vulkan
```

con los nombres que ve el usuario, no las claves internas.

Esto importa para quien estuviera en 1.14.0 con un perfil que no fuera «todas»: **las medidas
térmicas podían no estar ejecutándose siquiera**, y la 1.15.0 ya da la pantalla para marcarlas.

## 4 · Leer el estado térmico y sólo pintarlo era leerlo a medias

Si el sistema declara estrangulamiento serio o peor (nivel 3 de 6, según la propia documentación de
Android), ahora **avisa**, igual que avisa con la memoria por encima del umbral y la batería
caliente. El umbral vive en `ReglasDiagnostico.NIVEL_PREOCUPA` y lo usan las dos cosas, para que no
puedan divergir.

## Pruebas

`pruebasNucleo` (JVM, sin emulador): **21 comprobaciones nuevas** —que el estado térmico se
declare con palabras y no como número, que el margen siga siendo cantidad, que una excepción se diga
como excepción y no como «no contestó», que NaN no se pinte de margen, que un teléfono
estrangulándose avise y uno frío no, que el informe liste lo que no se ejecutó con nombres de
usuario, y que la categoría no salga como «0.00» ni con barra vacía en el Markdown generado.

El APK sale firmado con la clave original `1ff4c6ee…c6c9e803`, comprobada con `apksigner` antes de
publicar.

## Lo que NO se ha hecho

- **Esto sigue sin ser una prueba de teléfono.** Lo que hay es el aviso de un usuario y un arreglo
  razonado sobre el código. Que el estado térmico se vea ahora bien en su pantalla **no está
  confirmado**: hace falta que instale la 1.15.1 y lo mire.
- No se sabe qué teléfono era, qué nivel de API tiene, ni qué texto aparecía exactamente. Los tres
  defectos se arreglan porque son defectos en cualquier aparato, pero si lo que vio era otra cosa,
  esto no lo cubre.
- Si el perfil activo no marca la prueba térmica, el teléfono no la ejecuta: eso es correcto y ahora
  se dice en el informe, pero la solución es marcarla en «Qué pruebas se ejecutan» (1.15.0).

# GLTools Reborn

Perfiles de dispositivo para Android **sin root obligatorio, sin servidores y sin nada de pago**.

Código abierto. Los perfiles viajan dentro del APK: en modo avión funciona igual.

---

## Qué es esto y qué no es

**Es** una app nueva, escrita desde cero, que cambia lo que el teléfono declara ser
(`ro.product.model`, `ro.product.brand`, `ro.product.device`, `ro.hardware`,
`ro.opengles.version`) para que los juegos y las apps vean el dispositivo que tú elijas.
Cambiar el modelo es útil para que un juego aplique un preset gráfico en vez de otro.

**No es** GLTools. No lleva el módulo Xposed de nadie, no desbloquea la app de pago de
terceros, no trae binarios ajenos. Ver [PROCEDENCIA.md](PROCEDENCIA.md) para el detalle
de qué se tomó como referencia del proyecto original y qué se descartó, con nombre y motivo.

## Las tres vías, y la cuarta que es decir la verdad

| Vía | ¿Root? | Escribe propiedades |
|---|---|---|
| Shizuku activo + permiso | No | Sí |
| `su` disponible | Sí | Sí |
| Ninguna de las dos | — | **No, y lo dice** |

La cuarta fila es la importante. Sin Shizuku ni root, Android no deja escribir `ro.*` a una
app normal: no hay truco, no hay API oculta, no hay «modo compatibilidad» que lo evite. En
ese estado la app se abre, lee lo que puede, y muestra **SIN VÍA** en rojo arriba. Los
botones de aplicar existen pero responden «No se aplicó nada». Una app que dijera
«aplicado» ahí estaría mintiendo.

## Sin límite de apps

No hay lista blanca ni cuota. Un perfil se aplica al sistema entero: afecta a todas las
apps a la vez, que es lo que hace útil a esta clase de herramienta. No hay «3 perfiles
gratis» ni «desbloquea más dispositivos»: el catálogo entero está abierto.

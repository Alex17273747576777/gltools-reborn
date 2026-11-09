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

## Modo seguro y modo experto

Hay un interruptor arriba, siempre visible.

- **Seguro** (por defecto): sólo modelo, marca, placa, hardware y OpenGL ES. Si algo no
  cuadra al releer, **se deshace solo** y te dice que el teléfono está como estaba.
- **Experto**: añade API, versión de Android, huella de compilación y kernel. Cada
  escritura se anota antes de hacerse, y si la app muere a mitad se recupera sola al
  abrirla.

Lo que **ningún** modo permite: tocar IMEI, número de serie, MAC o Android ID, ni
`persist.*`, `ro.boot.*`, `ro.secure`. Esas listas están en `core/Seguridad.kt` y a la
vista en Diagnóstico. El detalle completo está en [SEGURIDAD.md](SEGURIDAD.md).

**No hay «acceso total»**, ni con el modo experto. Sin root no existe, y con root esta app
no lo necesita: no hay un botón que finja dártelo.

## Utilidades (1.2.0)

Sección **Útil**, todo a través de la shell de Shizuku — los mismos permisos que un
`adb shell`, sin root:

- **Resolución** con densidad recalculada y **reversión automática en 15 s** si no
  confirmas. El comando de vuelta corre aparte: funciona aunque la app se cierre.
- **Frecuencia de refresco** a 60 / 90 / 120 Hz.
- **Congelar apps** para que no gasten batería, sin desinstalarlas. Las del sistema están
  bloqueadas en el código.
- **Borrar caché** por app, midiendo antes y después.
- **DNS privado** y **escala de animación**.

De dónde salió cada idea y qué se descartó a propósito: [FUNCIONES.md](FUNCIONES.md).

## Las seis pantallas

- **Perfiles** — el estado real del teléfono arriba, el catálogo con su nivel de confianza,
  y *Aplicar* / *Restaurar*. Cada card dice si está VERIFICADO o PARCIAL y **qué falta**.
- **Apps** — todo lo instalado, contado y sin filtro. Está aquí para que compruebes tú
  mismo que no hay lista blanca: si tu app no aparece, es que no está instalada.
- **GPU** — lo que declara el teléfono y la regla del formato: `196610` es OpenGL ES 3.2.
  Poner el texto «OpenGL ES 3.2» en esa propiedad no la cambia, la rompe.
- **Super** — diez fichas del TOP500, con los datos de la lista vigente. **Son simulación**:
  el teléfono *declara* llamarse LineShine; no tiene sus 13.789.440 núcleos. Va aquí y no
  en Perfiles para que nunca se confunda con un perfil de dispositivo.
- **Diagnóstico** — cada afirmación con su estado, las listas del filtro completas, y una
  lista explícita de lo que esta app **no** puede hacer. A la vista, no en un pie.

## Compilar

```bash
./gradlew :app:assembleDebug          # APK de depuración
./gradlew :app:pruebasNucleo          # 70 pruebas del núcleo, sin emulador
```

Para firmar el release hacen falta variables de entorno; **no hay ninguna clave en el
repositorio**:

```bash
export REBORN_KEYSTORE=/ruta/reborn-release.jks
export REBORN_STORE_PASS=...  REBORN_KEY_PASS=...  REBORN_KEY_ALIAS=reborn
./gradlew :app:assembleRelease
```

Sin esas variables el resultado es `app-release-unsigned.apk`. Eso es a propósito: una
llave que se firma sola es una llave que cualquiera puede usar.

## Sin límite de apps

No hay lista blanca ni cuota. Un perfil se aplica al sistema entero: afecta a todas las
apps a la vez, que es lo que hace útil a esta clase de herramienta. No hay «3 perfiles
gratis» ni «desbloquea más dispositivos»: el catálogo entero está abierto.

# Compatibilidad con versiones de Android y con Huawei

Lo que se midió, cómo se midió y **lo que sigue sin probarse**. Sin adornos.

## 1. ¿Por qué el APK pesa menos de 1 MB?

944.179 bytes. Se midió descomprimido por dentro:

| Contenido | Tamaño | % |
|---|---|---|
| Código (`classes.dex`) | 746 KB | **85 %** |
| Recursos | 81 KB | 9 % |
| Librerías nativas (4 arquitecturas) | 37 KB | 4 % |
| Otros | 17 KB | 2 % |

**Es pequeño porque no lleva nada de lo que suele engordar una app:** cero anuncios, cero
analítica, cero SDK de Google Play, cero WebUI, cero fuentes propias, y 5 imágenes que
suman **2.448 bytes** (el icono se genera por código). Todo el peso es lógica.

Comparación honesta: los módulos del directorio que escaneamos pesan 300 KB–68 MB; los de
68 MB llevan JSON y WebUI de configuración. Esta app no lleva ninguna de las dos cosas.

**Tamaño no es calidad, y grande tampoco.** Lo que importa es si funciona, y eso es lo que
viene abajo.

## 2. Versiones de Android: lo que se comprobó

| Comprobación | Cómo | Resultado |
|---|---|---|
| `minSdk` | manifest | **26** (Android 8.0) |
| `targetSdk` | manifest | **35** (Android 15) |
| Mi código llama APIs que no existen en Android 8 | **Lint `NewApi`** | **0** — ninguna |
| Errores de Lint | `lintDebug` | **0 errores**, 7 avisos |
| Dependencias de Google Play / Firebase | `dependencies` | **ninguna** (crítico en Huawei sin GMS) |
| Arquitecturas incluidas | `aapt2 dump badging` | arm64-v8a, armeabi-v7a, x86, x86_64 |
| Permiso de INTERNET | `aapt2 dump permissions` | **0** |

### Un fallo real que encontró Lint

`getInstalledApplications` **dejó de devolver todas las apps en Android 11**. Mi pantalla
de apps habría mostrado cuatro cosas y la promesa de «sin límite de apps» habría sido
falsa en cualquier teléfono moderno. Corregido con `<uses-permission QUERY_ALL_PACKAGES>`
y un bloque `<queries>`. Además, si el sistema devuelve menos de 15 apps, la app **lo
dice** en vez de enseñar una lista corta como si fuera todo.

## 3. Huawei y HarmonyOS

Lo que se puede afirmar y lo que no:

| Afirmación | Estado |
|---|---|
| No usa Google Play Services | **Comprobado** — cero dependencias `gms`/`firebase` |
| No usa anuncios ni analítica | **Comprobado** — cero dependencias |
| Incluye `armeabi-v7a` (Huawei antiguos) | **Comprobado** en el APK |
| Detección de Huawei/Honor | **Implementada y probada** (lógica pura, 6 pruebas) |
| Detección de HarmonyOS | Implementada; lee `Build.DISPLAY` e `INCREMENTAL` |
| **Funciona en un Huawei concreto** | **NO PROBADO.** No tengo ninguno |
| **Funciona en HarmonyOS NEXT** | **No.** NEXT no ejecuta APKs de Android |

Sobre HarmonyOS NEXT, sin rodeos: si el teléfono es HarmonyOS NEXT puro, **ninguna APK de
Android se instala ahí**, ni esta ni ninguna. Si es EMUI/HarmonyOS con base Android (lo
habitual en Huawei hasta ahora), la APK se instala, pero si las funciones de escritura
funcionan depende de si su Shizuku permite ejecutar `wm`, `pm` y `settings`. Eso solo se
sabe probándolo.

## 4. La prueba que corre en tu teléfono

Como no puedo probar cada Android ni cada Huawei, la app **se prueba a sí misma**:
pestaña **Compat.** Lanza de verdad `wm size`, `pm list packages`, `settings get`,
`pm list packages -d`, `echo fox`… y enseña lo que cada una responde, con el texto real
del sistema cuando falla.

- **verde ✓** la función responde en tu equipo
- **rojo ✗** no responde, y dice por qué
- **ámbar —** no se pudo probar (por ejemplo, sin gestor activo)

El resumen no dice «compatible»: dice cuántas pasan y **avisa de que las que fallan no
funcionarán en este teléfono**.

Esa pantalla es la que convierte «no probado» en un dato que puedes leer en 5 segundos en
lugar de descubrirlo a golpes.

## 5. Lo que sigue sin probarse, y no se puede desde aquí

- **Instalar el APK en un teléfono real.** La máquina de compilación no tiene `/dev/kvm`:
  no hay emulador posible.
- **Un Huawei físico.** No hay ninguno disponible.
- **Shizuku real.** La API se verificó leyendo el AAR con `javap` (firmas reales, no de
  memoria), pero el comportamiento en un equipo concreto no se ha visto.

Todo lo demás de este documento está medido, y el comando está escrito al lado de cada
resultado para que se pueda repetir.

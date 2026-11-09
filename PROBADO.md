# Qué está probado y qué no

Máquina de compilación: JDK 21.0.13 · Gradle 8.11.1 · Android SDK platform 35 ·
build-tools 35.0.0.

## Probado de verdad

| Qué | Cómo | Resultado |
|---|---|---|
| Compila | `gradle :app:assembleDebug` | OK |
| Compila release y firma | `gradle :app:assembleRelease` con clave propia | OK, APK de 894 KB |
| Firma verificada | `apksigner verify --print-certs` | SHA-256 `BA:ED:D4:74…7F:FC:CA` |
| **Cero permiso de INTERNET** | `aapt2 dump permissions` sobre el APK | 0 coincidencias |
| Núcleo: catálogo | `gradle :app:pruebasNucleo` | **20 comprobaciones, todas pasan** (contadas en el fuente: `grep -c 'check("'` → 20) |
| Núcleo: escritura | idem, con ejecutor falso | escribe, omite vacíos, confirma |
| Núcleo: bloqueo del fabricante | idem | **no declara éxito si no cambió** |
| Núcleo: sin vía | idem | no aplica nada y lo dice |
| Núcleo: restaurar | idem | vuelve al valor original |
| La suite no da verde regalado | sabotear `ok = true` a propósito | la prueba falla; restaurado, verde |

## NO probado, y por qué

- **No se ha instalado ni arrancado en un teléfono.** Esta máquina no tiene `/dev/kvm`,
  así que no hay emulador posible. Decir «probado en el dispositivo» sería falso.
- **No se ha probado con Shizuku real.** No hay teléfono. La llamada usa la API pública
  `ShizukuSystemProperties.set/get`, cuya firma se comprobó leyendo el AAR 13.1.5 con
  `javap` —no de memoria—, pero el comportamiento en un teléfono concreto no se ha visto.
- **No se sabe si `setprop` de `ro.*` funciona en tu teléfono.** En Android moderno
  muchas `ro.*` son de sólo lectura. La app está escrita para **decirte que no** cuando
  no funcione (relee y compara), pero no puede garantizar que funcione.

Esa última línea es la más importante del documento: ninguna app puede prometer que
cambiar `ro.product.model` va a funcionar en todos los teléfonos, y la que lo prometa
está vendiendo algo que no controla.

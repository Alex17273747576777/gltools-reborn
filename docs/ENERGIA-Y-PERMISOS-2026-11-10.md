# Energía, propiedades y alcance de las pruebas — 2026-11-10

Revisión remota de código; ningún teléfono conectado ni APK nuevo publicado.

## Corrección concurrente conservada
Se parte de `1d1ec7e1`, que eliminó el ajuste global ficticio y corrigió API 30. Esta revisión no reescribe ni descarta ese trabajo.

## Diagnóstico sin cambiar el estado
`cmd power help` puede responder ayuda y código 255: BasicShellCommandHandler imprime la ayuda y devuelve -1. Esto NO demuestra que un servicio esté detenido. La detección examina la ayuda reconocible y distingue orden anunciada, no anunciada y error de consulta. No envía `false` durante una prueba de compatibilidad: podría apagar un modo activo. Anunciar el comando no prueba soporte del Power HAL. Solicitudes explícitas true/false conservan código y salida; una ayuda impresa, aun con código 0, no confirma el setter.

El modo fijo se documenta desde Android 11/API 30 y limita el rango de operación para comparaciones: no es frecuencia máxima ni el modo sostenido de las apps. La aceptación de una orden no mide una mejora física ni autoriza afirmar persistencia. Vigilar temperaturas; no se automatiza este modo al arrancar.

## Propiedades y permisos
SELinux controla cada contexto. UID 0 no elimina esas reglas. Una prueba con debug.* no valida escribir ro.* o persist.* ni demuestra que exista un consumidor de una propiedad inventada. persist.reborn.prueba no es una interfaz de rendimiento. Volver a un valor vacío no elimina necesariamente la clave. Se comprueban código y lectura de restauración; si no se confirma, se comunica el fallo, sin afirmar que todo quedó intacto.

Device Owner utiliza un componente de un DPC provisionado, no un token ni un identificador inventado. No es root. No se ejecuta `dpm set-device-owner`, no se cambian daemons/SELinux/firmware y no se instala init.d. La reanudación de perfiles seguros tras arranque requiere diseño explícito del ciclo de vida y revalidación de privilegios; no se promete permanencia universal de Shizuku ni de propiedades.

## Fuentes primarias consultadas
- https://developer.android.com/games/optimize/adpf/fixed-performance-mode
- https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-13.0.0_r1/services/core/java/com/android/server/power/PowerManagerShellCommand.java
- https://android.googlesource.com/platform/frameworks/libs/modules-utils/+/refs/heads/main/java/com/android/modules/utils/BasicShellCommandHandler.java
- https://source.android.com/docs/core/architecture/configuration/add-system-properties
- https://developer.android.com/tools/adb#dpm

## Pendiente de evidencia del dispositivo
Modelo y compilación exactos, Android/API, vía Shizuku/ADB/root, salida literal y código, mediciones térmicas/rendimiento y repetición tras reiniciar. No se cierran incidencias visuales, de superposición o de persistencia por compilar este código.

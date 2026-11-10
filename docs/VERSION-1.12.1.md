# GLTools Reborn 1.12.1 — preparación de actualización

## Prioridad: captura Screenshot_20261110-141956.jpg
- No usar `persist.reborn.prueba` como prueba ordinaria ADB. Se usa `debug.reborn.prueba`, se verifica código/lectura y se intenta restaurar el valor anterior. No se garantiza borrar una clave nueva.
- No escribir propiedades persistentes durante el diagnóstico informativo de propiedades protegidas, ni siquiera por detectar UID 0. SELinux sigue siendo relevante.
- Ayuda válida de `cmd power` con código 255 no significa servicio detenido. La orden ausente se muestra como no anunciada, sin prometer habilitar un HAL no implementado.
- Consultar la lista de apps deshabilitadas no comprueba capacidad para congelar apps: se corrige el título.
- Las pruebas incluyen los textos de error de la captura y el rechazo de escritura aunque el valor previo fuera 1.
- Incluye los arreglos anteriores de validación de paquetes, códigos y reversiones.

## Firma y publicación
No hay clave recuperada ni Secrets de firma configurados al iniciar esta preparación. No se fabrica una clave, no se usa la de debug y no se publica una actualización incompatible.

Workflow manual `.github/workflows/version.yml`: con `publicar=false`, valida compilación, pruebas y empaquetado; sin clave genera sólo artefactos explícitamente SIN FIRMA / NO INSTALABLES. Con `publicar=true`, exige Secrets y comprueba el certificado de ambos APK antes de publicar.

Configurar **directamente en Settings → Secrets and variables → Actions**, no pegarlos en incidencias, repositorio ni chat:
- `REBORN_KEYSTORE_BASE64`: el keystore ORIGINAL codificado en base64.
- `REBORN_STORE_PASS`
- `REBORN_KEY_ALIAS`
- `REBORN_KEY_PASS`

Certificado esperado de las versiones actuales: `1ff4c6ee90263a73a7ea6097de5b1f31bcd049c805e384904d2087b4c6c9e803`.

Un certificado público o una contraseña solos no recuperan la clave privada. Si no existe copia de esa clave, no se puede prometer una actualización en sitio. La publicación queda bloqueada hasta resolver su custodia. No desinstalar la app ni borrar sus datos como supuesto arreglo.

## Límites
La compilación y las pruebas simuladas no equivalen a comprobar el equipo de la captura. No se garantiza rendimiento máximo, persistencia de privilegios, root ni soporte de la ROM. El modo fijo es de pruebas y no se autoactiva al arrancar.

## Secret único GLTOOLS (JSON)
También se admite `GLTOOLS` como objeto JSON con los campos exactos `REBORN_KEYSTORE_BASE64`, `REBORN_STORE_PASS`, `REBORN_KEY_ALIAS`, `REBORN_KEY_PASS`, todos de tipo string y no vacíos. No pegar aquí sus valores. Tiene prioridad sobre los Secrets separados. Se rechazan JSON/campos/base64 inválidos y claves duplicadas, sin fallback silencioso ni registrar el contenido. Las contraseñas se enmascaran antes de Gradle. La clave se escribe sólo en el runner temporal, con permisos 0600, y se elimina al finalizar. Leer el JSON no acredita que la clave sea correcta: ambos APK deben superar apksigner y coincidir con el certificado esperado antes de publicar.

# Procedencia, licencia y qué se descartó

## Licencia

Este proyecto se distribuye bajo **Apache License 2.0**, la misma licencia del proyecto
del que se tomó la idea general:

> `github.com/KhanhNguyen9872/GLTools_mod_Xposed` — Apache 2.0.
> Se conserva la atribución y el aviso de licencia, como exige la licencia.

No se ha copiado código de ese repositorio a este. Se leyó como referencia y se escribió
de nuevo. Los nombres de paquete, el `applicationId`, el icono y la firma son propios:

- `applicationId`: `io.github.labandita.glreborn`
- Firma: generada aquí, con clave propia (ver `FIRMA.txt`)

## Qué se quitó del proyecto original y por qué

| Se quitó | Por qué |
|---|---|
| `PremiumHook` | Forzaba a `true` la comprobación de pago de una app de terceros. Eso no es una función: es abrir la caja de otro. Fuera. |
| `CoinsManager` y todo lo de monedas | Moneda falsa dentro de una app que no es mía. Fuera. |
| Dependencia del módulo LSPosed | Obligaba a tener Xposed y a apuntar a un solo paquete (`com.superpaninbros.glng`). Aquí no hay Xposed: no hace falta. |
| Scope de un solo paquete | Aquí no hay lista de paquetes: se aplica al sistema, sin límites. |
| `release.jks` + contraseña en `build.gradle` | El original traía el almacén de claves y la contraseña (`gltools123`) en claro. Una llave pública no es una llave. Aquí la firma se lee de variables de entorno y no está en el repositorio. |

## Qué se añadió

- **Shizuku** como vía sin root, por su API pública de propiedades
  (`ShizukuSystemProperties`), no por shell.
- **Confirmación por relectura**: después de aplicar se vuelve a leer y se compara.
  Si el fabricante bloqueó la propiedad, el resultado lo dice con el valor encontrado.
- **Restaurar** a los valores que el teléfono declaró al abrir la app.
- **Catálogo con confianza declarada**: cada perfil dice si está VERIFICADO o PARCIAL,
  y los parciales dejan los campos que no se pudieron comprobar **vacíos**, no inventados.

## Sobre los dos APK que se citaron al empezar

Se comprobó su ficha y conviene dejarlo escrito porque cambia el planteamiento:

- El «GLTools de 11,1 MB» de Appteka es `io.github.gltoolsreborn.standalone` 0.4.1,
  de **otro autor** (EnzoITA85), targetSdk 28. No es el GLTools original ni su
  continuación: es un proyecto distinto que se llama parecido.
- **Ultra Ram Swapper** (10,66 MB, `com.thesparks.ultra_ram_swapper`) no contiene ningún
  «robot»: crea un archivo de swap sin root. No había nada que replicar ahí.

Por eso este proyecto no imita a ninguno de los dos: hace su propia función —perfiles de
dispositivo— con su propio nombre.

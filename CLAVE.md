# Firma del APK — y un error que hay que decir

## Estado actual

Hay **dos claves** en la historia de este proyecto. Esto es importante antes de publicar
nada, porque Android identifica una app por su firma.

| | Huella SHA-256 | Estado |
|---|---|---|
| Firmó el **APK 1.1.0** publicado en este repositorio | `BA:ED:D4:74:39:0C:E5:33:94:A0:92:EA:49:C2:0C:0C:72:48:DA:96:1F:D7:21:01:AF:6B:06:A4:64:7F:FC:CA` | **clave perdida** |
| Clave actual (en poder del autor) | `1F:F4:C6:EE:90:26:3A:73:A7:EA:60:97:DE:5B:1F:31:BC:D0:49:C8:05:E3:84:90:4D:20:87:B4:C6:C9:E8:03` | válida, firma APKs correctamente |

## Qué implica, sin adornos

**El APK 1.1.0 no se puede actualizar encima con la clave actual.** Android rechaza
instalar una versión nueva firmada con otra identidad. Quien tenga la 1.1.0 instalada
tendrá que **desinstalarla** antes de instalar la próxima.

No hay forma de arreglar eso retroactivamente: la firma es criptográfica. La única salida
es publicar la próxima versión con la clave nueva y avisar de la desinstalación previa.

## Cómo pasó

Al limpiar el espacio de trabajo borré la carpeta del proyecto local —incluido
`reborn-release.jks`— sin comprobar antes qué contenía. El `.jks` no estaba en el
repositorio (correcto: no debe estarlo) y no había copia en ningún otro sitio.

Fue un descuido al limpiar, no un fallo de Android ni del sistema de archivos.

## Cómo no vuelve a pasar

- El `.jks` y su contraseña viven **fuera** del repositorio, en una carpeta con permisos
  `700`, y están anotados en el archivo que el autor guarda aparte.
- La clave se comprueba antes de cualquier publicación: `apksigner verify --print-certs`
  debe dar la huella de la tabla de arriba.
- Para firmar hacen falta variables de entorno (`REBORN_KEYSTORE`, `REBORN_STORE_PASS`,
  `REBORN_KEY_ALIAS`, `REBORN_KEY_PASS`). Sin ellas el build produce
  `app-release-unsigned.apk` y **no** se firma solo.

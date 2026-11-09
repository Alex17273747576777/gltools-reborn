# Con qué gestor de Shizuku funciona esto

## Respuesta corta

**Con Shizuku normal y con OmniShizuku.** No hace falta cambiar nada en la app.

## Por qué, medido

`Shizuku` no es solo una app: es un **protocolo**. La app cliente declara el permiso
`moe.shizuku.manager.permission.API_V23` y habla con el binder que expone el gestor.
Cualquier gestor que implemente ese permiso sirve.

Comprobado en el código de OmniShizuku (`git clone` del repo, 31 MB, 8-nov-2026):

```
stub/src/shizuku/AndroidManifest.xml → moe.shizuku.manager.permission.API_V23
probe/src/terminal/AndroidManifest.xml → moe.shizuku.manager.permission.API_V23
```

Es **el mismo permiso** que declara esta app. Por eso funciona con los dos gestores, y
también con el Shizuku original de Rikka.

| Gestor | ¿Funciona esta app? | Por qué |
|---|---|---|
| Shizuku (Rikka) | **Sí** | Implementa `API_V23` |
| **OmniShizuku** | **Sí** | Declara el mismo `API_V23` en `stub/src/shizuku` |
| Dhizuku | **No** | Es otro protocolo: Device Owner, no shell de ADB |

## Sobre Dhizuku, que no es lo mismo

OmniShizuku incluye un *stub* de Dhizuku (`stub/src/dhizuku/java/com/rosan/dhizuku/`,
dos archivos: un `Provider` y una `Activity` de reenvío). Eso **no es Dhizuku**: es un
puente para que OmniShizuku haga de intermediario.

Dhizuku de verdad da permisos de **Device Owner** (administrador del dispositivo), que
sirve para gestionar apps y políticas, no para ejecutar shell. Esta app necesita shell:
`wm`, `pm`, `settings`, `cmd`. Por eso **Dhizuku no le sirve**, y decirlo es más útil que
un botón que falle.

## Sobre los módulos de magiskmodule.com en OmniShizuku

OmniShizuku tiene su propio sistema de módulos (`README.md`, sección *Modules System*):
ZIPs con `install.sh`, `service.sh` y `boot.sh`, con WebUI, como los de Magisk pero
ejecutados por el shell de Shizuku. Muchos de los módulos de
`magiskmodule.com/category/axmanager-plugin` encajan ahí.

**Esta app no instala módulos.** No es una decisión de pereza: instalar y ejecutar
scripts de terceros en el shell del teléfono es una superficie de ataque que una app de
perfiles no debería abrir. Si quieres módulos, OmniShizuku los instala; GLTools Reborn
hace perfiles y utilidades concretas, cada una reversible y verificada.

**No son competencia: se complementan.** Los dos pueden estar instalados a la vez,
porque usan `applicationId` distintos (`com.krishna.omnishizuku` y
`io.github.labandita.glreborn`).

## Cómo activarlo

1. Instala OmniShizuku (o Shizuku).
2. Actívalo por depuración inalámbrica, igual que Shizuku.
3. Abre GLTools Reborn: la barra de arriba debe poner **SHIZUKU** en verde.
4. Si dice **SIN PERMISO**, entra en OmniShizuku y autoriza GLTools Reborn en su lista.

Si dice **SIN VÍA**, el gestor no está arrancado. La app no finge lo contrario.

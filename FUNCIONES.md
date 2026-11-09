# De dónde salió cada función

Escaneo de `magiskmodule.com` hecho el 8-nov-2026. El sitio es un **directorio**, no un
conjunto de repositorios: las fichas publican un `.zip` y, como mucho, el enlace al
perfil del autor. **No escaneé código fuente de nadie** — no hay fuente que escanear en
la mayoría de las fichas. Lo que hice fue leer qué hace cada módulo y volver a escribir
esa función con la API de Shizuku.

## Lo que hay en el sitio (medido)

| Categoría | Módulos |
|---|---|
| Módulos de root (Magisk / KernelSU / APatch) | **431** |
| Apps con Shizuku | **90** |
| Herramientas sin root | **21** |

**Casi todo lo del sitio requiere root**, y esta app no. Por eso solo sirven las dos
últimas categorías.

## Recreado — y por qué es viable sin root

| Función | Idea de | Cómo se hace aquí |
|---|---|---|
| Cambio de resolución con vuelta atrás | **Benimaru Tool** (Shizuku) | `wm size` + `wm density`, con densidad recalculada y un `sleep 15` que revierte aunque la app muera |
| Frecuencia de refresco | Benimaru Tool | `settings put system peak_refresh_rate` |
| Congelar / descongelar apps | **ZKMNEXT GameSpace**, Buge App Manager | `pm disable-user` / `pm enable`, con lista de paquetes intocables |
| Limpiar caché | **Cache Cleaner** | `rm -rf /data/data/<pkg>/cache` por shell en vez de un servicio de accesibilidad pulsando por ti |
| DNS privado | Benimaru Tool | `settings put global private_dns_mode` |
| Escala de animación | tweaks habituales | `settings put global *_animation_scale` |
| Modo rendimiento | Performance Mode, KinetiX | `cmd power set-fixed-performance-mode-enabled` |

## Lo que decidí NO copiar

| Descartado | De dónde | Por qué |
|---|---|---|
| Desactivar el limitador térmico | **Shelby Auto Thermal**, HyperCool | El limitador térmico existe para que el teléfono no se fría. Quitarlo puede dañar el hardware de verdad. Esta app no lo hace, ni con modo experto. |
| Frecuencias de CPU/GPU fijas | HyperCool, Kernel Enhancer, CPU GPU Tweak | Requiere escribir en nodos del kernel. Sin root no se puede; con root es exactamente donde se rompen cosas. Fuera. |
| Optimizaciones «mágicas» de RAM | VOID RAM, Turbo Boost | Los números de memoria libre no son un problema que se arregle matando procesos; Android ya gestiona eso. Sería vender humo. |
| Inyección de perfiles falsos en APKs ajenos | Graphics Unlocker (método LSPosed) | Modificar el comportamiento de otra app por dentro. Ya se decidió en el tramo anterior que no. |
| Cualquier cosa que toque particiones o AVB | NoMount Suite, Zygisk Next | Esta app no monta nada ni desmonta nada. |

## Sobre «un vagabundo para elegir rumbo»

Esa función —un perfil de rendimiento por juego, que cambia según qué app está abierta—
**no está implementada**. Sería lo siguiente si hace falta, y se podría hacer sin root:
detectar la app en primer plano con permiso de uso y aplicar el perfil. No la incluí
porque no está terminada, y prefiero decir eso a poner un botón que no hace nada.

## Aviso que también estaba en el sitio

Las fichas de magiskmodule.com llevan su propio descargo: *«altera ajustes del kernel y
del sistema; úsalo bajo tu responsabilidad»*. Aplica igual aquí. La diferencia es que
esta app **verifica después de escribir** y **revierte sola**; los módulos de root, no.

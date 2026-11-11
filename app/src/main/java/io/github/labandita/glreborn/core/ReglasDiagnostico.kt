package io.github.labandita.glreborn.core

/**
 * Las reglas de lo que se mide y, sobre todo, de CÓMO SE LLAMA lo que se mide.
 *
 * Van aparte y sin Android delante a propósito: lo que más se equivoca en un diagnóstico no es la
 * lectura, es la interpretación y el nombre. Un número bien leído con un nombre malo miente igual
 * que uno mal leído. Aquí se puede probar todo en la máquina de compilación.
 */
object ReglasDiagnostico {

    const val NOMBRE_CPU = "Velocidad de CPU (un hilo)"
    const val NOMBRE_TERMICO = "Estado térmico declarado por el sistema"
    const val NOMBRE_MARGEN = "Margen térmico declarado (no son grados)"
    const val NOMBRE_ESCRITURA = "Escritura en almacenamiento"

    /**
     * Lo que la prueba de CPU mide de verdad: operaciones de punto flotante por segundo en UN solo
     * hilo. Hasta la 1.13.2 se llamaba «Carga de CPU» y no medía carga ninguna: medía cuánto
     * trabajo saca un núcleo. Es el mismo vicio que llamar «congelar apps» a un
     * `pm list packages -d`, y se arregla igual: el nombre dice lo que hay.
     */
    fun cpu(vueltas: Long, msReales: Long, msPedidos: Long, nucleos: Int): Medida {
        if (vueltas <= 0L || msReales <= 0L) {
            return Medida(NOMBRE_CPU, null, "Mops/s", "", "la prueba no llegó a completar un bloque")
        }
        return Medida(
            NOMBRE_CPU,
            vueltas / 1_000_000.0 / (msReales / 1000.0),
            "Mops/s",
            "operaciones de punto flotante por segundo en UN SOLO HILO durante $msReales ms " +
                "(se pidieron $msPedidos) · $nucleos núcleos disponibles · no es la carga de la CPU " +
                "ni una puntuación comparable con AnTuTu",
        )
    }

    /*
     * Los siete estados térmicos de `PowerManager.getCurrentThermalStatus` (API 29), con la
     * descripción que da la documentación oficial de Android, traducida literalmente:
     *
     *   NONE      "Not under throttling"
     *   LIGHT     "Light throttling where UX is not impacted"
     *   MODERATE  "Moderate throttling where UX is not largely impacted"
     *   SEVERE    "Severe throttling where UX is largely impacted"
     *   CRITICAL  "Platform has done everything to reduce power"
     *   EMERGENCY "Key components in platform are shutting down due to thermal condition"
     *   SHUTDOWN  "Need shutdown immediately"
     *
     * Se trabaja con la ETIQUETA y no con el número a propósito: los valores numéricos de esas
     * constantes no aparecen en la documentación, y escribirlos de memoria sería inventarlos. El
     * nivel 0-6 que se enseña es NUESTRA escala sobre esas etiquetas, y se dice en el detalle.
     */
    val ESTADOS_TERMICOS = listOf(
        "NONE" to "sin estrangulamiento térmico",
        "LIGHT" to "estrangulamiento ligero: no afecta al uso",
        "MODERATE" to "estrangulamiento moderado: no afecta mucho al uso",
        "SEVERE" to "estrangulamiento serio: el uso ya se ve afectado",
        "CRITICAL" to "crítico: la plataforma ya hizo todo lo que puede para reducir consumo",
        "EMERGENCY" to "emergencia: componentes clave se están apagando por temperatura",
        "SHUTDOWN" to "hay que apagar de inmediato",
    )

    /** Desde qué nivel el calor ya está afectando al teléfono, según la propia documentación. */
    fun preocupa(etiqueta: String): Boolean {
        val i = ESTADOS_TERMICOS.indexOfFirst { it.first == etiqueta }
        return i >= 3
    }

    fun termico(etiqueta: String?, api: Int): Medida {
        if (api < 29) {
            return Medida(
                NOMBRE_TERMICO, null, "",
                "Android sólo publica el estado térmico a partir de la API 29; este teléfono está en la $api",
                "la API del teléfono no ofrece getCurrentThermalStatus",
            )
        }
        if (etiqueta == null) {
            return Medida(NOMBRE_TERMICO, null, "", "", "el sistema no devolvió estado térmico")
        }
        val i = ESTADOS_TERMICOS.indexOfFirst { it.first == etiqueta }
        if (i < 0) {
            return Medida(
                NOMBRE_TERMICO, null, "",
                "el sistema dio un estado que esta versión no reconoce",
                "estado desconocido «$etiqueta»: no se traduce a un nivel inventado",
            )
        }
        return Medida(
            NOMBRE_TERMICO,
            i.toDouble(),
            "nivel 0-6",
            ESTADOS_TERMICOS[i].second + " · es un NIVEL declarado por el sistema en la escala de " +
                "esta app, no una temperatura en grados ni el valor numérico de Android",
        )
    }

    /**
     * `PowerManager.getThermalHeadroom` (API 30) devuelve cuánto margen queda antes del
     * estrangulamiento serio. La documentación lo llama "estimate": es una ESTIMACIÓN del sistema,
     * y se etiqueta como tal, igual que el ancho de banda del enlace de red.
     */
    fun margenTermico(margen: Float?, api: Int): Medida = when {
        api < 30 -> Medida(
            NOMBRE_MARGEN, null, "",
            "getThermalHeadroom existe desde la API 30; este teléfono está en la $api",
            "la API del teléfono no ofrece margen térmico",
        )
        margen == null -> Medida(NOMBRE_MARGEN, null, "", "", "el sistema no devolvió margen térmico")
        else -> Medida(
            NOMBRE_MARGEN, margen.toDouble(), "0 a 1",
            "estimación que declara el sistema de cuánto queda antes del estrangulamiento serio; 1 " +
                "es margen de sobra y 0 o menos es que ya se está estrangulando. Es una estimación " +
                "del sistema, no una temperatura medida",
        )
    }

    /**
     * Escritura en almacenamiento. `conFsync` cambia lo que significa el número y por eso va en el
     * detalle: sin `fsync` se mide sobre todo la caché de páginas, y llamar a eso «velocidad del
     * almacenamiento» es vender una cifra que el teléfono no ha hecho.
     */
    fun escrituraIo(bytes: Int, ns: Long, conFsync: Boolean): Medida {
        if (bytes <= 0 || ns <= 0L) {
            return Medida(NOMBRE_ESCRITURA, null, "MB/s", "", "no se pudo escribir")
        }
        return Medida(
            NOMBRE_ESCRITURA,
            bytes / (ns / 1e9) / (1024 * 1024),
            "MB/s",
            "${bytes / 1048576} MB a la caché de la app " +
                if (conFsync) "con fsync: incluye lo que tardó en llegar al almacenamiento, no sólo a la caché"
                else "SIN fsync: puede estar midiendo la caché de páginas, no el almacenamiento",
        )
    }

    /**
     * Qué claves se ejecutan con este perfil. Hasta la 1.13.2 el perfil decía «las que no estén no
     * se ejecutan» y era mentira: `ejecutar` nunca recibía la lista y se ejecutaban todas. Ahora la
     * misma función decide en el teléfono y en las pruebas, para que no pueda haber dos versiones de
     * la verdad.
     */
    fun seleccion(pruebas: Set<String>, profundo: Boolean): Set<String> {
        val s = pruebas.intersect(PerfilDiagnostico.TODAS).toMutableSet()
        // Lo profundo cuesta tiempo de verdad; si el perfil dice rápido, no se hace.
        if (!profundo) {
            s.remove(PerfilDiagnostico.RAM)
            s.remove(PerfilDiagnostico.IO)
        }
        return s
    }

    /** Cuánto dura la prueba de CPU. El perfil lo elige (150/400/1000) y antes se ignoraba. */
    fun msCpu(msCpu: Int): Long = msCpu.coerceIn(50, 5000).toLong()
}

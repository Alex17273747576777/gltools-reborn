package io.github.labandita.glreborn.core

/**
 * SIMULACIÓN. Esto no convierte un teléfono en una supercomputadora.
 *
 * Son fichas para que el teléfono DECLARE los datos de los sistemas reales del TOP500.
 * Un juego que pregunte «¿cuántos núcleos tienes?» leerá 13.789.440 y hará lo que haga
 * con ese número; los núcleos del teléfono siguen siendo los que son. Poner aquí una
 * cifra grande no añade un solo gigaflop.
 *
 * Por eso estas fichas NO son perfiles de dispositivo y no se mezclan con ellos: van en
 * su propia sección, con la etiqueta SIMULACIÓN a la vista, para que nunca se confundan
 * con un «tu teléfono es un Pixel 9». Una ficha de supercomputadora dice la verdad del
 * sistema que describe y miente sobre el teléfono, a propósito y avisando.
 *
 * Fuente: TOP500, 67.ª edición, 17 de junio de 2026.
 * https://www.top500.org/lists/top500/2026/06/
 */
data class Supercomputadora(
    val rank: Int,
    val nombre: String,
    val sitio: String,
    val pais: String,
    val nucleos: Int,
    val rmaxPFlops: Double,
    val rpeakPFlops: Double,
    val potenciaKw: Int,   // 0 = TOP500 no lo publica
    val nota: String,
)

object Supercomputadoras {

    /** Los diez primeros de la lista vigente. Todos los números, copiados de la tabla. */
    val todos: List<Supercomputadora> = listOf(
        Supercomputadora(1, "LineShine", "NSCS Shenzhen", "China", 13_789_440, 2_198.40, 2_735.82, 42_220,
            "LingKun, LX2 304C 1.55 GHz, LingQi, Kylin OS. Primero de China en liderar desde 2017."),
        Supercomputadora(2, "El Capitan", "LLNL", "Estados Unidos", 11_340_000, 1_809.00, 2_821.10, 29_685,
            "HPE Cray EX255a, EPYC 24C 1.8 GHz, Instinct MI300A, Slingshot-11."),
        Supercomputadora(3, "Frontier", "Oak Ridge", "Estados Unidos", 9_066_176, 1_353.00, 2_055.72, 24_607,
            "HPE Cray EX235a, EPYC 64C 2 GHz, Instinct MI250X, Slingshot-11."),
        Supercomputadora(4, "Aurora", "Argonne", "Estados Unidos", 9_264_128, 1_012.00, 1_980.01, 38_698,
            "HPE Cray EX, Xeon CPU Max 9470 52C 2.4 GHz, Data Center GPU Max, Slingshot-11."),
        Supercomputadora(5, "JUPITER Booster", "EuroHPC / FZJ", "Alemania", 4_801_344, 1_000.00, 1_226.28, 15_794,
            "BullSequana XH3000, GH Superchip 72C 3 GHz, GH200, InfiniBand NDR200."),
        Supercomputadora(6, "HPC7", "Eni S.p.A.", "Italia", 3_461_472, 571.50, 861.13, 8_735,
            "HPE Cray EX255a, EPYC 24C 1.8 GHz, Instinct MI300A, Slingshot-11, RHEL 9."),
        Supercomputadora(7, "Eagle", "Microsoft Azure", "Estados Unidos", 2_073_600, 561.20, 846.84, 0,
            "Microsoft NDv5, Xeon Platinum 8480C 48C, H100, InfiniBand NDR. TOP500 no publica su potencia."),
        Supercomputadora(8, "HPC6", "Eni S.p.A.", "Italia", 3_143_520, 477.90, 606.97, 8_461,
            "HPE Cray EX235a, EPYC 64C 2 GHz, Instinct MI250X, Slingshot-11, RHEL 8.9."),
        Supercomputadora(9, "Supercomputer Fugaku", "RIKEN", "Japón", 7_630_848, 442.01, 537.21, 29_899,
            "Fujitsu A64FX 48C 2.2 GHz, Tofu interconnect D. Sin GPU: sólo CPU ARM."),
        Supercomputadora(10, "Alps", "CSCS", "Suiza", 2_121_600, 434.90, 574.84, 7_124,
            "HPE Cray EX254n, Grace 72C 3.1 GHz, GH200, Slingshot-11."),
    )

    /** Lo que se puede escribir de una ficha, y lo que NO sirve de nada escribir. */
    fun propiedadesDe(s: Supercomputadora): Map<String, String> = mapOf(
        // El nombre del sistema sí existe como propiedad y los juegos lo leen.
        "ro.product.model" to s.nombre,
        "ro.product.marketname" to s.nombre,
        "ro.product.brand" to s.pais,
        // El número de núcleos NO es una propiedad de Android. Se listaría aquí y sería
        // mentira: no existe la clave. Se deja fuera a propósito y se dice en la pantalla.
    )

    /** Los datos que NO tienen propiedad donde meterse, para enseñarlos como lo que son. */
    fun datosSoloInformativos(s: Supercomputadora): List<Pair<String, String>> = listOf(
        "Núcleos" to "%,d".format(s.nucleos),
        "Rmax" to "%.2f PFlop/s".format(s.rmaxPFlops),
        "Rpeak" to "%.2f PFlop/s".format(s.rpeakPFlops),
        "Potencia" to if (s.potenciaKw == 0) "no publicado" else "%,d kW".format(s.potenciaKw),
        "Sitio" to "${s.sitio} · ${s.pais}",
    )

    fun porNombre(n: String): Supercomputadora? = todos.firstOrNull { it.nombre == n }

    fun censo(): String = "10 sistemas · TOP500 jun-2026 · simulación, no rendimiento"
}

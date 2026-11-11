package io.github.labandita.glreborn.core

/**
 * Los textos en los que la interfaz PROMETE algo, derivados de hechos.
 *
 * Caza de promesas rotas de la 1.16.0. Se leyeron las 78 frases visibles que afirmaban un
 * comportamiento y se contrastaron una a una con el código. Cinco decían algo que el código no
 * hace, y las cinco tenían el mismo origen: la frase estaba escrita a mano en la pantalla, lejos de
 * la condición que la hace cierta o falsa. Aquí pasan a ser funciones de los hechos de los que
 * dependen (nivel de API, permiso concedido, ajuste del perfil, si hay vía), y así las puede probar
 * `pruebasNucleo` en JVM. Una frase escrita a mano no se puede probar; una que sale de un hecho, sí.
 *
 * Las que se comprobaron y resultaron VERDADERAS no se tocan, y quedan anotadas aquí para que nadie
 * las "arregle" sin mirar: el comando de vuelta de la resolución sí se lanza desacoplado
 * (`sh -c 'sleep N; wm size reset; wm density reset' >/dev/null 2>&1 &`, en `core/Pantalla.kt`),
 * cada escritura sí se anota antes de hacerse (`Escritor.escribirConRegistro`), el modo sí persiste
 * entre arranques (`Director.cargarModo`), «Seguir» sí borra el parte (`RegistroFallos.borrar` desde
 * `MainActivity.onContinuar`), la posición de la capa sí se guarda (`AjustesSuperposicion.
 * guardarPosicion`), y `Registro.deshacerTodo` sí vacía el cuaderno al terminar, así que la
 * reparación no se repite en cada arranque.
 *
 * Puro a propósito: sin Android, sin Context, sin IO. Si hace falta un hecho nuevo, se pasa como
 * parámetro.
 */
object ReglasPromesas {

    /** Android 13: `POST_NOTIFICATIONS` pasa a ser un permiso que se pide en runtime. */
    const val API_NOTIFICACIONES = 33

    /** Android 10: `MediaStore` deja escribir en Documentos con `RELATIVE_PATH`, sin permisos. */
    const val API_DOCUMENTOS = 29

    /**
     * Qué permisos necesita la capa superpuesta.
     *
     * Decía «es el único permiso que necesita», y era falso en Android 13 o más: la misma pantalla
     * promete una notificación fija, el servicio va en primer plano (`startForeground`), y la app
     * pide `POST_NOTIFICATIONS` en `MainActivity.pedirNotificaciones()`. Pero tampoco se puede decir
     * que sea obligatorio: sin él la capa dibuja igual, lo que no se ve es el aviso.
     */
    fun permisosSuperposicion(api: Int): String =
        if (api >= API_NOTIFICACIONES) {
            "Dale permiso de dibujar encima de otras apps. En Android 13 o más hace falta también " +
                "el de notificaciones para ver el aviso fijo con el botón «Quitar»: sin él la capa " +
                "sigue funcionando, pero el aviso no se ve. Los dos se pueden quitar cuando quieras."
        } else {
            "Dale permiso de dibujar encima de otras apps. Es el único que necesita este teléfono y " +
                "se lo puedes quitar cuando quieras."
        }

    /** El aviso de debajo, que prometía una notificación visible sin decir de qué depende. */
    fun avisoSuperposicion(api: Int): String =
        "Ni root ni Shizuku: esto usa la API pública de superposiciones de Android. Mientras está " +
            "encendida hay un servicio en primer plano con una notificación y el botón «Quitar»: " +
            "Android no permite dibujar encima de la pantalla en silencio, y hace bien." +
            if (api >= API_NOTIFICACIONES) {
                " Si no diste el permiso de notificaciones, la capa funciona igual y el aviso no se ve."
            } else {
                ""
            }

    /**
     * Para qué sirve el permiso de Shizuku.
     *
     * Decía «Solo Shizuku puede concederlo; sin él la app no puede hacer nada», y era falso dos
     * veces: con root se escribe sin Shizuku (lo dicen `Arquitectura.banner` y
     * `Director.descripcion`), y sin ninguna vía leer y buscar siguen funcionando (lo dice la propia
     * pantalla de Arquitectura, unos renglones más arriba).
     */
    fun permisoShizuku(): String =
        "Deja que GLTools pida permiso a Shizuku para ejecutar órdenes de shell. Es una de las dos " +
            "vías para escribir; la otra es root. Sin ninguna de las dos, leer y buscar siguen " +
            "funcionando, pero no se cambia nada del teléfono."

    /**
     * Qué pasa al cerrar la app cuando no se midió nada.
     *
     * Prometía el informe al cerrar sin mirar `perfil.alCerrar`, y la misma pantalla enseña más
     * arriba «Cerrar la app genera informe: no». Con el ajuste desactivado, esperar un informe es
     * esperar algo que no va a pasar.
     */
    fun informeAlCerrar(alCerrar: Boolean): String =
        if (alCerrar) {
            "Todavía no se midió nada en esta sesión. Al cerrar la app se genera el informe, con lo " +
                "que haya en ese momento."
        } else {
            "Todavía no se midió nada en esta sesión. El perfil tiene desactivado el informe al " +
                "cerrar: no se generará solo. Usa «Guardar informe ahora»."
        }

    /**
     * Dónde acaba el informe y cuándo se genera.
     *
     * Decía «Se guarda en Documentos/GLTools Reborn» siempre, y `Informes.guardar` sólo escribe ahí
     * en Android 10 o más: en Android 8 y 9 (`minSdk 26`) va a `getExternalFilesDir` y devuelve la
     * ruta exacta. El código ya lo documentaba con honestidad en su propio KDoc; la etiqueta no.
     */
    fun destinoInforme(api: Int, alCerrar: Boolean): String {
        val cuando = if (alCerrar) {
            "El informe se genera solo al cerrar la app, y también con «Guardar informe ahora»."
        } else {
            "El perfil tiene desactivado el informe al cerrar: se genera con «Guardar informe ahora»."
        }
        val donde = if (api >= API_DOCUMENTOS) {
            " Se guarda en Documentos/GLTools Reborn."
        } else {
            " En este teléfono (Android 9 o más viejo) no se puede escribir en Documentos sin " +
                "permiso de almacenamiento, así que se guarda en la carpeta de la app y se dice la " +
                "ruta exacta."
        }
        return cuando + donde +
            " Además se puede mandar por correo o mensajería: sale de tu teléfono sólo si tú eliges a dónde."
    }

    /**
     * La auto-reparación del modo experto.
     *
     * Es verdad que existe (`Director.refrescar` llama a `registro.deshacerTodo`), pero sólo con dos
     * condiciones que la frase no decía: seguir en modo experto al abrir, y que haya vía. Sin vía no
     * se puede escribir, tampoco para deshacer, y el cuaderno se queda guardado hasta que la haya.
     */
    fun recuperacionExperto(conVia: Boolean): String {
        val cabeza = "El modo experto toca API, versión de Android, huella y kernel. Cada escritura " +
            "se anota antes de hacerla."
        return if (conVia) {
            cabeza + " Si la app muere a mitad, al abrirla se devuelve sola lo que quedó escrito."
        } else {
            cabeza + " Si la app muere a mitad, el cuaderno se queda guardado y se devuelve en " +
                "cuanto haya vía (Shizuku en marcha o root): sin vía no se puede escribir, tampoco " +
                "para deshacer."
        }
    }
}

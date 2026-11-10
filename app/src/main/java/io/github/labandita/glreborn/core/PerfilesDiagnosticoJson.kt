package io.github.labandita.glreborn.core

import android.content.Context
import java.io.File

/**
 * Varios perfiles de diagnóstico, con nombre, guardados en JSON.
 *
 * Antes había **uno** y en formato `clave=valor`. El usuario pidió poder crear perfiles
 * personalizados: uno para mirar rápido, otro para el informe completo que se manda por correo,
 * otro para cuando el teléfono va lento. Eso necesita nombre, lista y un formato que se pueda
 * llevar de un teléfono a otro. De ahí el JSON.
 *
 * Tres cosas que esta clase hace a propósito, y se pueden probar sin Android:
 *
 *  1. **No pierde nada de lo anterior.** Al leer, si existe el fichero viejo `clave=valor` y no
 *     hay JSON, se importa como perfil «Equilibrado». Migrar no puede significar empezar de cero.
 *  2. **No se cae con un fichero roto.** Un JSON inválido devuelve los perfiles de fábrica y un
 *     aviso con la línea del error; el fichero roto se aparta a `.roto` en vez de borrarse.
 *  3. **Los avisos se dicen.** Validar un perfil que trae un umbral imposible devuelve el perfil
 *     corregido *y* la frase que explica qué se cambió.
 */
data class PerfilGuardado(
    val nombre: String,
    val perfil: PerfilDiagnostico,
    /** Si es el que se usa al abrir la app. Sólo uno puede serlo. */
    val activo: Boolean = false,
)

object ReglasPerfilesJson {

    /** El techo de perfiles. Es una lista para elegir, no una base de datos. */
    const val MAX_PERFILES = 12

    const val CLAVE_PERFILES = "perfiles"

    /** Los tres de fábrica: sirven para algo distinto y se explican solos. */
    fun deFabrica(): List<PerfilGuardado> = listOf(
        PerfilGuardado(
            "Rápido",
            PerfilDiagnostico(
                nombre = "Rápido", profundo = false,
                pruebas = setOf(PerfilDiagnostico.MEMORIA, PerfilDiagnostico.ALMACENAMIENTO, PerfilDiagnostico.BATERIA),
                msCpu = 150, alCerrar = true, conSonido = false,
            ),
            activo = false,
        ),
        PerfilGuardado(
            "Equilibrado",
            PerfilDiagnostico(nombre = "Equilibrado"),
            activo = true,
        ),
        PerfilGuardado(
            "Informe completo",
            PerfilDiagnostico(
                nombre = "Informe completo", profundo = true,
                pruebas = PerfilDiagnostico.TODAS, msCpu = 1000,
                umbralMemoriaPct = 85, umbralBateriaC = 40.0,
                alCerrar = true, conSonido = false,
            ),
            activo = false,
        ),
    )

    /** Recorta el nombre para que quepa y no rompa la lista: 1–28 caracteres, sin saltos. */
    fun nombreLimpio(bruto: String, ocupados: List<String> = emptyList()): String {
        val base = bruto.replace(Regex("[\\r\\n\\t]"), " ").trim().ifBlank { "Personalizado" }.take(28)
        if (base !in ocupados) return base
        // Si ya existe, se numera en vez de sobrescribir el del usuario sin avisar.
        var n = 2
        while ("$base $n" in ocupados) n++
        return "$base $n".take(28)
    }

    /** Deja exactamente un activo. Si no hay ninguno, activa el primero. */
    fun normalizarActivo(lista: List<PerfilGuardado>): List<PerfilGuardado> {
        if (lista.isEmpty()) return deFabrica()
        val primeroActivo = lista.indexOfFirst { it.activo }
        return lista.mapIndexed { i, p ->
            p.copy(activo = if (primeroActivo < 0) i == 0 else i == primeroActivo)
        }
    }

    /** Añade uno nuevo y lo deja activo. Respeta el techo devolviendo la lista sin cambios. */
    fun anadir(
        lista: List<PerfilGuardado>, nombre: String, perfil: PerfilDiagnostico,
    ): Pair<List<PerfilGuardado>, String?> {
        if (lista.size >= MAX_PERFILES) {
            return lista to "ya hay $MAX_PERFILES perfiles: borra uno antes de crear otro"
        }
        val limpio = nombreLimpio(nombre, lista.map { it.nombre })
        val (valido, avisos) = ReglasPerfilDiagnostico.validar(perfil.copy(nombre = limpio))
        val nuevos = lista.map { it.copy(activo = false) } +
            PerfilGuardado(limpio, valido, activo = true)
        return normalizarActivo(nuevos) to avisos.joinToString("; ").ifBlank { null }
    }

    /** Borra por nombre. No se puede quedar sin ninguno: si era el último, vuelven los de fábrica. */
    fun borrar(lista: List<PerfilGuardado>, nombre: String): Pair<List<PerfilGuardado>, String?> {
        val quedan = lista.filterNot { it.nombre == nombre }
        if (quedan.isEmpty()) return deFabrica() to "se borró el último perfil: vuelven los tres de fábrica"
        return normalizarActivo(quedan) to null
    }

    fun activo(lista: List<PerfilGuardado>): PerfilGuardado? =
        lista.firstOrNull { it.activo } ?: lista.firstOrNull()

    // ── Codificar y decodificar ───────────────────────────────────────────────

    fun codificar(lista: List<PerfilGuardado>): String = MiniJson.escribir(
        linkedMapOf(
            "formato" to 1,
            "nota" to "Perfiles del auto-diagnóstico de GLTools Reborn. Se pueden editar a mano.",
            CLAVE_PERFILES to lista.map { p ->
                linkedMapOf(
                    "nombre" to p.nombre,
                    "activo" to p.activo,
                    "profundo" to p.perfil.profundo,
                    "pruebas" to p.perfil.pruebas.sorted(),
                    "msCpu" to p.perfil.msCpu,
                    "umbralMemoriaPct" to p.perfil.umbralMemoriaPct,
                    "umbralBateriaC" to p.perfil.umbralBateriaC,
                    "alCerrar" to p.perfil.alCerrar,
                    "conSonido" to p.perfil.conSonido,
                )
            },
        ),
    )

    /**
     * Decodifica. Lo que no entienda, lo dice; lo que falte, lo pone por defecto. Nunca lanza:
     * se llama al abrir la app, donde no hay nadie para ver una excepción.
     */
    fun decodificar(texto: String): Triple<List<PerfilGuardado>, List<String>, Boolean> {
        val avisos = mutableListOf<String>()
        val raiz = try {
            MiniJson.objeto(MiniJson.leer(texto))
        } catch (e: Exception) {
            return Triple(
                deFabrica(),
                listOf("el fichero de perfiles no es JSON válido: ${e.message}. Se usan los de fábrica"),
                true,
            )
        }
        val bruto = raiz[CLAVE_PERFILES]
            ?: return Triple(
                deFabrica(),
                listOf("el fichero no tiene la lista «perfiles»: se usan los de fábrica"),
                true,
            )
        val lista = try {
            MiniJson.lista(bruto)
        } catch (e: Exception) {
            return Triple(deFabrica(), listOf("«perfiles» no es una lista: ${e.message}"), true)
        }
        if (lista.isEmpty()) {
            return Triple(
                deFabrica(),
                listOf("la lista de perfiles estaba vacía: se usan los tres de fábrica"),
                true,
            )
        }

        val salida = ArrayList<PerfilGuardado>()
        lista.forEachIndexed { i, item ->
            val m = item as? Map<String, Any?>
            if (m == null) {
                avisos += "el perfil ${i + 1} no es un objeto: se salta"
                return@forEachIndexed
            }
            val def = PerfilDiagnostico()
            val nombre = MiniJson.texto(m, "nombre")?.takeIf { it.isNotBlank() }
                ?: ("Perfil ${i + 1}".also { avisos += "el perfil ${i + 1} no tenía nombre: se llamó así" })
            val pruebas = (m["pruebas"] as? List<*>)?.mapNotNull { it as? String }?.toSet()
            var p = PerfilDiagnostico(
                nombre = nombre,
                profundo = MiniJson.booleano(m, "profundo") ?: def.profundo,
                pruebas = pruebas?.ifEmpty { def.pruebas } ?: def.pruebas,
                msCpu = MiniJson.entero(m, "msCpu") ?: def.msCpu,
                umbralMemoriaPct = MiniJson.entero(m, "umbralMemoriaPct") ?: def.umbralMemoriaPct,
                umbralBateriaC = MiniJson.numero(m, "umbralBateriaC") ?: def.umbralBateriaC,
                alCerrar = MiniJson.booleano(m, "alCerrar") ?: def.alCerrar,
                conSonido = MiniJson.booleano(m, "conSonido") ?: def.conSonido,
            )
            val (valido, corregido) = ReglasPerfilDiagnostico.validar(p)
            corregido.forEach { avisos += "$nombre: $it" }
            p = valido
            salida += PerfilGuardado(nombre, p, activo = MiniJson.booleano(m, "activo") ?: false)
        }
        if (salida.isEmpty()) {
            return Triple(
                deFabrica(),
                avisos + "ningún perfil era válido: se usan los de fábrica",
                true,
            )
        }
        if (salida.size > MAX_PERFILES) {
            avisos += "había ${salida.size} perfiles y el máximo es $MAX_PERFILES: se quedan los primeros"
        }
        // Nombres repetidos: se numeran, porque dos perfiles con el mismo nombre no se distinguen.
        val vistos = mutableListOf<String>()
        val conNombres = salida.take(MAX_PERFILES).map { p ->
            val n = nombreLimpio(p.nombre, vistos)
            vistos += n
            if (n != p.nombre) { avisos += "«${p.nombre}» estaba repetido: se llamó «$n»"; p.copy(nombre = n) } else p
        }
        return Triple(normalizarActivo(conNombres), avisos, false)
    }

    /** El JSON de un solo perfil, para exportarlo o pegarlo en un chat. */
    fun unSolo(p: PerfilGuardado): String = codificar(listOf(p.copy(activo = true)))
}

/**
 * Dónde viven los perfiles: `filesDir/perfiles-diagnostico.json`.
 *
 * La migración desde el fichero viejo se hace aquí y sólo una vez: si no hay JSON pero sí
 * `ajustes-diagnostico.txt`, se convierte en el perfil «Equilibrado» conservando lo que el
 * usuario hubiera tocado. Sin esto, actualizar la app le borraría su configuración.
 */
class PerfilesDiagnosticoJson(context: Context) {

    private val archivo = File(context.filesDir, "perfiles-diagnostico.json")
    private val viejo = File(context.filesDir, "ajustes-diagnostico.txt")
    private val roto = File(context.filesDir, "perfiles-diagnostico.roto.json")

    data class Carga(val lista: List<PerfilGuardado>, val avisos: List<String>)

    fun leer(): Carga {
        if (!archivo.exists() && viejo.exists()) {
            val migrado = runCatching { ReglasPerfilDiagnostico.decodificar(viejo.readText()) }
                .getOrDefault(PerfilDiagnostico())
            val lista = ReglasPerfilesJson.deFabrica().map { p ->
                if (p.activo) PerfilGuardado("Migrado", migrado.copy(nombre = "Migrado"), activo = true) else p
            }
            val (validos, avisos, _) = ReglasPerfilesJson.decodificar(ReglasPerfilesJson.codificar(lista))
            guardar(validos)
            return Carga(validos, listOf("se importó el perfil de la versión anterior") + avisos)
        }
        if (!archivo.exists()) {
            val fabrica = ReglasPerfilesJson.deFabrica()
            guardar(fabrica)
            return Carga(fabrica, emptyList())
        }
        val texto = runCatching { archivo.readText() }.getOrNull()
            ?: return Carga(ReglasPerfilesJson.deFabrica(), listOf("no se pudo leer el fichero de perfiles"))
        val (lista, avisos, invalido) = ReglasPerfilesJson.decodificar(texto)
        if (invalido) {
            // El fichero roto se aparta, no se borra: puede tener algo que el usuario escribió.
            runCatching { archivo.copyTo(roto, overwrite = true); archivo.delete() }
            guardar(lista)
        }
        return Carga(lista, avisos)
    }

    fun guardar(lista: List<PerfilGuardado>) {
        runCatching { archivo.writeText(ReglasPerfilesJson.codificar(lista)) }
    }

    fun ruta(): File = archivo

    /** Borra el JSON y vuelve a fábrica. El perfil viejo (`clave=valor`) no se toca. */
    fun restablecer(): List<PerfilGuardado> {
        val fabrica = ReglasPerfilesJson.deFabrica()
        guardar(fabrica)
        return fabrica
    }
}

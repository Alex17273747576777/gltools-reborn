"""Lee GLTOOLS JSON o cuatro Secrets individuales. Nunca registra valores ni genera claves."""
import base64
import json
import os
from pathlib import Path
import sys
import uuid

CAMPOS = ("REBORN_KEYSTORE_BASE64", "REBORN_STORE_PASS", "REBORN_KEY_ALIAS", "REBORN_KEY_PASS")


def sin_duplicados(pares):
    salida = {}
    for clave, valor in pares:
        if clave in salida:
            raise ValueError("Campos duplicados")
        salida[clave] = valor
    return salida


def leer_firma(entorno):
    paquete = entorno.get("GLTOOLS", "")
    if paquete:
        datos = json.loads(paquete, object_pairs_hook=sin_duplicados)
        if not isinstance(datos, dict):
            raise ValueError("Se requiere objeto JSON")
    else:
        datos = {k: entorno.get(k, "") for k in CAMPOS}
        if not any(datos.values()):
            return None
    if not all(isinstance(datos.get(k), str) and datos[k] for k in CAMPOS):
        raise ValueError("Campos requeridos incompletos")
    if any("\x00" in datos[k] for k in CAMPOS):
        raise ValueError("Caracteres no válidos")
    contenido = base64.b64decode("".join(datos[CAMPOS[0]].split()), validate=True)
    if not contenido or len(contenido) > 4 * 1024 * 1024:
        raise ValueError("Tamaño de keystore no válido")
    return contenido, {k: datos[k] for k in CAMPOS[1:]}



def diagnosticar_formato(entorno):
    """Sólo etiquetas prefijadas de un conjunto fijo: nunca imprime valores del Secret."""
    raw = entorno.get("GLTOOLS", "")
    if not raw:
        return ["FORMATO_GLTOOLS=AUSENTE"]
    try:
        datos = json.loads(raw, object_pairs_hook=sin_duplicados)
    except Exception:
        return ["FORMATO_JSON=INVALIDO"]
    if not isinstance(datos, dict):
        return ["FORMATO_JSON=NO_ES_OBJETO"]
    resultado = ["FORMATO_JSON=OBJETO_VALIDO"]
    for clave in CAMPOS:
        valor = datos.get(clave)
        estado = "AUSENTE" if clave not in datos else "TIPO_INVALIDO" if not isinstance(valor, str) else "VACIO" if not valor else "CADENA_PRESENTE"
        resultado.append("CAMPO_" + clave + "=" + estado)
    valor = datos.get(CAMPOS[0])
    if isinstance(valor, str) and valor:
        try:
            contenido = base64.b64decode("".join(valor.split()), validate=True)
            estado = "VACIO" if not contenido else "DECODIFICABLE"
        except Exception:
            estado = "INVALIDO"
        resultado.append("FORMATO_KEYSTORE_BASE64=" + estado)
    return resultado


def ocultar(valor):
    # Escape del protocolo de comandos de Actions, incluidos secretos multilínea.
    valor = valor.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    print("::add-mask::" + valor, flush=True)


def exportar(nombre, valor):
    delimitador = "REBORN_" + uuid.uuid4().hex
    while delimitador in valor.splitlines():
        delimitador = "REBORN_" + uuid.uuid4().hex
    with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as f:
        f.write(f"{nombre}<<{delimitador}\n{valor}\n{delimitador}\n")


def main():
    try:
        firma = leer_firma(os.environ)
    except Exception:
        for etiqueta in diagnosticar_formato(os.environ):
            print(etiqueta, flush=True)
        print("::error::GLTOOLS debe ser un JSON válido con los cuatro campos REBORN indicados y keystore base64 válido. No se muestran valores.")
        return 1
    if firma is None:
        if os.environ.get("PUBLICAR", "").lower() == "true":
            print("::error::Falta la firma original; publicación bloqueada.")
            return 1
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as f:
            f.write("firmado=false\nartefacto=NO-INSTALABLE-1.12.1-SIN-FIRMA\n")
        print("::warning::Sólo empaquetado sin firma: NO INSTALABLE.")
        return 0
    contenido, parametros = firma
    # Los valores extraídos de JSON no están automáticamente enmascarados por GitHub.
    for valor in parametros.values():
        ocultar(valor)
    try:
        os.umask(0o077)
        ruta = Path(os.environ["RUNNER_TEMP"]) / "reborn.jks"
        with open(ruta, "wb") as f:
            f.write(contenido)
        ruta.chmod(0o600)
        exportar("REBORN_KEYSTORE", str(ruta))
        for nombre, valor in parametros.items():
            exportar(nombre, valor)
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as f:
            f.write("firmado=true\nartefacto=version-1.12.1-firma-verificada\n")
    except Exception:
        print("::error::No se pudo preparar la firma en el runner; no se publica.")
        return 1
    print("Parámetros preparados sin mostrar valores. La coincidencia del certificado se verificará tras firmar ambos APK.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

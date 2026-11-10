"""Lee la versión de build.gradle una sola vez. Nadie escribe números a mano."""
import os
import re
import sys
from pathlib import Path


def leer(ruta="app/build.gradle"):
    """(nombre, codigo) tal como están escritos. Falla si no los encuentra."""
    texto = Path(ruta).read_text(encoding="utf-8")
    nombre = re.search(r'versionName\s+"([^"]+)"', texto)
    codigo = re.search(r"versionCode\s+(\d+)", texto)
    if not nombre or not codigo:
        raise SystemExit("build.gradle no declara versionName/versionCode")
    return nombre.group(1), int(codigo.group(1))


if __name__ == "__main__":
    nombre, codigo = leer()
    salida = os.environ.get("GITHUB_OUTPUT")
    if salida:
        with open(salida, "a", encoding="utf-8") as f:
            f.write(f"nombre={nombre}\ncodigo={codigo}\n")
    print(f"VERSION_ACTUAL={nombre} CODIGO_ACTUAL={codigo}")

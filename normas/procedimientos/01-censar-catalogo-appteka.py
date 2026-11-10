#!/usr/bin/env python3
"""
Censar el catálogo de Appteka, categoría por categoría, con el filtro de código abierto.

Por qué existe: la primera vez que se miró el sitio se leyó el HTML, que solo trae la primera
página y solo lo más reciente. Eso da una foto falsa. El sitio tiene una API interna que usa su
propio explorador —`/api/proxy/list`— y con ella se puede recorrer de verdad.

Uso:
    python3 01-censar-catalogo-appteka.py            # censo completo → censo.json
    python3 01-censar-catalogo-appteka.py --todas    # sin el filtro de código abierto

Lo que produce: `censo.json` con, por categoría, cuántas apps hay, cuántas son de código
abierto y cuáles son, con su repositorio de origen. Y `resumen.md` para leer en dos minutos.
"""
import json, sys, time, urllib.request, collections

API = "https://appteka.store/api/proxy/list"
CABECERAS = {"User-Agent": "Mozilla/5.0 (censo; 1 petición cada 300 ms)"}
PAUSA = 0.3          # cortés: no se bombardea el servidor de nadie
PAGINA = 96          # lo que devuelve la API por página


def pedir(params: dict) -> dict:
    """Una llamada a la API, con reintentos. Devuelve el bloque `result`."""
    from urllib.parse import urlencode
    url = API + ("?" + urlencode(params) if params else "")
    for intento in range(4):
        try:
            req = urllib.request.Request(url, headers=CABECERAS)
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)["result"]
        except Exception:
            time.sleep(1.5 * (intento + 1))
    return {"entries": [], "total": 0}


def categorias() -> dict:
    """El mapa de categorías. La API no lo sirve: se sondea por id (1..50)."""
    halladas, pendientes = {}, list(range(1, 51))
    # primero, lo que aparece en el listado general (es más rápido)
    aid = None
    for _ in range(6):
        d = pedir({"open_source": 1, **({"app_id": aid} if aid else {})})
        e = d.get("entries") or []
        if not e:
            break
        for x in e:
            c = x["category"]
            halladas[c["id"]] = c["name"]["en"]
        aid = e[-1]["app_id"]
        if len(e) < PAGINA:
            break
        time.sleep(PAUSA)
    # después, sondear las que falten
    for cid in pendientes:
        if cid in halladas:
            continue
        d = pedir({"category_id": cid, "open_source": 1})
        e = d.get("entries") or []
        if e and e[0]["category"]["id"] == cid:
            halladas[cid] = e[0]["category"]["name"]["en"]
        time.sleep(PAUSA)
    return halladas


def recorrer(cid: int, solo_abierto: bool) -> dict:
    """Recorre una categoría entera, página a página, hasta que se acaba."""
    vistos, aid = {}, None
    while True:
        params = {"category_id": cid}
        if solo_abierto:
            params["open_source"] = 1
        if aid:
            params["app_id"] = aid
        d = pedir(params)
        e = d.get("entries") or []
        if not e:
            break
        for x in e:
            if x["category"]["id"] == cid:
                vistos[x["app_id"]] = {
                    "nombre": x["label"], "paquete": x["package"],
                    "version": x["ver_name"], "tamano": x["size"],
                    "descargas": x.get("downloads", 0),
                    "fuente": x.get("source_url") or "",
                    "autor": (x.get("author") or {}).get("name", ""),
                }
        aid = e[-1]["app_id"]
        if len(e) < PAGINA:
            break
        time.sleep(PAUSA)
    return vistos


def main() -> None:
    solo_abierto = "--todas" not in sys.argv
    cats = categorias()
    print(f"{len(cats)} categorías encontradas")
    censo = {}
    for cid, nombre in sorted(cats.items()):
        apps = recorrer(cid, solo_abierto)
        censo[nombre] = {"id": cid, "total": len(apps), "apps": apps}
        print(f"  {nombre:28} {len(apps):5}")
    json.dump(censo, open("censo.json", "w"), ensure_ascii=False, indent=1)

    total = sum(c["total"] for c in censo.values())
    con_repo = sum(1 for c in censo.values() for a in c["apps"].values() if a["fuente"])
    lineas = [
        "# Censo de Appteka" + (" · sólo código abierto" if solo_abierto else ""), "",
        f"- Categorías: **{len(censo)}**",
        f"- Apps: **{total}**",
        f"- Con repositorio de origen declarado: **{con_repo}**",
        "", "| Categoría | Apps |", "|---|---|",
    ]
    lineas += [f"| {n} | {c['total']} |" for n, c in sorted(censo.items(), key=lambda kv: -kv[1]["total"])]
    open("resumen.md", "w").write("\n".join(lineas) + "\n")
    print(f"\nTOTAL {total} apps · {con_repo} con repositorio · escrito censo.json y resumen.md")


if __name__ == "__main__":
    main()

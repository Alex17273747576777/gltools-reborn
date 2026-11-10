import base64
import json
import unittest
from preparar_firma import leer_firma, CAMPOS, diagnosticar_formato


class FirmaJSON(unittest.TestCase):
    def datos(self):
        return dict(zip(CAMPOS, (base64.b64encode(b"fixture-not-a-real-key").decode(), "store-fixture", "alias-fixture", "key-fixture")))

    def test_json_valido(self):
        key, datos = leer_firma({"GLTOOLS": json.dumps(self.datos())})
        self.assertEqual(key, b"fixture-not-a-real-key")
        self.assertEqual(datos[CAMPOS[2]], "alias-fixture")

    def test_diagnostico_no_revela_valores(self):
        d = self.datos(); d[CAMPOS[0]] = "fixture-invalida-no-publicar"
        etiquetas = diagnosticar_formato({"GLTOOLS": json.dumps(d)})
        self.assertIn("FORMATO_KEYSTORE_BASE64=INVALIDO", etiquetas)
        for valor in d.values():
            self.assertNotIn(valor, "\n".join(etiquetas))

    def test_diagnostico_sintaxis(self):
        self.assertEqual(diagnosticar_formato({"GLTOOLS": "not-json"}), ["FORMATO_JSON=INVALIDO"])

    def test_diagnostico_campo_ausente(self):
        d = self.datos(); del d[CAMPOS[3]]
        self.assertIn("CAMPO_REBORN_KEY_PASS=AUSENTE", diagnosticar_formato({"GLTOOLS": json.dumps(d)}))

    def test_individuales(self):
        self.assertIsNotNone(leer_firma(self.datos()))

    def test_ausencia(self):
        self.assertIsNone(leer_firma({}))

    def test_json_invalido(self):
        with self.assertRaises(ValueError):
            leer_firma({"GLTOOLS": "not-json"})

    def test_lista_no_objeto(self):
        with self.assertRaises(ValueError):
            leer_firma({"GLTOOLS": "[]"})

    def test_campo_ausente(self):
        d = self.datos(); del d[CAMPOS[3]]
        with self.assertRaises(ValueError):
            leer_firma({"GLTOOLS": json.dumps(d)})

    def test_base64_invalido(self):
        d = self.datos(); d[CAMPOS[0]] = "not base64 !"
        with self.assertRaises(ValueError):
            leer_firma({"GLTOOLS": json.dumps(d)})

    def test_tipo_no_string(self):
        d = self.datos(); d[CAMPOS[1]] = 123
        with self.assertRaises(ValueError):
            leer_firma({"GLTOOLS": json.dumps(d)})

    def test_multilinea(self):
        d = self.datos(); d[CAMPOS[1]] = "linea1\nlinea2"
        self.assertEqual(leer_firma({"GLTOOLS": json.dumps(d)})[1][CAMPOS[1]], "linea1\nlinea2")

    def test_duplicados(self):
        with self.assertRaises(ValueError):
            leer_firma({"GLTOOLS": '{"REBORN_KEY_PASS":"a","REBORN_KEY_PASS":"b"}'})

    def test_no_fallback_silencioso(self):
        d = self.datos(); d["GLTOOLS"] = "not-json"
        with self.assertRaises(ValueError):
            leer_firma(d)

    def test_parcial_individual(self):
        with self.assertRaises(ValueError):
            leer_firma({"REBORN_KEY_PASS": "fixture"})


if __name__ == "__main__":
    unittest.main()

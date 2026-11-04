from pathlib import Path
import zipfile

raiz = Path(__file__).resolve().parents[1]
destino = (raiz / "build/validacao-api-email").resolve()
destino.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(raiz / "build/entregas/renovai-api-email-atualizado.zip") as z:
    prefixo = next(n.removesuffix("pom.xml") for n in z.namelist() if n.endswith("/pom.xml") and "target/" not in n)
    for item in z.infolist():
        if item.is_dir() or not item.filename.startswith(prefixo):
            continue
        relativo = item.filename[len(prefixo):]
        alvo = (destino / relativo).resolve()
        if not alvo.is_relative_to(destino):
            raise ValueError("Caminho fora da pasta de validação")
        alvo.parent.mkdir(parents=True, exist_ok=True)
        alvo.write_bytes(z.read(item))
print(destino)

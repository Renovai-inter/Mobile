"""Gera as substituições da API e do Apps Script a partir da referência aprovada.

Não altera o ZIP original, não acessa credenciais e não envia e-mails.
"""
from pathlib import Path
import base64
import re
import sys
import zipfile

raiz = Path(__file__).resolve().parents[1]
arquivo_api = Path(sys.argv[1])
referencia = Path(sys.argv[2]).read_text(encoding="utf-8")
destino = raiz / "integracoes" / "email-api"
with zipfile.ZipFile(arquivo_api) as pacote:
    prefixo = next(n.removesuffix("src/main/java/com/renovai/api/service/EmailService.java")
                    for n in pacote.namelist() if n.endswith("src/main/java/com/renovai/api/service/EmailService.java"))
    service_path = "src/main/java/com/renovai/api/service/EmailService.java"
    service = pacote.read(prefixo + service_path).decode("utf-8").replace("\r\n", "\n")
    doc = pacote.read(prefixo + "docs/email-apps-script.md").decode("utf-8").replace("\r\n", "\n")

mascote = re.search(r'data:image/png;base64,([A-Za-z0-9+/=]+)', referencia).group(1)
imagem = destino / "src/main/resources/templates/email/assets/nimo.png"
imagem.parent.mkdir(parents=True, exist_ok=True)
imagem.write_bytes(base64.b64decode(mascote))
html = re.sub(r'data:image/png;base64,[A-Za-z0-9+/=]+', "cid:nimo", referencia)
html = html.replace("Confirme seu e-mail", "Redefina sua senha")
html = html.replace("Oi! Estamos quase lá. Use o código abaixo para confirmar seu endereço de e-mail e continuar sua jornada com o Renovai.",
                    "Recebemos um pedido para redefinir sua senha no Renovai. Digite o código abaixo no aplicativo para continuar.")
html = html.replace("SEU CÓDIGO DE VERIFICAÇÃO", "SEU CÓDIGO DE RECUPERAÇÃO")
html = html.replace("482 916", "{{CODIGO}}")
html = html.replace("10 minutos", "{{VALIDADE}} minutos")
html = html.replace("Se você não solicitou esta confirmação, pode ignorar este e-mail.",
                    "Se você não solicitou a troca de senha, pode ignorar este e-mail. Sua senha continua a mesma.")
html = html.replace("letter-spacing:10px", "letter-spacing:8px")
html = html.replace("© Renovai · Transformando resíduos em novas possibilidades.</div>",
                    '© Renovai · Transformando resíduos em novas possibilidades.<br>'
                    '<a href="mailto:renovai.suporte@gmail.com" style="color:#882B4E;">Fale com o suporte</a></div>')
# Células, cores e largura têm fallback inline em clientes que ignoram o <style>.
html = html.replace('<body>', '<body style="margin:0;padding:0;width:100%;background:#F3F3EF;font-family:Arial,Helvetica,sans-serif;color:#292929;">')
html = html.replace('role="presentation"', 'role="presentation" cellspacing="0" cellpadding="0" border="0"')
html = html.replace('table{border-collapse:collapse}', 'table{border-collapse:separate;border-spacing:0}')
template_path = "src/main/resources/templates/email/codigo-redefinicao.html"
(destino / template_path).write_text(html, encoding="utf-8")
fixtures = raiz / "app/src/androidTest/assets/email"
fixtures.mkdir(parents=True, exist_ok=True)
(fixtures / "codigo-redefinicao.html").write_text(html, encoding="utf-8")
(fixtures / "nimo.png").write_bytes(imagem.read_bytes())
service = service.replace('helper.setText(textoPuro, html);',
                         'helper.setText(textoPuro, html);\n'
                         '            helper.addInline("nimo", new ClassPathResource("templates/email/assets/nimo.png"), "image/png");')
source = destino / service_path
source.parent.mkdir(parents=True, exist_ok=True)
source.write_text(service, encoding="utf-8")
script = re.search(r"```javascript\s*([\s\S]*?)```", doc).group(1)
script = re.sub(r"const MODELO = `[\s\S]*?`;", lambda _: "const MODELO = `" + html + "`;", script)
script = script.replace("name: 'Renovaí'", "name: 'Renovaí', inlineImages: { nimo: nimo() }")
script += '\nfunction nimo() { return Utilities.newBlob(Utilities.base64Decode(NIMO_BASE64), "image/png", "nimo.png"); }\n'
script += 'const NIMO_BASE64 = "' + mascote + '";\n'
(destino / "apps-script").mkdir(exist_ok=True)
(destino / "apps-script/Code.gs").write_text(script, encoding="utf-8")
(destino / "docs").mkdir(exist_ok=True)
(destino / "docs/email-apps-script.md").write_text(
    "# Envio de e-mail\n\nO código atualizado do Apps Script está em `../apps-script/Code.gs`. "
    "O corpo recebido continua `{ para, codigo, validade }`, com as mesmas validações e limites. "
    "O HTML e o mascote são locais ao script; não se aceita HTML de fora.\n\n"
    "Copie o script para a implantação existente e publique uma nova versão mantendo a URL. "
    "No SMTP, copie os arquivos `src/` para a API. O mascote usa CID em ambos os provedores.\n", encoding="utf-8")
saida = raiz / "build/entregas/renovai-api-email-atualizado.zip"
saida.parent.mkdir(parents=True, exist_ok=True)
substituicoes = {service_path: service.encode("utf-8"), template_path: html.encode("utf-8"),
                 "src/main/resources/templates/email/assets/nimo.png": imagem.read_bytes(),
                 "apps-script/Code.gs": script.encode("utf-8"),
                 "docs/email-apps-script.md": (destino / "docs/email-apps-script.md").read_bytes()}
for teste in (destino / "src/test").rglob("*.java"):
    substituicoes[teste.relative_to(destino).as_posix()] = teste.read_bytes()
with zipfile.ZipFile(arquivo_api) as original, zipfile.ZipFile(saida, "w", zipfile.ZIP_DEFLATED) as novo:
    for item in original.infolist():
        # Entrega de código-fonte: sem .git, artefatos compilados ou credenciais locais.
        relativo = item.filename.removeprefix(prefixo)
        if not item.filename.startswith(prefixo) or relativo.startswith((".git/", "target/")):
            continue
        if Path(relativo).name in {".env", "local.properties"}:
            continue
        if relativo not in substituicoes:
            novo.writestr(item, original.read(item))
    for nome, conteudo in substituicoes.items():
        novo.writestr(prefixo + nome, conteudo)
print(f"Arquivos de integração: {destino}")
print(f"API atualizada: {saida}")

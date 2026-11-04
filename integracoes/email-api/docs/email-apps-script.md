# Envio de e-mail

O código atualizado do Apps Script está em `../apps-script/Code.gs`. O corpo recebido continua `{ para, codigo, validade }`, com as mesmas validações e limites. O HTML e o mascote são locais ao script; não se aceita HTML de fora.

Copie o script para a implantação existente e publique uma nova versão mantendo a URL. No SMTP, copie os arquivos `src/` para a API. O mascote usa CID em ambos os provedores.

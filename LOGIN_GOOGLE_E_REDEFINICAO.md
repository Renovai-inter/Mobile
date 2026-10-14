# Login com Google e redefinição de senha por código

Depende da API (Spring-API) com os endpoints `/auth/google`, `/auth/esqueci-senha`,
`/auth/verificar-codigo` e `/auth/redefinir-senha` (branch `feat/login-google-codigo-email`).

## Login com Google

1. Na tela de login, **Entrar com Google** abre a lista de contas Google do aparelho (Credential Manager).
2. O app recebe um *ID token* do Google e manda para `POST /auth/google`.
3. A API confere o token no Google e, se o e-mail tiver conta no Renovaí, devolve o mesmo JWT do
   login por senha. O app segue o mesmo caminho de sempre (Empresa, Gestor, Cooperado, Motorista).
4. O login com Google **não cria conta**: quem ainda não tem cadastro recebe a mensagem para se cadastrar.

### Configuração (Firebase `renovai-23544`)

- Provedor **Google** ativado em Authentication → Método de login.
- `app/google-services.json` já traz o "ID do cliente da Web" (vira `default_web_client_id`).
- **Cada computador que compila o app precisa ter o SHA-1 do seu certificado de debug cadastrado**
  em Configurações do projeto → Seus apps → Renovai Android → Adicionar impressão digital.
  Para ver o SHA-1: `./gradlew signingReport` (linha `SHA1` da variante `debug`).
  Sem isso o Google responde "Developer console is not set up correctly".
- Para o APK de release, cadastre também o SHA-1 da chave de release.

## Redefinição de senha em duas etapas

1. **Esqueci minha senha**: a pessoa informa o e-mail (1º fator). A API envia um **código de 6 dígitos**
   num e-mail com a identidade do Renovaí (2º fator). A resposta é a mesma mesmo se o e-mail não
   tiver conta, para não revelar quem está cadastrado.
2. **Redefinir senha**: a pessoa digita o código (`POST /auth/verificar-codigo`); só então aparecem
   os campos da nova senha (`POST /auth/redefinir-senha` com `email`, `token` = código e `novaSenha`).
3. O código vale **15 minutos**, serve uma vez só e é invalidado após **5 tentativas erradas**.
   Dá para pedir outro código pela própria tela ("Reenviar código", 1 por minuto).

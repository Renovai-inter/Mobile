# Login com Google e redefinição de senha por código

- **Login com Google:** feito todo no app. A API **não muda** (usa o `POST /auth/login` de sempre).
- **Redefinição de senha:** depende da API com `/auth/esqueci-senha` enviando o código por e-mail,
  `/auth/verificar-codigo` e `/auth/redefinir-senha` (branch `feat/login-google-codigo-email` da Spring-API).

## Login com Google

1. Na tela de login, **Entrar com Google** abre a lista de contas Google do aparelho
   (Credential Manager, classe `GoogleLogin`).
2. O app recebe um *ID token* do Google e o próprio app o confere (`controller/GoogleTokenValidator`):
   assinatura RS256 com as chaves públicas do Google, `iss` = accounts.google.com,
   `aud` = ID do cliente da Web do Firebase, `exp` não vencido e `email_verified` = true.
3. Com o e-mail confirmado, o app busca a senha desse e-mail guardada **neste aparelho**
   (`CofreCredenciais`: AES-256/GCM com a chave no Android Keystore) e faz o login normal
   `POST /auth/login`. Daí em diante o caminho é o de sempre (Empresa, Gestor, Cooperado, Motorista).
4. A senha é guardada no **primeiro login com e-mail e senha** no aparelho. Antes disso o botão do
   Google avisa: "entre uma vez com e-mail e senha".
5. Se a senha mudar em outro lugar, o login pelo Google falha uma vez, a senha guardada é apagada e
   o app pede para entrar com a senha nova. Ao redefinir a senha pelo app, a senha guardada é atualizada.
6. O login com Google **não cria conta**.

> Limite: o Google só serve para destravar a senha guardada no aparelho. Quem garante o acesso na
> API continua sendo a senha. Para a API aceitar o Google sem senha, seria preciso um endpoint novo
> na API (ex.: `/auth/google`).

### Configuração (Firebase `renovai-23544`)

- Provedor **Google** ativado em Authentication → Método de login.
- `app/google-services.json` já traz o "ID do cliente da Web" (vira `default_web_client_id`).
- **Cada computador que compila o app precisa ter o SHA-1 do seu certificado de debug cadastrado**
  em Configurações do projeto → Seus apps → Renovai Android → Adicionar impressão digital.
  Para ver o SHA-1: `./gradlew signingReport` (linha `SHA1` da variante `debug`).
  Sem isso o Google responde "Developer console is not set up correctly".
- Para o APK de release, cadastre também o SHA-1 da chave de release.

## Redefinição de senha com verificação em dois fatores

As telas mostram um indicador de 3 etapas: **1 E-mail → 2 Código → 3 Nova senha**
(`include_etapas_redefinicao.xml` + `EtapasRedefinicao.java`).

1. **Esqueci minha senha**: a pessoa informa o e-mail (1º fator). A API envia um **código de 6 dígitos**
   num e-mail com a identidade do Renovaí, enviado por **renovai.suporte@gmail.com** (2º fator). A resposta é a mesma mesmo se o e-mail não
   tiver conta, para não revelar quem está cadastrado.
2. **Redefinir senha**: a pessoa digita o código (`POST /auth/verificar-codigo`); só então aparecem
   os campos da nova senha (`POST /auth/redefinir-senha` com `email`, `token` = código e `novaSenha`).
3. O código vale **15 minutos**, serve uma vez só e é invalidado após **5 tentativas erradas**.
   Dá para pedir outro código pela própria tela ("Reenviar código", 1 por minuto).

### Por que o código é enviado pela API e não pelo Firebase

As contas do Renovaí ficam no Postgres da API, não no Firebase Auth. O "esqueci a senha" do
Firebase só funciona para contas criadas no próprio Firebase Auth (e manda um link, não um código),
e enviar e-mail personalizado pelo Firebase (extensão Trigger Email / Cloud Functions) exige o
plano Blaze. Por isso o 2º fator é o código gerado e conferido pela API.

### Configuração do envio (servidor: Render e EKS)

1. Na conta **renovai.suporte@gmail.com**: ative a Verificação em duas etapas e crie uma
   **Senha de app** (Conta Google → Segurança → Senhas de app).
2. No servidor, defina:
   - `MAIL_USERNAME=renovai.suporte@gmail.com` (já é o padrão se não for definido)
   - `MAIL_PASSWORD=<senha de app de 16 letras>`

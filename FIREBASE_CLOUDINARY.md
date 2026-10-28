# Fotos (Cloudinary), cache offline (Firestore) e relatórios (Firebase Storage)

A API (Spring + Postgres) continua sendo a fonte da verdade. O app usa:

| O quê | Onde | Classe |
|---|---|---|
| Foto da coleta | Cloudinary (upload unsigned, pasta `Renovai/coletas`) → URL vai no `imagemUrl` do `POST /coletas` | `CloudinaryUpload`, `AdicionarColetaActivity` |
| Fotos nas listas e detalhes (coletas, triagens, cooperativas) | Glide com cache em memória/disco; sem URL na listagem, busca `GET /coletas/{id}` | `Imagens`, `FotoColetaRepository`, `TipoImagem` |
| Cópia da conta logada (role, usuarioId, empresa/cooperativa, cargo, nome; sem token nem senha) | Firestore `usuarios/{uid}/contas/{email}` | `FirebaseCache.salvarConta`, `AuthController` |
| Cache offline das listagens (Gestor, Empresa, Cooperado) | Firestore `usuarios/{uid}/contas/{email}/cache/{chave}` | `FirebaseCache`, `GestorCache`, `CooperadoPreload` |
| Descrição e data prevista da triagem da coleta (o banco da API não tem esses campos) | Firestore `cooperativas/{cooperativaId}/coletas/{coletaId}` | `ColetaDetalhes`, `AdicionarColetaActivity` |
| Relatórios PDF | Storage `relatorios/{cooperativaId}/{id}.pdf` + lista no Firestore `cooperativas/{cooperativaId}/relatorios/{id}` | `GestorRelatorios`, `GestorRelatoriosActivity` |

## Configuração (uma vez)

### 1. Cloudinary
Em `local.properties` (na raiz do projeto) (não vai para o Git), preencha:
```
cloudinary.cloudName=SEU_CLOUD_NAME
cloudinary.uploadPreset=SEU_UPLOAD_PRESET
```
O preset precisa ser **Unsigned**. Recomendado restringir formatos (jpg, png, webp), tamanho máximo e a pasta `renovai/`.
Enquanto estiver com `SEU_...`, o botão de anexar foto fica desativado ("Envio de foto não configurado").

### 2. Firebase
1. Console do Firebase → crie o projeto → **Adicionar app Android** com o pacote `com.example.renovai`.
2. Baixe o `google-services.json` e coloque em `app/google-services.json`.
   Sem esse arquivo o app compila e roda normalmente, mas com o Firebase desligado
   (sem cache offline e sem relatórios na nuvem). É assim que o CI compila.
3. **Authentication → Sign-in method → Anônimo → Ativar.**
4. **Firestore Database → Criar banco** e cole as regras de `firebase/firestore.rules`.
5. **Storage → Começar** e cole as regras de `firebase/storage.rules`.
   Atenção: o Google pode exigir o plano Blaze para usar o Storage em projetos novos.
6. Gradle Sync no Android Studio.

## Como funciona

- **Cache offline:** cada listagem recebida da API é gravada no Firestore. Ao abrir o app (mesmo sem internet), a cópia é lida do disco do aparelho e mostrada na hora; a API atualiza em seguida. O cache é separado por conta (e-mail do login).
- **Cópia da conta:** a cada login (e no cadastro de empresa) os dados da conta são gravados no Firestore. Se no login a API não devolver o funcionário/cooperativa, o app usa a cópia do último login dessa mesma conta (mesmo `usuarioId`). A conta de verdade e a senha continuam só na API.
- **Fotos:** ao registrar a coleta, a foto sobe primeiro para o Cloudinary; só então a coleta é enviada à API com a URL. Sem internet, o envio é cancelado e a pessoa tenta de novo.
- **Relatórios:** o PDF é salvo no aparelho e enviado ao Storage em segundo plano. A tela "Seus Relatórios" sincroniza com o Firestore: mostra relatórios criados em outro aparelho (baixados ao abrir) e reenvia os que foram criados sem internet. Excluir remove do aparelho, do Storage e do Firestore.

## Segurança (limite conhecido)
O login do app é o JWT da API, que o Firebase não conhece. Por isso o Firebase usa login **anônimo**: as regras impedem acesso de fora do app e isolam o cache de cada instalação, mas não separam uma cooperativa da outra nos relatórios. Para produção, o ideal seria a API emitir um *custom token* do Firebase.

## Login com Google

O botão "Entrar com Google" (tela de login) usa o Credential Manager do Android: o Google mostra as
contas do celular e devolve um ID token, que o app manda para `POST /auth/google`. A API confere o
token no Google e devolve o mesmo login do e-mail e senha. Ela não cria conta: o e-mail do Google
precisa já ter cadastro no Renovaí.

Para funcionar:

1. Firebase Console → Authentication → Sign-in method → ativar **Google**. Isso cria o
   "ID do cliente da Web" (Web client ID).
2. Firebase Console → Configurações do projeto → app Android → adicionar a impressão digital
   **SHA-1** da chave que assina o APK (debug: `./gradlew signingReport`).
3. No `local.properties` do app: `google.webClientId=XXXX.apps.googleusercontent.com`.
4. Na API: a rota `/auth/google` (branch `feat/login-google-codigo-email`) publicada, com
   `GOOGLE_CLIENT_ID` igual ao mesmo Web client ID.

Sem o `google.webClientId`, o botão avisa que o login com Google não está configurado.

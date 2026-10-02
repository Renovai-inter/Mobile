# Fotos (Cloudinary), cache offline (Firestore) e relatórios (Firebase Storage)

A API (Spring + Postgres) continua sendo a fonte da verdade. O app usa:

| O quê | Onde | Classe |
|---|---|---|
| Foto da coleta | Cloudinary → URL vai no `imagemUrl` do `POST /coletas` | `CloudinaryUploader`, `AdicionarColetaActivity` |
| Cache offline das listagens (Gestor, Empresa, Cooperado) | Firestore `usuarios/{uid}/contas/{email}/cache/{chave}` | `FirebaseCache`, `GestorCache`, `CooperadoPreload` |
| Relatórios PDF | Storage `relatorios/{cooperativaId}/{id}.pdf` + lista no Firestore `cooperativas/{cooperativaId}/relatorios/{id}` | `GestorRelatorios`, `GestorRelatoriosActivity` |

## Configuração (uma vez)

### 1. Cloudinary
Em `Mobile-main/local.properties` (não vai para o Git), preencha:
```
cloudinary.cloudName=SEU_CLOUD_NAME
cloudinary.uploadPreset=SEU_UPLOAD_PRESET
```
O preset precisa ser **Unsigned**. Recomendado restringir formatos (jpg, png, webp), tamanho máximo e a pasta `renovai/`.
Enquanto estiver com `SEU_...`, o botão de anexar foto fica desativado ("Envio de foto não configurado").

### 2. Firebase
1. Console do Firebase → crie o projeto → **Adicionar app Android** com o pacote `com.example.renovai`.
2. Baixe o `google-services.json` e coloque em `Mobile-main/app/google-services.json`.
   Sem esse arquivo o build falha (o plugin `google-services` exige).
3. **Authentication → Sign-in method → Anônimo → Ativar.**
4. **Firestore Database → Criar banco** e cole as regras de `firebase/firestore.rules`.
5. **Storage → Começar** e cole as regras de `firebase/storage.rules`.
   Atenção: o Google pode exigir o plano Blaze para usar o Storage em projetos novos.
6. Gradle Sync no Android Studio.

## Como funciona

- **Cache offline:** cada listagem recebida da API é gravada no Firestore. Ao abrir o app (mesmo sem internet), a cópia é lida do disco do aparelho e mostrada na hora; a API atualiza em seguida. O cache é separado por conta (e-mail do login).
- **Fotos:** ao registrar a coleta, a foto sobe primeiro para o Cloudinary; só então a coleta é enviada à API com a URL. Sem internet, o envio é cancelado e a pessoa tenta de novo.
- **Relatórios:** o PDF é salvo no aparelho e enviado ao Storage em segundo plano. A tela "Seus Relatórios" sincroniza com o Firestore: mostra relatórios criados em outro aparelho (baixados ao abrir) e reenvia os que foram criados sem internet. Excluir remove do aparelho, do Storage e do Firestore.

## Segurança (limite conhecido)
O login do app é o JWT da API, que o Firebase não conhece. Por isso o Firebase usa login **anônimo**: as regras impedem acesso de fora do app e isolam o cache de cada instalação, mas não separam uma cooperativa da outra nos relatórios. Para produção, o ideal seria a API emitir um *custom token* do Firebase.

# Área de Cooperado — Implementação

Este pacote implementa as 7 telas da seção **2. Cooperado**
(`Mapeamento_Telas_Renovai_2.0.txt`), integradas com a API real do backend
(`renovai-api-revisado`), seguindo o padrão de arquitetura já usado em
`Mobile-main` (Retrofit 2 + `ApiClient`/`SessionManager`/`AuthInterceptor` +
DTOs `dto/request`/`dto/response` + `controller`/`adapter`/`view`).

Nenhum dado é mockado: toda tela chama a API de verdade. Onde a API tem uma
limitação real (ver seção "Limitações conhecidas"), isso está documentado tanto
aqui quanto em comentário no código, no ponto exato onde a limitação aparece.

---

## 1. Telas entregues

| # | Tela | Activity | Endpoints reais usados |
|---|------|----------|------------------------|
| 2.1 | Home | `CooperadoHomeActivity` | `GET /coletas/por-cooperado/{id}`, `GET /triagens/por-cooperado/{id}` |
| 2.2 | Adicionar Coleta | `AdicionarColetaActivity` | `POST /coletas`, `GET /materiais` (categorias), `GET /rotas/ativas/por-cooperativa/{id}` |
| 2.3 | Completar Triagem | `CompletarTriagemActivity` | `GET /triagens/por-coleta/{id}`, `PUT /triagens/{id}`, `PATCH /triagens/{id}/concluir` |
| 2.4 | Perfil | `PerfilCooperadoActivity` | nenhum (lê cache resolvido no login — ver seção 3) |
| 2.5 | Rateios Recebidos | `RateiRecebidosActivity` | `GET /rateios/por-cooperativa/{id}` + `GET /rateios/{id}/distribuicao` |
| 2.6 | Listagem de Coletas | `ListagemColetasActivity` | `GET /coletas/por-cooperado/{id}` |
| 2.7 | Listagem de Triagens | `ListagemTriagensActivity` | `GET /triagens/por-cooperado/{id}` |

Cada Activity confere, no `onCreate`, se `CooperadoSession.temCooperadoResolvido()`
é verdadeiro; se não for, mostra um aviso e manda a pessoa de volta para o login
— isso não deveria acontecer no fluxo normal (ver seção 3), mas evita uma tela
quebrada num caso extremo.

### "Detalhes" não é uma 8ª Activity

O wireframe (`Cooperado.png`) tem uma tela "Detalhes" reaproveitada a partir dos
botões "Detalhes" das listagens de Coleta e Triagem. Como o formato de entrega
pedido lista exatamente 7 Activities (nenhuma "Detalhe*Activity"), essa tela
virou um `AlertDialog` (ver `DetalheDialogHelper.java`), aberto de dentro da
própria Activity que tem o botão — mostra os mesmos campos, sem estourar a
lista de entregáveis.

Os campos mostrados no diálogo de Coleta são só os que `ColetaResponse`
realmente tem (quantidade, data, feito por, tipo, rota, status) — os campos
extras do wireframe ("Materiais", "Origem", "Triada", "Descrição") não existem
em `ColetaResponse` (ver seção 2) e por isso não aparecem, em vez de mostrar
"XX kg" fixo como no mockup.

---

## 2. Limitações reais da API (não são bugs deste app)

Conferidas lendo o código-fonte do backend (`renovai-api-revisado`), não
supondo:

1. **`ColetaRequest` não tem campo para materiais, "necessita triagem?" nem
   "data prevista da triagem".** A tela 2.2 mostra esses campos (fiel ao
   wireframe) e deixa o cooperado preenchê-los, mas **eles não são enviados**
   — não há onde persisti-los no `Requests.ColetaRequest` atual. Só
   `quantidadeKg`, `tipoColeta`, `rotaId`, `imagemUrl` e `statusId` (opcional)
   são realmente gravados. A lista "Materiais coletados" na tela usa
   `GET /materiais` (endpoint que já existia) só para preencher o dropdown de
   categoria com dados reais da cooperativa — não é enviada com a coleta.
2. **Não existe endpoint de upload de imagem em nenhum lugar da API.** "Foto
   da coleta" e "Foto da coleta vinculada" (tela 2.3) só mostram uma
   pré-visualização local (a partir da galeria/câmera do aparelho) — a
   `imagemUrl` enviada em `POST /coletas` vai sempre `null`. Quando o backend
   tiver um jeito de subir imagem (ex.: URL pré-assinada), é só trocar o
   `null` pela URL retornada.
3. **Não existe `GET /rateios/por-cooperado/{id}`.** Só existe
   `GET /rateios/por-cooperativa/{id}` (pensado pra tela do Gestor, 4.7). A
   tela 2.5 busca todos os rateios da cooperativa do cooperado e, pra cada um,
   busca `GET /rateios/{id}/distribuicao`, filtrando pelo `funcionarioId` do
   cooperado logado (ver `RateiController.java`). Funciona, mas custa N+1
   chamadas — o mesmo padrão que `HomeFragment` (Empresa) já usa pra cruzar
   cooperativas favoritas.
4. **Rateio não tem um campo de status** (Fechado/Pendente/Agendada, como no
   wireframe). `RateioListaResponse` não expõe isso. O status mostrado na tela
   2.5 é uma heurística baseada em `dataRateio` comparada com a data atual
   (passado = Fechado, mês atual = Pendente, futuro = Agendada) — ver
   `RateiController.classificarStatus()`.
5. **Uma "Triagem" na API é UMA LINHA POR MATERIAL, não uma sessão de triagem
   inteira.** Uma coleta com N materiais gera N registros em `/triagens`,
   todos com o mesmo `coletaId`. Por isso:
   - A tela 2.3 recebe um `coletaId` (não um `triagemId`) e busca
     `GET /triagens/por-coleta/{coletaId}` pra montar a lista de materiais.
   - "Concluir" (`PATCH /triagens/{id}/concluir`) conclui UM material por vez,
     não a coleta inteira — não existe (nem foi inventado aqui) um endpoint
     "concluir toda a triagem".
   - As telas 2.1/2.7, que mostram "um cartão por triagem", agrupam essas
     linhas por `coletaId` no app (`TriagemController.agruparPorColeta()`),
     calculando progresso e status agregados.
6. **Os valores exatos de status (`statusAtual`) e cargo (`role`) não são
   conhecidos** sem consultar o banco — `Status` e `Cargo` são tabelas de
   domínio livres (strings cadastradas, não um enum fixo no código). O único
   valor confirmado no código do backend é `"CONCLUIDA"` (hardcoded em
   `TriagemService.concluir()`). Por isso, toda classificação de status neste
   app é por palavra-chave (`contains("CONCLU")`, `contains("AGEND")` etc.),
   igual ao padrão que `PedidoAdapter` (Empresa) já usava. Se os valores reais
   do banco forem diferentes do esperado, ajuste os métodos `rotuloStatus` /
   `classificarMaterial` / `classificarStatus` nos arquivos citados.
7. **Editar o nome do cooperado (lápis na tela 2.4) não está funcional.**
   `PUT /usuarios/{id}` (`Requests.UsuarioRequest`) exige CPF e senha não-vazia
   no corpo — não dá para atualizar só o nome sem pedir a senha atual de novo.
   O ícone existe (fiel ao wireframe) mas só mostra um aviso explicando isso,
   em vez de simular uma atualização que quebraria os outros campos.

---

## 3. Mudanças fora da pasta da área Cooperado (e por quê)

O prompt pediu para reaproveitar `SessionManager`/`ApiClient`/`AuthInterceptor`
sem modificá-los — e não modificamos. Só que **nenhum dos dois** guarda hoje
quem é o Funcionario (cooperado) logado — só `SessionManager.getEmpresaId()`
existe, pra Empresa. Sem isso, nenhuma tela de Cooperado saberia "quem" está
logado depois que o app reabre (o token continua válido, mas não há
`funcionarioId` em lugar nenhum). Foram feitas as seguintes mudanças mínimas,
fora da pasta da área Cooperado, para viabilizar isso — todas comentadas no
próprio código:

| Arquivo | Mudança |
|---|---|
| `dto/response/LoginResponse.java` | Adicionado o campo `usuarioId` — a API (`POST /auth/login`) já devolvia esse valor, o DTO só não capturava. |
| `controller/AuthController.java` | Depois do login, se o `role` não for de Empresa/Cooperativa-admin, resolve o `Funcionario` correspondente (via `FuncionarioResolver`, novo) e guarda em `CooperadoSession` (novo). Não altera o fluxo existente de resolução de `empresaId`. |
| `view/LoginActivity.java` | Depois do login, se o `role` contiver "COOPERADO", abre `CooperadoHomeActivity` em vez de `MainActivity` (que continua sendo o destino para todos os outros roles, exatamente como antes). |
| `RenovaiApplication.java` | Chama `CooperadoSession.init(this)` ao lado de `SessionManager.init(this)` (que continua intocado). |
| `AndroidManifest.xml` | As 7 novas Activities registradas (nenhuma como launcher — `SplashActivity` continua sendo a única). |
| `res/values/colors.xml` | Uma cor nova (`background_light_cooperado`); as 3 cores existentes foram mantidas. |

`CooperadoSession.java` (novo, arquivo próprio, SharedPreferences próprio
`"renovai_cooperado_session"`) guarda `funcionarioId`, `cooperativaId`,
`cooperativaNome`, `cargo`, `usuarioNome` e, à parte, o `usuarioId` bruto (pra
permitir tentar resolver de novo se a primeira tentativa falhar por uma queda
de rede passageira — ver `PerfilCooperadoController`).

**Importante:** o `role` de um login de Empresa/Cooperativa-admin usa o mesmo
campo (`usuarioId`, no JSON) só que preenchido com `Perfil.perfilId` em vez de
`Usuario.usuarioId` — é assim que o próprio `AuthService.login()` do backend
funciona hoje (reaproveita o campo). Por isso a resolução do Funcionario só é
tentada quando o `role` não é `"GESTOR_EMPRESA"` nem `"ADMIN_COOPERATIVA"`.

---

## 4. Como compilar e testar

1. Copie o conteúdo deste zip por cima de `Mobile-main/app/src/main/`
   (os arquivos Java caem nos pacotes certos, os recursos em `res/`, e o
   `AndroidManifest.xml` e `colors.xml` já vêm com o conteúdo antigo + o novo
   mesclados — não precisa mesclar na mão).
2. Abra no Android Studio, deixe o Gradle sincronizar (nenhuma dependência
   nova foi adicionada — só as que já estavam em `build.gradle.kts`).
3. Para testar de ponta a ponta: faça login com uma conta cujo **cargo**
   (tabela `cargos`, referenciada por `funcionarios.cargo_id`) contenha a
   palavra "COOPERADO" — o app te leva direto pra `CooperadoHomeActivity`.
   Se ainda não existir esse cargo/funcionário no banco de testes, é preciso
   cadastrar um antes (`POST /funcionarios` ou `/funcionarios/pre-cadastro`
   + completar cadastro, telas 1.5/1.6 — já implementadas).
4. Para abrir uma tela específica direto (sem passar pelo login todo santo
   dia), dá pra usar o `adb`, por exemplo:
   `adb shell am start -n com.example.renovai/.view.CooperadoHomeActivity`
   — mas lembre que ela vai checar `CooperadoSession`, então só funciona
   depois de já ter logado ao menos uma vez nesse aparelho/emulador.

---

## 5. Notas de arquitetura

- **`ColetaApiService.java` foi corrigido**, não só estendido: o método
  `listar` que já existia usava `coletas?cooperadoId=...` (query param), mas o
  endpoint real é `coletas/por-cooperado/{cooperadoId}` (path param) — a
  versão antiga nunca teria funcionado contra a API de verdade.
- **`ColetaResponse.java` teve um campo removido** (`origem`), que não existe
  no record real do backend e não era usado em nenhum outro lugar do app
  (conferido com busca no código antes de mexer).
- Foram criadas as interfaces `TriagemApiService`, `RateioApiService`,
  `FuncionarioApiService` e `RotaApiService` porque nenhuma delas existia
  ainda — não havia o que "reaproveitar ou estender". `ColetaApiService` e
  `MaterialApiService` (essa última já existia e não precisou de nenhuma
  mudança) foram reaproveitadas como estavam.
- `CooperadoBottomNav.java` (novo) centraliza a navegação das 5 abas
  (Rateios/Coletas/Home/Triagens/Perfil) — como o prompt pede só Activities
  (sem Fragments), cada aba é sua própria Activity, e trocar de aba é
  `startActivity` + `finish()`, pra não empilhar tela.
- `DetalheDialogHelper.java` e `CooperadoUi.java` (novos) evitam duplicar,
  em 3+ Activities, o código de montar o diálogo de detalhes e de gerar as
  iniciais do avatar (esse segundo é a mesma lógica que já existia, privada,
  em `EmpresaActivity.gerarIniciais` — extraída pra um lugar comum, sem mexer
  em `EmpresaActivity`).
- Os drawables novos (`*_cooperado_background.xml`, `ic_*_cooperado.xml` etc.)
  são cópias próprias, deliberadamente, em vez de apontar para os recursos já
  existentes com "empresa" no nome — mantém a área de Cooperado independente
  dos recursos da área de Empresa. Recursos genéricos que já existiam e não
  tinham nome de área nenhuma (`voltar.xml`, `avatar_placeholder_background.xml`,
  `kpi_card_background.xml`, `menu_background.xml`, `logo_app.xml`) foram
  reaproveitados diretamente, sem duplicar.
- Paleta de cores: foi usada a paleta que a Empresa/Login já usam de verdade
  (`#519059` verde, `#882B4E` vinho, `#D4E699` oliva, `#E8A9BB`/`#FBE4EA` rosa)
  em vez da paleta sugerida no prompt original (`#27A84F` etc.), para manter
  consistência visual com as telas já implementadas.

---

## 6. Sugestões de próximos passos (fora do escopo deste pacote)

- Adicionar campos em `ColetaRequest`/`Coleta` (backend) para materiais,
  necessidade de triagem e data prevista, se essa informação precisar mesmo
  ser persistida (hoje só existe na tela).
- Um endpoint de upload de imagem (S3 pré-assinado, Cloudinary etc.) pra
  "Foto da coleta" parar de ser só uma pré-visualização local.
- Um `GET /rateios/por-cooperado/{id}` na API evitaria o N+1 do
  `RateiController` (mobile).
- Confirmar no banco os valores reais de `cargos.cargo` e
  `status.status_atual` (referencia=COLETA/TRIAGEM) e trocar os casamentos
  por palavra-chave por comparações exatas, se fizer sentido.

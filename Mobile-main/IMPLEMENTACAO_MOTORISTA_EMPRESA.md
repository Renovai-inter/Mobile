# Motorista + Empresa — o que foi feito, e revisão geral do projeto

> Documento histórico. O motorista não usa mais `estaAtiva=false` ou progresso local como conclusão de viagem. O comportamento atual está na [revisão das três áreas](REVISAO_COOPERADO_GESTOR_MOTORISTA.md); a Empresa segue [os contratos atualizados](IMPLEMENTACAO_EMPRESA_API.md).

## Backend: patch novo (8 arquivos, nenhum arquivo do patch anterior reaproveitado)

Arquivos em `/mnt/user-data/outputs/`: `SecurityConfig.java`, `AuthController.java`, `AuthService.java`,
`Requests.java`, `Responses.java`, `PerfilRepository.java`, `TelefoneRepository.java`, `EmpresaContaController.java`.

Baseado na versão do backend que já tinha o login de empresa e o CORS configurados (a que vocês me
mandaram desta vez, `renovai-api-revisado__2_`, estava 4 commits **atrás** dessa — sem login de empresa
funcionando e sem CORS. Usei a versão mais recente como base; se isso não bater com o que está rodando
em produção agora, me avisem).

- **`POST /auth/cadastro`** (`AuthController`/`AuthService`) — a tela 1.4 (Cadastro de Empresa) já estava
  pronta no app e **chamava esse endpoint, mas ele não existia** — todo cadastro de empresa dava 404.
  Criei o endpoint: cria a Empresa, o Endereço (texto livre), o Perfil com senha já criptografada, e o
  Telefone, tudo em uma chamada, e devolve token (a empresa já entra logada). Sem essa rota, a área da
  Empresa não tinha como ser testada de ponta a ponta, então esse conserto era obrigatório.
- **`SecurityConfig`** — `/cooperativas/**` só liberava GET pra ADMIN_SITE/ADMIN_COOPERATIVA. Isso bloqueava
  a Empresa de buscar cooperativas (tela 5.2) e ver o perfil público (5.2.1). Abri GET para qualquer
  autenticado (a escrita continua restrita como antes).
- **`EmpresaContaController`** (`/empresas-conta/meu-perfil`, GET/PATCH) — a Empresa precisa editar o
  próprio e-mail/CNPJ/endereço (tela 5.6), mas `PUT /perfis/{id}` exige ADMIN_SITE/ADMIN_COOPERATIVA e
  nem aceita senha. Em vez de abrir esse endpoint pra qualquer autenticado (o que deixaria uma conta
  editar o Perfil de **qualquer outra**, bastando saber o id), criei um endpoint que resolve sempre a
  conta de quem está logado pelo e-mail do token — nunca por id vindo do corpo.
- **`PerfilRepository.existsByCnpj`**, **`TelefoneRepository.findByPerfil_PerfilId`** — pequenos métodos
  novos usados pelos dois pontos acima.

**Verificação:** sem `lombok.jar` disponível neste ambiente não deu pra compilar o backend de verdade
(as classes de modelo usam `@Data`/`@Builder`), mas conferi à mão, campo por campo, que os métodos e
construtores usados (`Empresa.builder()`, `Perfil.builder()`, `Endereco.setLogradouro`, etc.) batem com
os modelos reais, e todos os arquivos passam no balanceamento de chaves/parênteses.

## O que ficou órfão do patch anterior (achado da revisão)

Ao comparar com o que vocês me mandaram desta vez, vi que a equipe **reescreveu a área do Gestor pra não
depender mais do meu patch anterior** (`GestorController.java`, ainda em `/mnt/user-data/outputs/` de uma
entrega antiga): "Nova Triagem" agora usa `/equipes` + `/equipes-cooperados` + `/triagens` (com peso
marcador de 0,001 kg); o rateio agora sempre usa `/rateios/executar-geral` e `/executar-proporcional`
(sem edição manual de porcentagem); e a edição de funcionário caiu para só a função (via
`/funcionarios/{id}/cargo/{cargoId}`, que já existia). **Se esse `GestorController.java` antigo nunca foi
publicado no backend, não precisa fazer nada — o app não usa mais nenhum dos seus endpoints.** Se já foi
publicado, pode remover sem afetar nada.

## Motorista (telas 3.1 a 3.3, 4 telas no total — o wireframe tem uma tela a mais que o mapeamento: "Rotas
da semana")

| Tela | Arquivo |
|---|---|
| 3.1 Home ("Rotas do Dia") | `MotoristaHomeActivity` |
| Rotas da semana (nav "Rotas") | `MotoristaRotasActivity` |
| 3.2 Rota selecionada | `MotoristaRotaDetalheActivity` |
| 3.3 Perfil | `MotoristaPerfilActivity` |

A API não tem nada de "rota em andamento", progresso por parada, data/hora da rota, nem motorista
atribuído — só `estaAtiva` (aberta/fechada). Tudo isso é local no aparelho (`MotoristaLocal`):
- "Em andamento" = rota aberta que **este aparelho** marcou como iniciada.
- Progresso por parada = o motorista marca cada parada como feita tocando nela; fica salvo localmente.
- "Abrir GPS do trajeto" abre o Google Maps (Intent com origem/paradas/destino, sem SDK de mapas).
- "Finalizar Rota" chama `PUT /rotas/{id}` com `estaAtiva=false` — é o **primeiro** lugar no app que
  realmente fecha uma rota (o Gestor só lista aberta/fechada, nunca fechava nenhuma).
- Os contadores "Atribuídas/Em Andamento/Concluídas" da Home são uma aproximação razoável (dados do
  mockup também eram só ilustrativos) — documentado no código.

## Empresa (telas 5.1 a 5.6, 12 telas no wireframe)

| Tela | Arquivo |
|---|---|
| 5.1 Home (Dashboard) | `EmpresaHomeActivity` |
| 5.2 Busca de Materiais | `EmpresaCooperativasActivity` |
| 5.2.1 Perfil Público da Cooperativa | `EmpresaPerfilPublicoActivity` |
| 5.3 / 5.3.1 Enviar Pedido / Finalização | `EmpresaEnviarPedidoActivity`, `EmpresaFinalizarPedidoActivity` |
| 5.4 / 5.4.1 Pedidos / Detalhe | `EmpresaPedidosActivity`, `EmpresaPedidoDetalheActivity` |
| Conversa da empresa (chat) | `EmpresaChatActivity` |
| 5.5 Favoritas | `EmpresaFavoritasActivity` |
| 5.6 Perfil da Empresa | `EmpresaPerfilActivity` |

Reaproveitei o header/menu/ícones de Empresa que já estavam prontos no projeto (`header_empresa_background`,
`menu_empresa.xml`, etc.) — só faltava ligar em Activities de verdade; a `EmpresaActivity`/`HomeFragment`
antigas (com 4 das 5 abas sem nenhuma ação) ficam no projeto sem uso, sem que eu tenha apagado nada.

**Limitações da API e como tratei cada uma:**
- **Preço na busca (5.2):** a API não devolve nota nem preço na lista de busca — só o perfil público
  de cada cooperativa tem isso. Busco a nota de cada resultado à parte (uma chamada por card). O chip
  "Melhor preço" **não tem preço pra ordenar nessa tela** (preço é por material × cooperativa, só existe
  ao abrir uma cooperativa específica) — na prática ele ordena pela nota, igual o "★4+".
- **Endereço do pedido:** `Pedido` não tem campo de endereço — vai dentro de `observacao`, no formato
  `"Endereço: X | Complemento: Y"`, e a tela de detalhe sabe reler esse formato.
- **Qual cooperativa recebeu um pedido "Aberto":** a API não devolve isso (só depois que vira negociação).
  Guardo localmente no momento do envio (`EmpresaLocal`); uma vez que existe negociação, uso o dado real
  da API em vez do guardado localmente.
- **"Cooperativas Recentes" na Home:** virou a lista de Favoritas (não existe histórico de interação
  recente na API).
- **"Anos no mercado" do perfil público:** não existe no schema — omitido (não inventei número).
- **Meios de comunicação / horário:** mostro o texto cru de `contatoPreferencial`/`horarioFuncionamento`
  (campos únicos, não telefone+e-mail separados).

## "Esqueci minha senha" (1.7/1.7.1) — reaproveitado e consertado

A tela 1.7.1 (nova senha) já existia mas não chamava nenhuma API; a 1.7 (pedir e-mail) nem existia.
Criei `EsqueciSenhaActivity` (1.7) e liguei a tela existente (`RecuperarSenhaActivity`, 1.7.1) em
`POST /auth/esqueci-senha` e `POST /auth/redefinir-senha` (endpoints que já existiam prontos no backend).

**Limitação importante que não dava pra resolver só no app:** o projeto não tem nenhum serviço de
e-mail — a API gera o código de redefinição e grava no banco, mas **não envia pra ninguém**. Por isso
tive que colocar um campo pra colar o código manualmente na tela 1.7.1: hoje, na prática, só quem tem
acesso ao banco (o script `listar_cooperados.py` já dá essa base, adaptável) consegue recuperar esse
código pra passar pro usuário. Isso é uma limitação de infraestrutura do projeto, não do app.

## Revisão geral do app (pedido "revise tudo")

- **Padrão seguido:** toda a área nova usa exatamente o mesmo desenho das áreas Gestor/Cooperado —
  Activity por tela, cache "mostra o que tem e atualiza depois" (`EmpresaData`/`MotoristaData`, mesmo
  molde do `GestorData`), nav própria por área, e os mesmos utilitários (`GestorUi`, `ListaAdapter`,
  `RotaMapView`, `RotaEstimador` foram reaproveitados sem duplicar código).
- **Sessão:** Motorista reaproveita `CooperadoSession` (é Funcionario, como Cooperado/Gestor); Empresa
  usa `SessionManager` (é Perfil, não Funcionario) — os dois já existiam no projeto antes de mim.
- **Verificação feita:** todos os XML (layouts + manifest) validados; `javac` no projeto inteiro sem
  erro de sintaxe (os "erros" que aparecem são só classes do Android/Retrofit ausentes, porque este
  ambiente não tem o SDK — mesma limitação das entregas anteriores); conferência cruzada de
  `R.id`/`R.layout`/`R.drawable`/`R.menu` em todo arquivo novo; nenhuma activity duplicada no Manifest.
  **Sem SDK Android aqui, não rodei o Gradle** — se o build acusar algo, cole o erro que eu corrijo.
- **Coisas que ficaram fora do escopo desta rodada, de propósito:** o botão "cooperativa" da tela
  `EscolheActivity` (1.3) continua sem ação — cadastro de cooperativa não está em nenhuma tela do
  mapeamento, então não criei essa tela; o `MainActivity` de teste (login hardcoded) continua no
  projeto, sem uso, como já estava.

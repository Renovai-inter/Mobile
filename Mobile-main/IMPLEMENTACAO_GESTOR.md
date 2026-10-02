# Área do Gestor da Cooperativa — o que foi feito e como usar

> Documento histórico. Para o comportamento atual e as dependências da API do pacote (7), consultar [a revisão das três áreas](REVISAO_COOPERADO_GESTOR_MOTORISTA.md) e [a integração Empresa](IMPLEMENTACAO_EMPRESA_API.md).

Construído em cima do **seu app atual** (`renovai.zip`, com `CooperadoPreload`, `ApiClient` local etc.).
Só **2 arquivos existentes** foram alterados: `AndroidManifest.xml` (activities + FileProvider) e `view/LoginActivity.java`
(redirecionamento do Gestor + pré-carga). Todo o resto é novo.

## Login → Gestor
`LoginActivity`: role que contém `GESTOR` e **não** contém `EMPRESA` → limpa o cache da conta anterior, roda
`GestorData.precarregar()` (8 listagens em paralelo, no máximo 15 s) e abre `GestorHomeActivity`.
`GESTOR_EMPRESA` continua indo para `MainActivity`; `COOPERADO` continua indo para `CooperadoHomeActivity`.
A sessão do gestor reaproveita `CooperadoSession` (funcionarioId/cooperativaId/nome), preenchida pelo `AuthController` que você já tem.

## Cache ("guardar listagens e mostrar depois")
`GestorCache` (memória, validade de 2 min) + `GestorData` (camada de dados). Toda tela chama, por exemplo,
`GestorData.coletas(false, ouvinte)` e recebe **duas vezes**: primeiro o que já está no cache (`doCache = true`, aparece na hora),
depois o dado novo da API. Os dados nunca são apagados ao vencer — só marcados como velhos. Gravações (aceitar pedido, fechar rateio,
criar rota/triagem…) invalidam só as listagens afetadas.

## Telas (todas do planejamento 4.1–4.9 + as do wireframe Gestor.png)
| Tela | Activity |
|---|---|
| 4.1 Home (KPIs, ações rápidas, recentes) | `GestorHomeActivity` |
| 4.2 Coletas (filtros estado/origem) e 4.2.1 detalhe | `GestorColetasActivity`, `GestorColetaDetalheActivity` |
| 4.3 Estoque | `GestorEstoqueActivity` |
| 4.4 / 4.4.1 Relatórios | `GestorRelatoriosActivity`, `GestorCriarRelatorioActivity` |
| 4.5 Pedidos, pendentes, 4.5.1 detalhe e chat | `GestorPedidosActivity`, `GestorPedidosPendentesActivity`, `GestorPedidoDetalheActivity`, `GestorChatActivity` |
| 4.6 Perfil | `GestorPerfilActivity` |
| 4.7 / 4.7.1 Rateios | `GestorRateiosActivity`, `GestorRateioDetalheActivity` |
| 4.8 / 4.8.1 / 4.8.2 Triagens | `GestorTriagensActivity`, `GestorTriagemDetalheActivity`, `GestorNovaTriagemActivity` |
| 4.9 Funcionários + detalhe | `GestorFuncionariosActivity`, `GestorFuncionarioDetalheActivity` |
| Rotas (lista, nova, detalhe) | `GestorRotasActivity`, `GestorNovaRotaActivity`, `GestorRotaDetalheActivity` |

## Backend: patch novo (2 arquivos, nenhum arquivo existente alterado)
- `controller/GestorController.java` e `repository/TelefoneRepository.java`
  - `POST /gestor/rateios/manual` — fecha rateio com os valores editados pelo gestor (a API só sabia calcular sozinha). Bloqueia mês duplicado.
  - `POST /gestor/triagens/iniciar` — cria equipe + vincula funcionários + abre uma linha de triagem (0 kg) por material da cooperativa. Com 0 kg não mexe no estoque.
  - `GET /gestor/funcionarios/por-cooperativa/{id}`, `GET/PATCH /gestor/funcionarios/{id}` — e-mail, CPF, telefone e cargo (a API não devolvia e-mail/telefone).
- **Sem o patch o app continua abrindo**: funcionários usam os endpoints antigos, o rateio cai no `executar-geral/proporcional` (se você não editou %),
  e as telas que dependem do patch (iniciar triagem, editar e-mail/perfil) avisam claramente.

## Regras que o app assume (confirme no banco)
1. O **cargo do gestor** precisa ser `GESTOR_COOPERATIVA` — é o que o `SecurityConfig` exige (`ROLE_` + cargo em maiúsculas). Se for outro texto, o login abre a área, mas a API responde 403.
2. Status de pedido/negociação são achados na tabela `status` por palavra-chave (Aceit*, Recus*, Negocia*, Conclu*). Negociação usa os literais que o backend já usa (`EM_NEGOCIACAO`, `CONCLUIDO`, `RECUSADO`).
3. **Aceitar pedido**: tenta a procedure `sp_aceitar_pedido_cooperativa`; se falhar, muda o status do pedido e abre a negociação (com os itens) pelos endpoints normais. Em ambos os casos garante que exista uma negociação para o chat.
4. **Chat**: o remetente é o `Perfil` da cooperativa (a API exige `perfilId`); é resolvido via `GET /perfis`.
5. Despesas de água/energia são criadas como despesa `VARIAVEL` (valores de `TipoDespesa`: FIXA/VARIAVEL) e lançadas no mês do rateio.

## Limitações da API e como cada uma foi tratada (nada foi inventado silenciosamente)
- **Relatórios**: não existe endpoint. O PDF é gerado no aparelho (dados reais de negociações/triagens/coletas), guardado no app e abre/compartilha via FileProvider.
  *Impacto ambiental* usa fatores médios por kg (papel 0,9/25/3,5 · plástico 1,5/10/5,0 · metal 8/40/12 · vidro 0,3/2/0,5 · outros 0,5/5/1 — CO2 kg / água L / energia kWh); o PDF diz que são **estimativas**.
- **Rotas**: a API não guarda coordenadas, distância, tempo, data de criação, motorista ou veículo. Distância/tempo são **estimados** com o `Geocoder` do Android (linha reta × 1,3, 30 km/h) e aparecem como "(estimado)" — ou ficam "—" se o aparelho não geocodificar. Data de criação: só das rotas criadas por este app. Motorista: cooperados das coletas da rota. Veículo: "Não informado". O mapa é esquemático (ordem real das paradas, posições ilustrativas).
- **Coleta não guarda materiais**: os materiais da tela 4.2.1 vêm das linhas de triagem da coleta.
- **Home / Estoque total** = estoque triado + coletas ainda não processadas ("em trânsito", por palavra-chave do status).
- **Pedidos pendentes** (KPI) = pedidos abertos (aguardando resposta) + negociações em andamento.
- **Endereço de entrega** do pedido não existe na API → "Não informado". "Complemento" mostra a observação do pedido.
- **Rateio Tipo 1** = divisão igual; **Tipo 2** = proporcional a coletas + triagens do mês (por nome na triagem). O "aberto" do mês é calculado no app (a API não guarda rateio aberto).
- **Detalhes da triagem**: a linha "Empresa" do wireframe virou "Coleta feita por"; "Duração" mostra "—" (a API não registra hora de conclusão).
- **Estoque na conclusão da triagem**: hoje o backend só credita estoque em `POST /triagens`; `concluir`/`atualizar` **não** creditam. Isso é anterior a esta entrega e afeta os números do Estoque — vale corrigir no `TriagemService`.

## Pequenos acréscimos ao wireframe (para nenhuma tela ficar inacessível)
Home: links "Ver rotas" e "Meus relatórios" abaixo das ações rápidas · Coletas: filtro extra "Imediata" (previsto no planejamento) ·
Rateio detalhe: cartão "Gastos do período (água e energia)" (previsto no 4.7.1) · Perfil: "Sair da conta".

## Como foi verificado
Sem SDK Android neste ambiente, então **não houve build no Gradle nem teste em aparelho**. Foi feito: parse sintático de todos os `.java` com o
`javac` (0 erros de sintaxe), validação de todos os XML, conferência de todo `R.id/R.layout/R.drawable/R.menu` contra os recursos,
e conferência de **todas** as rotas do Retrofit e dos corpos de requisição contra os controllers/records reais da API (+ o patch).
Se o primeiro build acusar algo, cole o erro que eu corrijo.

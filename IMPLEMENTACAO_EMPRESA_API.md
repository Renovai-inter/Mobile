# Área Empresa — integração com a API revisada

Referências usadas: `empresa-conta-contratos.md` fornecido em Downloads e o código do pacote `renovai-api-revisado (7).zip`, incluindo `EmpresaContaController`, `NegociacaoController`, `NegociacaoFluxoService` e DTOs; seção 5 do mapeamento de telas; TAP profissional 2.0.

## Telas e contratos

Todos os caminhos abaixo são relativos à base `/api/` configurada em `ApiClient`.

| Tela / operação | Contrato utilizado |
| --- | --- |
| Cadastro | `POST auth/cadastro`, incluindo CPF do responsável |
| Identificação da empresa e edição cadastral | `GET/PATCH empresas-conta/meu-perfil` |
| Dashboard | `GET empresas-conta/dashboard`; quantidade de favoritas obtida na listagem de favoritos |
| Busca | `GET empresas-conta/cooperativas?categoriaId=&cidade=&quantidadeMin=` |
| Categorias | `GET categorias-material` |
| Materiais globais | `GET empresas-conta/materiais` |
| Perfil público e estoque por cooperativa | `GET empresas-conta/cooperativas/{id}` |
| Interesse em materiais | `GET/PUT empresas-conta/interesse`; a edição usa `PUT` com `categoriaIds` |
| Envio de pedido | `POST empresas-conta/pedidos` |
| Pedidos próprios | `GET empresas-conta/pedidos` |
| Pedido e materiais | `GET empresas-conta/pedidos/{id}` e `GET empresas-conta/pedidos/{id}/itens` |
| Cooperativa do pedido | `GET empresas-conta/meus-pedidos/{id}/cooperativas` |
| Favoritos | `GET/POST empresas-conta/favoritos`, `DELETE empresas-conta/favoritos/{cooperativaId}` |
| Avaliações e distribuição | `GET empresas-conta/cooperativas/{id}/avaliacoes`, `GET empresas-conta/cooperativas/{id}/estrelas` |
| Avaliação vinculada a compra | `POST empresas-conta/avaliacoes` com `pedidoId`, `cooperativaId`, `nota` e `comentario` |
| Negociações próprias | `GET empresas-conta/negociacoes`, relacionadas ao pedido no app |
| Aceitar valor / recusar | `PATCH empresas-conta/negociacoes/{id}/aceitar` sem body e `PATCH empresas-conta/negociacoes/{id}/recusar` com justificativa |
| Conversa | `GET/POST empresas-conta/negociacoes/{id}/mensagens` |

## Comportamento corrigido

- Componentes, estilos `G.*`, navegação e utilitários compartilhados seguem os padrões existentes dos outros perfis.
- Dashboard mostra pedidos enviados, aceitos, valor negociado, favoritas e ações rápidas. Pedidos enviados não são rotulados como “do mês”, pois o endpoint não recebe período.
- A busca pré-seleciona os interesses. Selecionar várias categorias reúne os resultados dos filtros da API, sem duplicar cooperativas. Cidade e quantidade mínima são enviadas ao servidor. A digitação tem atraso de 400 ms; respostas antigas não substituem a pesquisa atual.
- Os cartões da busca mostram o estoque público. O filtro antigo “Melhor preço”, que na verdade ordenava por nota, foi substituído pela seleção de categorias.
- Materiais de pedido são globais: o app cruza seus IDs com o estoque público da cooperativa. Não exige `cooperativaId` no material global.
- O pedido permite preço proposto por kg, valida precisão de peso/preço e soma os pesos de itens do mesmo material antes de comparar com o estoque.
- O envio faz uma operação única, com `cooperativaId`, `chaveSolicitacao` e `itens`. A solicitação é congelada após iniciar o envio; uma repetição usa o mesmo corpo e UUID. Solicitações pendentes são guardadas por empresa para retomada após encerramento do processo. Rascunhos ainda não enviados ficam em memória.
- Não há envio separado de pedido, itens e vínculo; não há sucesso parcial apresentado como pedido completo.
- Não são solicitados endereço de entrega e complemento, porque o contrato atômico não aceita esses campos. O detalhe exibe endereço e contato da cooperativa.
- Pedidos abertos continuam sendo exibidos quando ainda não existe negociação. Os cartões mostram materiais, peso, data e status; a listagem inclui total aprovado e filtros de status.
- A cooperativa é recuperada pelo vínculo no servidor, sem depender de preferências locais de um dispositivo anterior.
- Favoritos só mudam após resposta HTTP de sucesso. Avaliações usam um pedido aprovado da própria empresa; não são enviadas avaliações sem compra vinculada.
- O pré-carregamento usa um ouvinte independente por consulta. Login de empresa resolve a própria conta em `meu-perfil`, sem listar perfis de todos os usuários.
- A entrada antiga `EmpresaActivity` redireciona à Home atual, evitando o menu legado sem ações.
- A Empresa aceita o valor sem enviar `valorFinal` e sem concluir o pedido. A conclusão é realizada pela cooperativa; os botões de resposta desaparecem após aceite, recusa ou finalização.
- `Acordo fechado` sem data de fechamento é exibido como `Aceito`. A finalização considera o vínculo e as datas; uma recusa com data de fechamento continua sendo `Recusado`. Total aprovado inclui aceitos e finalizados. Filtros de status podem ser rolados em telas estreitas.
- O detalhe exibe `NegociacaoResponse.observacao`, incluindo a remoção da observação quando vier nula em uma nova consulta. Os materiais e pesos da negociação são exibidos quando presentes, com os itens originais como fallback. O chat permanece disponível após o aceite.
- Interesses são substituídos em uma chamada `PUT`, inclusive quando a seleção fica vazia. O app não limpa os interesses antes de salvar. Em erro HTTP mantém a seleção exibida; em falha de conexão consulta novamente para verificar se a operação foi confirmada.
- Favoritos e listagem de negociações não enviam `empresaId`; a API identifica a Empresa autenticada. A listagem faz uma consulta das negociações da conta para todos os pedidos, evitando uma chamada de negociação por cartão.

## Atualização da API e validação de ambiente

A passagem do gestor também foi corrigida: abrir negociação não marca o pedido como aceito; o gestor só conclui após o aceite da empresa, pelo valor acordado, usando `/negociacoes/{id}/concluir`. Contrapropostas enviam a observação no endpoint específico, sem criar uma mensagem especial manualmente. A proposta deixa de ser editável após aceite ou encerramento; relatórios do gestor só contabilizam conclusão confirmada.

Os limites anteriormente registrados para aceite, persistência de observação, substituição de interesses e rotas da conta foram resolvidos no código da API do pacote (7). O app foi migrado para esses contratos. A baixa de estoque, idempotência do aceite/conclusão e sincronização do vínculo são responsabilidade de `NegociacaoFluxoService`; o app não altera o estoque diretamente.

A base continua sendo a configurada em `ApiClient`. Os testes locais do app não verificam a revisão efetivamente implantada, os triggers PostgreSQL nem uma negociação entre duas contas reais.

## Validação

Comando final: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.

Resultado dessa etapa: compilação concluída, 18 testes aprovados (10 contratos, 7 estados e 1 teste básico), sem falhas. Lint concluído sem erros. A suíte atual e os avisos do projeto estão documentados em `REVISAO_COOPERADO_GESTOR_MOTORISTA.md`.

APK gerado em `app/build/outputs/apk/debug/app-debug.apk`; relatório estático em `app/build/reports/lint-results-debug.html`.

`EmpresaApiContractTest` exercita caminhos e filtros Retrofit, pedido atômico/idempotente, material global/estoque, CPF, avaliação, carrinho, aceite sem body, interesses vazios e múltiplos, favoritos sem `empresaId` e observação da contraproposta. `EmpresaStatusTest` verifica aceite/conclusão/recusa, variações do catálogo, precedência do vínculo aceito e seleção da negociação mais recente. São testes locais, sem criar pedidos ou avaliações no servidor.

Para validar a integração completa em dispositivo, usar uma conta de empresa e uma cooperativa de teste: cadastrar/logar, selecionar interesses, buscar por cidade/peso, favoritar, enviar pedido, interromper e repetir o envio, negociar com o gestor, conferir dashboard/vínculo/estoque e avaliar a compra. Não foi realizado teste ponta a ponta com conta autenticada nem validação visual em emulador.

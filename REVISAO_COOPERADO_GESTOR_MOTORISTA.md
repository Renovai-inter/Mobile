# Revisão das áreas Cooperado, Gestor e Motorista

Referência: código da API em `renovai-api-revisado (7).zip`, mapeamento 2.0 e orientação de manter o desenho das telas. Esta revisão substitui as descrições antigas de progresso local do motorista, fechamento de rota por `estaAtiva` e status de rateio por mês.

## Layout e navegação

- Cores, ícones, cartões, menus e estrutura das telas foram mantidos. Os textos foram corrigidos quando anunciavam um estado que a API não informa.
- `TelaInsets` aplica o padding a partir das medidas originais. A barra do cooperado não acumula o espaço de navegação a cada evento de insets. Sua altura continua sendo 72 dp mais o espaço dos gestos/botões do Android.
- Cabeçalhos e conteúdo das três áreas respeitam barra de status, recortes, laterais e teclado. O chat do gestor utiliza esse tratamento, sem aplicar o teclado duas vezes.
- Os botões flutuantes do cooperado ficam acima da posição real da barra inferior. O estoque usa uma coluna em telas estreitas ou com fonte maior, mantendo duas nas demais condições.

## Cooperado

- A coleta externa agora envia `tipoColeta: EXTERNA` e preserva `rotaId`. Entregas internas enviam `ENTREGA`, sem rota. A exibição dos cartões reconhece os dois nomes legados de coleta externa.
- O peso mínimo acompanha o contrato: 0,001 kg. Gravações não podem ser disparadas simultaneamente pelo mesmo formulário.
- Coletas e triagens invalidam os caches após gravação. A pré-carga avisa todos os ouvintes, aguarda as três consultas e não considera uma atualização com erro como válida. Respostas de uma sessão encerrada não repovoam o cache da conta seguinte.
- A conclusão de material salva primeiro peso e rejeito via `PUT /triagens/{id}` e só então chama `PATCH /triagens/{id}/concluir`. O rejeito digitado deixa de ser descartado. Pesos inválidos mantêm o diálogo aberto, e o app bloqueia gravações concorrentes.
- Rateios com distribuição registrada são exibidos como fechados, inclusive no mês atual. Falhas ao consultar distribuições não viram uma lista completa vazia. As listagens preservam dados anteriores ao ocorrer erro de atualização.
- Materiais globais aparecem no catálogo. Descrição, anexos, materiais da coleta e agendamento não têm persistência no `ColetaRequest` atual: esses controles foram deixados indisponíveis, sem sugerir que seriam salvos. Peso, origem e rota continuam sendo registrados.

## Gestor

- Os pedidos em negociação não são contados duas vezes na Home. Acordos aceitos aguardando conclusão não entram como vendas concluídas. O estoque principal mostra o estoque retornado pela API, sem somar peso bruto de coletas.
- A criação de triagem usa marcador bruto/rejeito de 0,001 kg, com entrada líquida zero. Isso evita creditar estoque fictício ao criar linhas que ainda não foram separadas. O marcador não aparece como material separado.
- Seleções de equipe/coleta não mudam durante a gravação. Falhas na consulta de pré-cadastro não transformam funcionários desconhecidos em ativos confirmados.
- O período do rateio usa formatação ISO completa, incluindo meia-noite e o último dia do mês. A versão anterior podia lançar exceção ao recortar uma data com apenas 16 caracteres.
- A prévia do rateio acompanha a API atual, que distribui vendas brutas e registra gastos separadamente. Não anuncia uma dedução de despesas que `RateioService` não faz. Participação e percentuais permanecem somente leitura.
- Lançamentos de água/energia sempre encerram o estado de carregamento, inclusive em falha parcial. Somente campos com lançamento confirmado são limpos. Fechamento e lançamentos bloqueiam toques repetidos.
- Relatórios aguardam todas as consultas e congelam tipo/período durante a geração. Falhas não geram PDFs com dados incompletos como se a consulta tivesse sido bem-sucedida.
- A criação do cadastro da rota exige partida, coleta e retorno. Na mesma tela, pode retomar paradas/endereço já confirmados sem recriar toda a rota. A API ainda não oferece uma operação atômica para esse cadastro; falhas sem confirmação exigem conferir o cadastro antes de repetir.
- Rotas são chamadas de disponíveis/inativas. A atribuição do motorista não é inferida do cooperado que registrou uma coleta.

## Motorista: Redis e GPS futuro

O app consulta somente `GET /rotas/por-cooperativa/{id}` e `GET /rotas/{id}`. O cadastro existente fornece nome, cooperativa, disponibilidade e endereços ordenados. Não fornece atribuição pessoal, data da viagem, cronômetro ou estado de cada parada.

`estaAtiva` é disponibilidade do cadastro da rota; não equivale a uma viagem concluída. O app não grava mais `estaAtiva=false` ao finalizar uma viagem e não usa as preferências do protótipo local como progresso confirmado. Também não chama `/coletas` com o papel motorista: o `SecurityConfig` do pacote não permite esse papel nessa área.

Início, marcação de parada e conclusão operacional ficam indisponíveis até existir o contrato correspondente na API. Os blocos visuais continuam nas telas, com estado indisponível indicado. As telas mostram rotas da cooperativa, sem anunciar uma atribuição diária que o servidor não devolve. O mapa permanece uma prévia esquemática; a abertura externa do trajeto não coleta GPS nem confirma andamento.

A integração futura deverá consumir a API responsável pelo estado no Redis: identificar a execução e motorista autenticado, consultar estado/paradas, confirmar início e cada andamento, finalizar execução e devolver duração. Os nomes das rotas, corpos e regras de repetição só serão implementados quando o contrato for fornecido. O Android não acessará Redis diretamente e não criará um espelho desse progresso no SQL.

## Dependências reais da API

1. No pacote enviado, `TriagemService.criar` credita estoque; `atualizar` e `concluir` não creditam a diferença nem o peso final. O app salva os pesos/rejeitos e conclui via API, mas a entrada de estoque após separação continua dependendo de correção do backend. Não foi acrescentada movimentação compensatória no cliente.
2. O contrato de coleta não inclui materiais, necessidade/data de triagem ou upload de arquivos. A regra de envio direto ao estoque para material único também não está implementada por esse contrato.
3. O cadastro de equipe/membros/linhas e o de rota/endereço/paradas são operações separadas. Sem endpoint transacional/idempotente, não é possível garantir atomicidade ou repetição após perda de confirmação só no app.
4. Os endpoints atuais de rateio não recebem percentuais manuais nem deduzem os gastos. O backend também não expõe `mesReferencia` na lista para confirmar com precisão o período de um rateio executado em outro mês. A distribuição oficial continua sendo calculada e registrada pela API.
5. GPS, atribuição e andamento de viagem aguardam a API futura, conforme solicitado.

## Verificações

- Auditoria de 72 declarações de endpoints das interfaces revisadas: todos os métodos/caminhos correspondem a controllers do pacote. O inventário está em `CONTRATOS_AREAS_AUDITADOS.json`. Isso verifica o contrato estático, não uma implantação remota.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug` e `assembleDebugAndroidTest` executados. A suíte tem 24 testes locais, incluindo contratos, coleta externa, marcador de triagem, pesos, datas e separação entre disponibilidade e viagem.
- Resultado final: compilação e testes locais aprovados; lint com zero erros e 1.036 avisos no projeto, sobretudo textos fixos (700 `HardcodedText` e 148 `SetTextI18n`). Esses avisos não foram ocultados por uma configuração nova.
- Três testes Android passaram no aparelho SM-A366E com Android 16. A inflação/medição abrange 27 layouts em 320 dp com fonte a 130%; o teste da barra aplica insets repetidamente e troca o tamanho da área dos gestos.
- Os testes de layout não fazem login ou chamadas à API. Não equivalem a testar cada fluxo com duas contas reais, dados de produção, GPS ou Redis. Nenhum pedido, rateio ou coleta foi criado no servidor durante a validação.
- A tentativa de copiar a imagem de teste após a execução não encontrou o arquivo; a nova tentativa ficou impedida pela desconexão do aparelho. A validação da barra se baseia nas medidas e assertions executadas no dispositivo, sem inspeção da imagem.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Relatórios: `app/build/reports/tests/testDebugUnitTest`, `app/build/reports/androidTests/connected/debug` e `app/build/reports/lint-results-debug.html`.

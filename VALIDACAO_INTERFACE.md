# Interface Renovai — implementação e validação

## Referências e escopo

Foram usados os arquivos `Renovai_Popup_Mobile.html` e `Renovai_Email_Codigo_6_Digitos.html` fornecidos pelo usuário. O componente Android mantém a composição de fundo creme, bordô, folhas, arredondamento de 27 dp, fechamento no topo e ações verticais. O calendário conserva o seletor nativo com as cores e a superfície do padrão.

`RenovaiDialogBuilder` centraliza os diálogos anteriormente construídos com MaterialAlertDialogBuilder e o seletor de origem da foto. Mantém seleção simples/múltipla, ações positivas/negativas/neutras, cancelamento, campos personalizados e os callbacks originais. Conteúdos longos rolam, e a altura das ações é reservada no AlertDialogLayout. Toasts de feedback e seletores do sistema Android conservam o comportamento nativo.

## Perfis e permissões

| Perfil | Comportamento |
| --- | --- |
| Empresa | Nome, CNPJ, descrição, e-mail, telefone, endereço e cidade usam o PATCH existente. Categorias de interesse usam o PUT existente. Campos agrupados por dados da empresa, contato/localização e preferências. |
| Gestor da cooperativa | Nome, e-mail, cargo e vínculo continuam em consulta. A interface informa como solicitar alterações. |
| Motorista | Nome, e-mail e cargo continuam em consulta, com orientação sobre alterações cadastrais. |
| Cooperado | Mantida a consulta dos dados e o controle de orientação cadastral; respostas tardias não acessam uma view destruída. |
| Administrador do site | Preservada a consulta do perfil e das permissões existentes. |

O editor da empresa carrega o valor atual, usa teclado adequado, informa campos obrigatórios e mantém texto e erro no formulário. Durante o PATCH, impede confirmação repetida e cancelamento ambíguo. O sucesso usa a resposta real, atualiza o cache e pede novo login quando o e-mail muda. Telefone é obrigatório segundo o serviço da API; cidade e descrição podem ser limpas.

Não foi inventado um endpoint de edição para os outros perfis. O contrato de perfil da empresa fornecido também não permite substituir a foto; a imagem recebida pode ser visualizada, sem simular uma gravação de foto de perfil.

## Fotos

O visualizador interno `FotoActivity` mantém a foto inteira, aceita pinça/arrasto, restaura o enquadramento com toque duplo e oferece fechar e tentar carregar novamente. As miniaturas continuam recortadas apenas no espaço reservado à miniatura; a abertura mostra a imagem completa. URLs HTTPS continuam sendo validadas pelo código existente.

As prévias locais usam Glide em vez de decodificação síncrona via setImageURI. O seletor OpenDocument permite manter a leitura da imagem após recriação da tela, sem permissão ampla de armazenamento. A seleção e a URL de upload já concluído são restauradas no cadastro de material. Durante o envio, a foto não pode ser trocada. A coleta mantém a câmera, o FileProvider e o pedido de permissão existentes. A recusa de acesso ao arquivo produz mensagem de erro em vez de exceção.

## E-mails e integração da API

A API do ZIP contém um tipo de e-mail: código de recuperação de senha. Existem duas formas de envio: SMTP e Google Apps Script. Não foram criados fluxos de confirmação de cadastro ou de notificação que não existiam na API.

As substituições estão em `integracoes/email-api`, e a API completa com as alterações está em `build/entregas/renovai-api-email-atualizado.zip`. O ZIP original não foi alterado.

- `src/main/resources/templates/email/codigo-redefinicao.html`: composição da referência, com a mensagem correta para recuperação de senha e placeholders `{{CODIGO}}` e `{{VALIDADE}}`.
- `src/main/resources/templates/email/assets/nimo.png`: o mascote exato extraído da referência.
- `src/main/java/com/renovai/api/service/EmailService.java`: adiciona o mascote ao MIME usando CID. Mantém os provedores, destinatário, assunto e alternativa em texto puro.
- `apps-script/Code.gs`: mesmo layout e imagem inline. Mantém o corpo `{ para, codigo, validade }`, as validações, a espera por endereço e o limite diário. Não passa a aceitar HTML ou assunto externos.

Para ativar os e-mails em produção, os arquivos `src/` precisam ser incorporados à API implantada. O Apps Script, que é o provedor padrão dessa revisão, precisa receber `apps-script/Code.gs` e ser republicado em uma nova versão da implantação existente, mantendo a URL. Apenas atualizar o Android ou publicar a API não substitui o HTML dentro de uma implantação Apps Script existente.

## Verificações

Resultados: 106 testes unitários Android aprovados; compilação e Lint sem erros (o projeto continua com avisos de Lint). A suíte completa de instrumentação tem 12 testes e passou também a 320 dp com fonte a 130%. Foram aprovados seis testes Node do e-mail, sete testes Java de envio/MIME, 59 verificações do repositório e dois testes de persistência/releitura de perfil em H2.

Compilação e validação Android:

```powershell
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline
```

Os testes de instrumentação usam componentes reais, sem login nem gravação em serviços externos. Cobrem callbacks, cancelar/fechar, seleção múltipla e ação neutra, formulário longo, ações verticais, bloqueio durante salvamento, abertura/fechamento de foto, campos do perfil e renderização do e-mail em 320 e 560 pixels. O teste antigo da navegação do cooperado foi atualizado para a casca atual `activity_cooperado_menu`.

Os testes foram executados diretamente por `adb shell am instrument`, porque faltam artefatos UTP no cache local do executor connectedDebugAndroidTest. O APK de testes foi compilado pelo Gradle normalmente.

```powershell
node tools/testar_email.cjs
```

Esses seis testes de e-mail verificam HTML comum aos provedores, código com zero inicial, validade dinâmica, PNG e CID, destinatário, alternativa de texto, rejeição de dados inválidos e limite de reenvio, sem envio real.

Na cópia local da API, `EmailServiceScriptTest`, `EmailVisualTest`, `EmpresaSchemaRepositoryTest` e `PerfilEmpresaPersistenciaTest` validam o contrato de envio, o MIME e a edição seguida de releitura pelo serviço e repositório reais. A persistência usa H2 isolado em modo PostgreSQL, sem alterar o banco fornecido.

A revisão da API exige Java 25, indisponível nesta máquina. A validação local usa o JDK 21 e uma cópia temporária do pom em `build/validacao-api-email/pom-validacao.xml`, substituindo apenas a versão de compilação. O pom do ZIP de entrega continua exigindo Java 25.

Capturas das renderizações estão em `build/qa-interface`. A inspeção por Chrome não pôde continuar devido a uma limitação da ferramenta de navegador; as imagens de verificação foram produzidas pela instrumentação Android e pelo WebView local, com a rede bloqueada no teste do e-mail.

## Limites da validação

Não foram usados credenciais ou perfis reais para alterar contas, nem foi enviado e-mail a uma caixa real. Não foi executado upload real para Cloudinary, captura de câmera física ou teste nos clientes Gmail/Outlook. A renderização em WebView verifica dimensões e recursos locais; o teste de MIME verifica o empacotamento do e-mail. A publicação dos templates e o teste em caixas reais permanecem etapas do ambiente de implantação.

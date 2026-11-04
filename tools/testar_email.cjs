// Testes locais do template e dos contratos; nenhum e-mail real é enviado.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const base = path.resolve(__dirname, '../integracoes/email-api');
const html = fs.readFileSync(path.join(base, 'src/main/resources/templates/email/codigo-redefinicao.html'), 'utf8').replaceAll('\r\n', '\n');
const source = fs.readFileSync(path.join(base, 'apps-script/Code.gs'), 'utf8');
let enviados = [];
const cache = new Map(), props = new Map();
const context = vm.createContext({
  GmailApp: { sendEmail: (...args) => enviados.push(args) },
  CacheService: { getScriptCache: () => ({ get: k => cache.get(k), put: (k,v) => cache.set(k,v) }) },
  PropertiesService: { getScriptProperties: () => ({ getProperty: k => props.get(k), setProperty: (k,v) => props.set(k,v) }) },
  Utilities: { formatDate: () => '2026-11-03', base64Decode: s => Buffer.from(s, 'base64'), newBlob: (bytes, type, name) => ({ bytes, type, name }) },
  ContentService: { MimeType: { JSON: 'application/json' }, createTextOutput: s => ({ setMimeType: () => JSON.parse(s) }) },
  Session: { getActiveUser: () => ({ getEmail: () => 'teste@example.invalid' }) }
});
vm.runInContext(source, context);
let casos = 0;
function teste(nome, executar) { executar(); casos++; console.log('OK ' + nome); }
function post(dados) { return context.doPost({ postData: { contents: JSON.stringify(dados) } }); }
teste('SMTP e Apps Script usam o mesmo HTML e preservam zeros iniciais', () => {
  const esperado = html.replaceAll('{{CODIGO}}', '012345').replaceAll('{{VALIDADE}}', '7');
  assert.equal(context.modelo('012345', 7), esperado);
  assert(esperado.includes('012345'));
  assert(esperado.includes('7 minutos'));
  assert(!esperado.includes('{{'));
});
teste('mascote CID tem arquivo PNG e inlineImages nos dois provedores', () => {
  const png = fs.readFileSync(path.join(base, 'src/main/resources/templates/email/assets/nimo.png'));
  assert.equal(png.subarray(1,4).toString(), 'PNG');
  assert(html.includes('cid:nimo'));
  assert(!html.includes('data:image'));
  assert.equal(context.nimo().bytes.compare(png), 0);
  const service = fs.readFileSync(path.join(base, 'src/main/java/com/renovai/api/service/EmailService.java'), 'utf8');
  assert(service.includes('helper.addInline("nimo"'));
});
teste('layout mantém tabelas, dimensões responsivas e link original de suporte', () => {
  assert(html.includes('@media(max-width:600px)'));
  assert(html.includes('max-width:560px'));
  assert(html.includes('width:100%'));
  assert(html.includes('mailto:renovai.suporte@gmail.com'));
  assert(!html.includes('482 916'));
  assert(!html.includes('Confirme seu e-mail'));
});
teste('destinatário, assunto, texto e código permanecem dinâmicos', () => {
  const resultado = post({para:'teste@example.invalid', codigo:'012345', validade:7});
  assert.equal(resultado.ok, true);
  assert.equal(enviados.length, 1);
  assert.equal(enviados[0][0], 'teste@example.invalid');
  assert(enviados[0][1].includes('012345'));
  assert(enviados[0][2].includes('7 minutos'));
  assert(enviados[0][3].htmlBody.includes('012345'));
  assert(enviados[0][3].inlineImages.nimo.bytes.length > 0);
});
teste('destinatário inválido e código inválido não enviam', () => {
  assert.equal(post({para:'invalido', codigo:'012345', validade:7}).ok, false);
  assert.equal(post({para:'outro@example.invalid', codigo:'abc123', validade:7}).ok, false);
  assert.equal(post({para:'outro@example.invalid', codigo:'012345', validade:90}).ok, false);
  assert.equal(enviados.length, 1);
});
teste('limite de reenvio é preservado', () => {
  assert.equal(post({para:'teste@example.invalid', codigo:'012345', validade:7}).ok, false);
  assert.equal(enviados.length, 1);
});
console.log(`${casos} testes de e-mail aprovados; nenhum envio real.`);

# Etapa 9: frontend do cliente (design)

**Data:** 2026-11-19
**Status:** em aprovação
**Cartão:** etapa 9 no [board](https://github.com/users/capozziV/projects/2)

## 1. Intenção
O usuário quer, nesta ordem:
1. **mostrar o backend a um recrutador em cerca de 2 minutos**, como um cliente veria o app;
2. ter um app para testar o backend no dia a dia;
3. aprender o básico de Angular.

O backend aparece pelo comportamento. São cinco coisas:
- a transferência conclui;
- a transferência é recusada pelo limite diário;
- a devolução aparece com o motivo;
- o extrato atualiza;
- a notificação chega.

O "por dentro" (correlation ID, linha do tempo da saga, logs) fica para uma etapa própria de observabilidade ([#202](https://github.com/capozziV/DigitalBanking/issues/202)).

## 2. Objetivos
- Telas do **cliente**, em português, rodando **local** (compose) e **na AWS** (`aws-up`).
- **Cadastro pelo próprio app.** O recrutador cria o próprio usuário em 1 a 2 minutos.
- **Bônus de boas-vindas** de R$ 100 ao abrir a conta, para o cadastro não cair num app vazio.
- **Usuário de demonstração** criado pelo seed, com saldo, extrato e notificações, para o recrutador só entrar e olhar.
- Teste ponta a ponta no navegador com **Playwright** no CI.

## 3. Fora do escopo
- Telas de admin: bloquear, encerrar, tipo de conta e transferência presa ([#201](https://github.com/capozziV/DigitalBanking/issues/201)).
- Observabilidade visível no app ([#202](https://github.com/capozziV/DigitalBanking/issues/202)).
- Internacionalização, tema escuro, app nativo.

**Anti-objetivos:** o app não pode parecer um painel técnico, nem pedir mais de 2 minutos até o recrutador ver uma transferência concluir.

## 4. Decisões do usuário
| Tema | Decisão |
|---|---|
| Quem usa | Só o cliente |
| Onde roda | Local e AWS |
| Visual | Estilo "Apple": fundo off-white `#f5f5f7`, preto e branco (`#1d1d1f`), cinza `#86868b` para texto secundário, fonte do sistema (`-apple-system`/SF), cartões brancos com cantos de 18 px e sombra leve; o saldo fica num cartão com os botões Transferir (preto) e Extrato (contorno) |
| Navegação | Menu lateral **fixo** no computador; **recolhível** (☰, deslizando da esquerda) no celular; o mesmo menu nos dois |
| Entrada e saída | Sem cor: ícone em círculo, **↓ em círculo preto cheio** e valor em negrito para entrada, **↑ em círculo vazado** e valor em cinza para saída |
| Login e cadastro | **Telas próprias** no Angular; o BFF fala com o Cognito pela API (sem a hosted UI) |
| Senha | **Exatamente 6 dígitos numéricos**, validados nas nossas telas e no BFF; bloqueio por tentativas erradas do Cognito ligado. É fraca de propósito: o banco é simulado, sem dinheiro nem dados reais, e o ADR registra isso |
| Bônus | R$ 100 (10000 centavos) creditados ao abrir a conta |
| Usuário de demonstração | Sim, pelo seed, além do cadastro. Sem botão "entrar como demo": o usuário guarda o e-mail e a senha e os envia ao recrutador |
| Destino da transferência | **Telefone celular** do destinatário, com confirmação do nome mascarado antes de enviar |
| Verificação do telefone | Sim, por código de 6 dígitos por SMS, atrás de uma porta. Nesta etapa o adaptador é **simulado**: o app mostra o código num aviso "SMS recebido". O SMS real da AWS fica para depois |
| Troca do telefone | Permitida em "Minha conta", com verificação do número novo |
| Testes | Unidade (Angular e NestJS) e Playwright |
| Idioma | Português |

As telas aprovadas como referência estão em `.superpowers/brainstorm/2215-1795093861/content/`, fora do git: `visual-inicio-v2.html` opção E, `visual-inicio-v3.html` opções F e G, e `entradas-saidas.html` opção H.

## 5. Abordagens
1. **Duas imagens (escolhida).** O Angular compilado é servido por um nginx; o BFF NestJS roda em outra imagem. O ALB e o nginx local roteiam `/` para o Angular e `/bff` para o BFF, na mesma origem, como `docs/arquitetura.md` já descreve. Custo: um pod, uma imagem e uma regra de ALB a mais, sobre o chart e as convenções de imagem da etapa 8.
2. **Uma imagem.** O BFF serve os arquivos do Angular. Tem menos peças, mas o front só pode subir junto com o BFF, e a arquitetura teria de mudar.
3. **CDN (S3 + CloudFront).** Seria outra origem, com CORS e cookie entre domínios, contra a decisão de mesma origem. Descartada.

## 6. Desenho
### 6.1 Estrutura
`frontend/` com npm workspaces, sem Nx:
- `web/`: Angular (standalone components, signals, Angular Material com tema próprio);
- `bff/`: NestJS;
- `e2e/`: Playwright.

### 6.2 Telas
| Tela | O que mostra | Rotas do backend (via BFF) |
|---|---|---|
| Entrar | E-mail e senha de 6 dígitos; link para Criar conta | Cognito |
| Criar conta | E-mail, senha de 6 dígitos (com confirmação), depois o código recebido por e-mail | Cognito |
| Completar cadastro | Nome e telefone celular no primeiro acesso, se `GET /customers/me` responder 404; depois, o código de verificação do telefone | `POST /customers`, verificação do telefone (nova) |
| Início | Cartão do saldo (saldo disponível, tipo de conta, cheque especial, Transferir e Extrato), limite diário usado e disponível, últimos lançamentos; sem conta, botão "Abrir conta" | `GET /accounts/...`, `GET /accounts/{id}/daily-limit`, `GET /accounts/{id}/statement` |
| Extrato | Lançamentos paginados com o ícone de entrada e saída; o bônus aparece como "Bônus de boas-vindas" | `GET /accounts/{id}/statement` (cursor) |
| Transferir | Telefone do destinatário e valor; o app mostra o nome mascarado ("Ana S.") para confirmar; depois o status até `COMPLETED`, `FAILED` ou `COMPENSATED`, com o motivo em português | busca por telefone (nova), `POST /transfers`, `GET /transfers/{id}` |
| Avisos | Lista de notificações, marcar como lidas, contador no menu | `GET /notifications`, `POST /notifications/read` |
| Minha conta | Dados do cliente e da conta; trocar o telefone (com verificação do número novo); encerrar conta (saldo zero, sem pendentes) | `GET /customers/me`, troca do telefone (nova), `POST /accounts/{id}/close` |

O builder confere os caminhos exatos de cada rota no OpenAPI de cada serviço.

### 6.3 BFF
- **Endpoints por tela, não proxy genérico.** Exemplo: `GET /bff/home` agrega conta, limite e últimos lançamentos.
- **Sessão no Redis** com cookie `HttpOnly`, `Secure` e `SameSite=Strict`, mais CSRF.
- **Refresh do token** no próprio BFF.
- **Correlation ID** com `AsyncLocalStorage` e logs JSON com pino.
- **Identidade numa porta própria, com dois adaptadores:**
  - Cognito na AWS, pela API: `SignUp`, `ConfirmSignUp`, `InitiateAuth` com `USER_PASSWORD_AUTH` e refresh;
  - mock local: o `mock-oauth2-server` não tem cadastro. O adaptador local aceita o cadastro, pula o código por e-mail e emite o token pelo mock.

  O builder define o desenho exato e o registra.
- **Validação da senha** de 6 dígitos também no BFF, não só na tela.
- **`Idempotency-Key`** gerada no Angular ao abrir o formulário de transferência; o BFF só repassa.
- **Status da transferência** por polling (RxJS) a partir do Angular, com intervalo e prazo definidos pelo builder.

### 6.4 Mudanças no backend e na infraestrutura
- **accounts: bônus de boas-vindas.**
  - Ao abrir a conta, um lançamento `CREDIT` de 10000 centavos, na mesma transação da abertura, com descrição própria.
  - Entra pelo outbox como os demais lançamentos, para o extrato e o notification.
  - Termo novo no `CONTEXT.md`: **Bônus de boas-vindas**.
  - O valor fica numa configuração, com o padrão de 10000.
- **customer: telefone do cliente.**
  - O cliente ganha o campo telefone (celular brasileiro, guardado em E.164, `+55DD9XXXXXXXX`), **único** entre os clientes e **verificado** por código de 6 dígitos com validade curta e limite de tentativas.
  - A transferência só aceita destinatário com telefone verificado.
  - O envio do código fica numa **porta** (`PhoneVerificationSender` ou nome equivalente). O adaptador desta etapa é simulado: o código chega ao app, que o mostra no aviso "SMS recebido". O SMS real da AWS é um adaptador futuro (cartão próprio no Backlog).
  - **Troca do telefone:** o cliente pede a troca, verifica o número novo e só então o antigo é liberado. Uma transferência é resolvida no momento do envio; trocar o telefone depois não afeta transferências já feitas.
  - **Busca por telefone:** devolve só o nome mascarado e o que o BFF precisa para achar a conta; revela que o número tem conta, como o PIX. É aceitável no banco simulado e fica no ADR.
  - O evento de cadastro (`CustomerRegistered`) e um evento de troca de telefone carregam o telefone; a versão segue o padrão dos eventos.
  - Termos novos no `CONTEXT.md`: **Telefone** e **Verificação do telefone**.
- **BFF:** junta a busca por telefone com a conta não encerrada do cliente (cada cliente tem uma só) para montar o destino da transferência.
- **Cognito (`modules/identity`):**
  - política de senha com mínimo de 6 e sem exigência de classes;
  - `ALLOW_USER_PASSWORD_AUTH` no app client web;
  - autocadastro permitido, com verificação por e-mail.
- **Seed:** cria o usuário de demonstração, com transferências, uma recusada pelo limite e notificações, local e no `aws-up`. O e-mail e a senha ficam nas variáveis do seed. Na AWS, a senha sai de um segredo (`secrets`), nunca versionada.
- **Deploy:**
  - duas imagens novas (`web`, `bff`) nas convenções de imagem da etapa 8;
  - charts com o chart comum;
  - regras `/` e `/bff` no ALB e no nginx local, conferidas pelo `check-alb-routes.py`;
  - o BFF usa o Redis já provisionado, com prefixo de chave próprio.
- **CI:** job novo para `frontend/` com lint, unidade, build e Playwright contra o compose, e `paths-filter`.

### 6.5 Testes
- Unidade no Angular e no NestJS (ferramenta padrão de cada um, escolha do builder).
- Playwright no fluxo principal:
  1. criar conta;
  2. completar o cadastro;
  3. abrir conta e ver o bônus de R$ 100;
  4. transferir para o usuário de demonstração;
  5. ver `COMPLETED`, o extrato e a notificação;
  6. tentar uma transferência acima do limite e ver a recusa.
- Mutantes no padrão do projeto: senha com 5 ou 7 dígitos é recusada no BFF; o bônus é lançado uma vez só; telefone não verificado não recebe transferência; dois clientes não ficam com o mesmo telefone.
- O Playwright também cobre a verificação do telefone (código do aviso simulado) e a transferência pelo telefone do usuário de demonstração.

### 6.6 Pontos fechados na leitura do builder (decisões da coordenadora)
- **Cliente já nasce Ativo:** `POST /customers` cria o cliente com `CustomerStatus.ACTIVE` (`Customer.kt`); o cadastro não precisa de rota de admin.
- **HTTPS na AWS:** o app entra pela borda que já existe, o API Gateway HTTP API, com o domínio padrão `execute-api` em HTTPS e as rotas `/` e `/bff` adicionadas aos prefixos do módulo `edge`; sem domínio próprio. O cookie `Secure` vale na AWS; localmente, sobre `http://127.0.0.1`, o BFF desliga o `Secure` por configuração.
- **E-mail do Cognito:** fica o e-mail padrão do Cognito, com limite diário baixo, suficiente para uma demonstração; sem SES. O limite fica registrado no README.
- **App client:** um **app client novo** para o BFF, sem segredo e com `ALLOW_USER_PASSWORD_AUTH` e `ALLOW_REFRESH_TOKEN_AUTH`; o client `web` atual não muda.
- **Testes que assumem saldo zero:** a task do bônus ajusta todos os testes que assumem conta nova com saldo zero: e2e, smoke, Bruno, contratos de mensagem, `statement` e `notification`.

## 7. Restrições
- Código, testes e logs em inglês; textos da interface em português.
- Valores em centavos no backend. No front, a formatação em reais é só de exibição.
- O BFF nunca guarda token no navegador.
- Nada de segredo versionado.
- O `domain` do accounts segue sem Spring.
- ADR novo para a senha numérica de 6 dígitos, o login pela API do Cognito, o telefone como chave de transferência e a verificação simulada.

## 8. Deixado ao builder
- As versões exatas de Angular, NestJS e Playwright, conferidas na fonte primária.
- A ferramenta de unidade de cada parte.
- O desenho da porta de identidade e do adaptador local.
- O intervalo e o prazo do polling.
- Os nomes dos endpoints do BFF.
- A paginação do extrato na tela.

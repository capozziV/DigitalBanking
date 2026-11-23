# 0024 — Login e telefone do app

- **Status:** aceito
- **Data:** 2026-11-19

## Contexto
O app do cliente (etapa 9) precisa de cadastro e login pelo próprio recrutador, em 1 a 2 minutos, e de um jeito de achar o destino de uma transferência. O banco é simulado: não há dinheiro nem dados pessoais reais. O [ADR 0005](0005-frontend-angular-e-bff-nestjs.md) previa login OAuth2 com o Cognito; o [ADR 0020](0020-identidade-e-autorizacao.md) define o `sub` como id do cliente.

## Decisão
- **Senha numérica de exatamente 6 dígitos.** É fraca de propósito: o banco é simulado e uma senha curta deixa o cadastro rápido. Validada na tela e no BFF. O bloqueio por tentativas erradas do Cognito fica ligado, e a política do Cognito pede só o mínimo de 6, sem classes de caractere.
- **Login e cadastro em telas próprias.** O BFF fala com o Cognito pela API (`SignUp`, `ConfirmSignUp`, `InitiateAuth` com `USER_PASSWORD_AUTH` e refresh), sem hosted UI. Um app client novo, só do BFF e sem segredo; o client `web` não muda. O token nunca chega ao navegador: fica na sessão do BFF, no Redis, atrás de cookie `HttpOnly`. Localmente, o `mock-oauth2-server` não tem cadastro; um adaptador local aceita o cadastro, pula o código por e-mail e emite o token pelo mock.
- **Telefone celular, sem verificação, como chave de transferência.** Formato brasileiro validado, único entre os clientes, trocável em "Minha conta". A busca por telefone devolve o nome completo do dono, que o cliente confere antes de enviar. Ela revela que o número tem conta e o nome completo, mais do que o PIX mostra. Como não há verificação, qualquer um pode cadastrar um número que não é seu. As duas coisas são aceitáveis só porque o banco é simulado.

## Consequências
- Positivas: cadastro e primeira transferência em poucos minutos; tela de login com a cara do app; token fora do navegador; transferência por um dado que o cliente conhece, com confirmação do nome contra erro de digitação.
- Negativas: senha de 6 dígitos tem só 1 milhão de combinações (o bloqueio do Cognito é a única defesa); `USER_PASSWORD_AUTH` manda a senha ao BFF, que passa a tratá-la; dois adaptadores de identidade para manter (Cognito e local), e o local não exercita o `SignUp` real; o e-mail do Cognito padrão tem limite diário baixo (sem SES), suficiente só para demonstração; busca por telefone permite enumerar números e nomes; número alheio pode ser cadastrado antes do dono. Nada disso serve a um banco real.

## Alternativas consideradas
- **Hosted UI do Cognito com OAuth2 (`openid-client`):** menos código de identidade e senha fora do BFF, mas telas fora do app e do visual, e o mock local não a reproduz.
- **Senha forte (política padrão do Cognito):** mais segura, mas atrasa o cadastro e não protege nada real.
- **Verificar o telefone por SMS (real ou simulado):** fecha o cadastro de número alheio, mas exige SMS pago ou um simulador que não prova nada; o usuário decidiu não verificar.
- **Chave por e-mail ou id da conta:** evita revelar telefone, mas o cliente não sabe o id da conta de quem recebe, e o e-mail é mais exposto no cadastro.

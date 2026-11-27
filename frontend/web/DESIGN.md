---
name: capzzPay
description: Banco digital simulado na direção visual Noite Apple — preto puro, luz difusa, vidro e o cartão flutuante.
colors:
  night: '#000000'
  moonlight: '#f5f5f7'
  moonlight-muted: 'rgb(245 245 247 / 0.64)'
  hairline: 'rgb(255 255 255 / 0.12)'
  glass: 'rgb(255 255 255 / 0.06)'
  glass-strong: 'rgb(255 255 255 / 0.1)'
  raised: '#16161a'
  light-blue: '#6aa8ff'
  light-lilac: '#b18cff'
  light-pink: '#ff7ab6'
  positive-mint: '#5ee6a8'
  negative-rose: '#ff8a9a'
  alert-red: '#e0164a'
  glow-blue: 'rgb(59 59 255 / 0.5)'
  glow-lilac: 'rgb(193 59 255 / 0.38)'
  card-chip-light: '#f4e2a6'
  card-chip-deep: '#b8954a'
  card-free-light: '#3a3f4a'
  card-free-deep: '#1c1f26'
  card-standard-light: '#2f5fb8'
  card-standard-deep: '#101c44'
  card-standard-edge: 'rgb(160 196 255 / 0.28)'
  card-gold-light: '#ecd394'
  card-gold-mid: '#b48d45'
  card-gold-deep: '#7d5f25'
  card-gold-ink: '#1b1407'
  card-black-light: '#2c2c31'
  card-black-deep: '#0b0b0e'
  card-diamond-ice: '#d9f2ff'
  card-diamond-blue: '#8fb8ff'
  card-diamond-lilac: '#c9a8ff'
  card-diamond-ink: '#0b1230'
typography:
  display:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Segoe UI Variable Display', 'Segoe UI', Roboto, 'Helvetica Neue', system-ui, sans-serif"
    fontSize: '2.25rem'
    fontWeight: 700
    lineHeight: 1.05
    letterSpacing: '-0.035em'
  headline:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Segoe UI Variable Display', 'Segoe UI', Roboto, 'Helvetica Neue', system-ui, sans-serif"
    fontSize: '1.375rem'
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: '-0.03em'
  body:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'SF Pro Text', 'SF Pro Display', 'Segoe UI Variable Text', 'Segoe UI', Roboto, 'Helvetica Neue', system-ui, sans-serif"
    fontSize: '0.875rem'
    fontWeight: 400
    lineHeight: 1.45
  label:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'SF Pro Text', 'SF Pro Display', 'Segoe UI Variable Text', 'Segoe UI', Roboto, 'Helvetica Neue', system-ui, sans-serif"
    fontSize: '0.6875rem'
    fontWeight: 500
    lineHeight: 1
    letterSpacing: '-0.01em'
rounded:
  sm: '8.4px'
  md: '11.2px'
  lg: '14px'
  xl: '18.2px'
  2xl: '22.4px'
  pill: '9999px'
spacing:
  gutter-phone: '20px'
  gutter-desktop: '40px'
  sidebar: '240px'
  tabbar-height: '64px'
components:
  nav-link:
    textColor: '{colors.moonlight-muted}'
    rounded: '{rounded.xl}'
    padding: '10px 12px'
  nav-link-active:
    backgroundColor: '{colors.glass-strong}'
    textColor: '{colors.moonlight}'
    rounded: '{rounded.xl}'
    padding: '10px 12px'
  glass-panel:
    backgroundColor: '{colors.glass}'
    textColor: '{colors.moonlight}'
    rounded: '{rounded.2xl}'
  bell-badge:
    backgroundColor: '{colors.alert-red}'
    textColor: '{colors.moonlight}'
    rounded: '{rounded.pill}'
    typography: '{typography.label}'
  button-primary:
    backgroundColor: '{colors.moonlight}'
    textColor: '{colors.night}'
    rounded: '{rounded.lg}'
    height: '46px'
  popover:
    backgroundColor: '{colors.raised}'
    textColor: '{colors.moonlight}'
    rounded: '{rounded.2xl}'
    padding: '16px'
---

# Design System: capzzPay

## Overview

**Estrela-guia: Noite Apple.** O lado escuro de apple.com, escolhido pelo usuário entre seis mockups: um campo preto puro, iluminado por luzes de cor difusas que se movem devagar, com o conteúdo flutuando em vidro fosco. A interface recua; a luz e o cartão falam.

É uma superfície de operar (saldo, extrato, transferência), então a ambição aparece como precisão: tipografia apertada, uma hierarquia por tela e um único momento autoral, o cartão. O movimento serve à ação e desliga inteiro com "reduzir movimento".

Antirreferência confirmada na spec: cara de site genérico de IA (Inter em tudo, gradiente roxo-azul sem propósito, card dentro de card, ícone em quadradinho sobre cada título).

## Colors

Estratégia contida: neutros de noite mais três luzes. As luzes aparecem como luz (brilho difuso, gradiente do título, acabamento do cartão), nunca como preenchimento chapado de botão ou fundo.

### Primary

- **Moonlight** (moonlight): todo o texto principal e o botão primário (texto preto sobre ele). É o contraste máximo do sistema.

### Secondary

- **Luz azul, lilás e rosa** (light-blue, light-lilac, light-pink): só nas luzes de fundo, no trecho de destaque do título e no anel de foco (lilás). O lilás é o acento de interação. As luzes de fundo usam versões mais saturadas e translúcidas (glow-blue, glow-lilac).

### Tertiary

- **Menta e rosa** (positive-mint, negative-rose): crédito e débito, valor positivo e negativo, sucesso e erro. **Vermelho de alerta** (alert-red): só no contador do sininho, com contraste AA para o número branco.

### Neutral

- **Night** (night): o fundo de tudo, preto puro.
- **Glass / Glass strong** (glass, glass-strong): o material dos painéis e do estado ativo.
- **Hairline** (hairline): a borda fina de vidro e as divisórias.
- **Moonlight muted** (moonlight-muted): rótulos e texto secundário, 7:1 sobre o preto.
- **Raised** (raised): superfícies sobrepostas opacas (popover, menu).

### Named Rules

**A Regra da Luz.** Cor só entra como luz. Se um bloco ficou colorido por inteiro, está errado.
**A Regra do Contador.** O número de não lidas aparece num lugar só: o sininho.

## Typography

Pilha de sistema: SF Pro no Apple (Display nos títulos, Text no corpo), Segoe UI Variable no Windows e **Roboto como a alternativa livre** (Apache 2.0, padrão do Android). Inter não entra em nenhuma pilha (há teste conferindo).

### Hierarchy

- **Display** (2.25rem, 700, -0.035em, 1.05): saudação e títulos de tela; o trecho de destaque usa o gradiente das luzes.
- **Headline** (1.375rem, 700, -0.03em): títulos de seção e passos.
- **Body** (0.875rem, 400, 1.45): texto corrido, linhas de lançamento.
- **Label** (0.6875rem, 500): rótulos das abas e contadores. 11 px é o piso de texto funcional.
- Valores em dinheiro usam algarismos tabulares e peso 600–650.

### Named Rules

**A Regra do Piso.** Nenhum texto funcional abaixo de 11 px, nem dentro do cartão.

## Layout

- **Celular (< 1024 px):** barra superior fina (marca e sininho), conteúdo com respiro de 20 px e uma barra de abas de vidro flutuando acima do indicador de início (64 px de altura, 12 px das bordas, respeitando a área segura).
- **Computador (≥ 1024 px):** barra lateral fixa de 240 px (marca, sininho, cinco destinos, Sair embaixo) e uma coluna de conteúdo de até 70 rem com respiro de 40 px.
- Rodapé "© 2026 capzz.labs" no fim da coluna de conteúdo.
- Cinco destinos, na mesma ordem nos dois formatos: Início, Extrato, Transferir, Notificações, Minha conta.

## Elevation & Depth

Profundidade vem de luz e translucidez, não de sombra empilhada. O vidro (6% de branco, borda fina, desfoque de 18 px com saturação) separa os painéis do campo preto; o estado ativo sobe para 10%. A única sombra marcada é a do cartão flutuante: deslocada para baixo e suave, para ele parecer solto do fundo.

### Shadow Vocabulary

- **Float** (`0 24px 48px -16px rgb(0 0 0 / 0.7), 0 8px 16px -8px rgb(0 0 0 / 0.5)`): só no cartão.

### Named Rules

**A Regra do Vidro Único.** Vidro nunca dentro de vidro. Um painel é a camada; o que está dentro dele é conteúdo.

## Shapes

Cantos generosos e contínuos: 14 px em botões e campos, 18 px em itens de navegação, 22 px em painéis, pílula em contadores e no indicador da aba ativa. O cartão tem a proporção de um cartão de verdade (1,586) e raio proporcional à largura.

## Components

### Buttons

Primário: moonlight com texto preto, 46 px de altura, raio de 14 px. Secundário: vidro com borda fina. Foco visível com anel lilás de 2 px afastado 2 px.

### Cards / Containers

**GlassPanel** é o contêiner padrão (saldo, lançamentos, notificações). Nunca aninhado.

### Navigation

- **Barra lateral:** link inativo em texto atenuado; ativo com vidro forte e borda fina completa (sem barra lateral colorida). `aria-current="page"` marca o destino atual.
- **Barra de abas:** ícone com rótulo de 11 px; a aba atual ganha uma pílula de vidro atrás do ícone, não só a cor.
- **Sininho:** botão de 44 px com o contador (até "99+") e um painel de vidro opaco com as recentes e "Ver todas".

### Account Card (assinatura)

Cartão 3D inclinado que flutua (6,5 s) com um brilho que atravessa a cada 5,5 s. O acabamento segue o tipo de conta: Free grafite, Standard azul-noite, Gold ouro escovado, Black preto com borda clara, Diamond gelo azul-lilás (cores card-* da paleta, usadas só no cartão). Mostra marca, tipo, chip e, quando houver, "•••• 1234". Nome acessível "Cartão {Tipo}". Tamanhos sm (9,5 rem, só tipo e chip), md (18 rem), lg (22 rem). Para em `prefers-reduced-motion` ou com `still`.

### Motion

- **Entrada de tela:** opacidade e 10 px de subida, 0,42 s, ease-out exponencial (`cubic-bezier(0.16, 1, 0.3, 1)`).
- **Revelar item:** opacidade e 14 px, 0,6 s, com atraso para escalonar um grupo.
- **Contagem do saldo:** 0,9 s em centavos inteiros, terminando sempre no valor exato.
- **Laços** (luzes 22–30 s, cartão, brilho, gradiente do título por cruzamento de opacidade): só `transform` e `opacity`.
- **Reduzir movimento:** luzes e cartão param, transições viram corte seco, a contagem mostra o valor final.

## Do's and Don'ts

### Do:

- **Do** deixar o cartão ser o único momento autoral de cada tela.
- **Do** usar o gradiente das luzes só no trecho de destaque de um título.
- **Do** marcar estados com forma (vidro, borda, pílula), não só com cor.
- **Do** manter todo laço de animação em `transform`/`opacity` e desligá-lo com "reduzir movimento".

### Don't:

- **Don't** usar Inter, nem como alternativa.
- **Don't** pôr vidro dentro de vidro, nem ícone em quadradinho sobre título.
- **Don't** repetir o contador de não lidas fora do sininho.
- **Don't** usar barra colorida na lateral de um item ativo.
- **Don't** pôr texto funcional abaixo de 11 px.

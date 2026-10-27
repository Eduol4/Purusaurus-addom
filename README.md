# Purusaurus Addon

Addon para o mod **Unearthed Journey** (Minecraft **1.20.1**, **Forge**). Adiciona o
**Ovo de Purusaurus**, um bloco no estilo do ovo de tartaruga que choca um
**Purusaurus bebê**.

Este é um mod **separado e independente**: ele **não** contém nem redistribui o
código ou os assets do Unearthed Journey. Ele apenas adiciona conteúdo próprio e,
na hora de chocar, procura a entidade do Purusaurus pelo ID de registro
`unearthed_journey:purussaurus_brasiliensis`. Por isso o Unearthed Journey
**precisa estar instalado** junto.

---

## O que já existe no mod base (importante)

Durante a análise do `.jar` confirmei que **o Purusaurus já tem bebê**. A entidade
herda de `Animal`, então já possui todo o sistema de reprodução (acasala comendo
carne crua, gera filhote, o filhote segue os pais). E existe um procedimento de
escala que o renderizador aplica: **bebês são o próprio modelo adulto renderizado a
0.5x de tamanho**. Ou seja, não foi preciso criar modelo de bebê — o ovo só gera o
filhote com `setBaby(true)` e o jogo cuida do resto (inclusive de crescer sozinho).

---

## O que este addon faz (fase 1 — o ovo)

- **Bloco "Ovo de Purusaurus"** espelhado no ovo de tartaruga vanilla, com as
  diferenças pedidas:
  - **1 ovo por bloco** (não tem a propriedade de 1–4 ovos empilhados; cada ovo é
    um bloco inteiro, logo nasce **1 Purusaurus por ovo**).
  - **Choca em ~1 dia/noite** (24000 ticks), de forma precisa — ver "Ajustes".
  - Mostra **estágios visuais de rachadura** (inteiro → rachado → muito rachado).
  - Ao chocar, nasce **1 Purusaurus bebê** no lugar do ovo.
  - **Pisoteio** opcional igual ao da tartaruga (pode ser desligado — ver "Ajustes").
    O próprio Purusaurus não pisoteia os próprios ovos.
- Textura amarelada estilo casca de ovo e **modelo um pouco maior** que o de tartaruga.
- O ovo aparece numa aba criativa própria ("Purusaurus Addon") e pode ser quebrado
  para recuperar o item.

> **Como conseguir o ovo no jogo hoje:** ele está na aba criativa. Ainda **não** há
> receita de craft nem o Purusaurus adulto "botando" ovos — isso é uma decisão em
> aberto (ver "Próximos passos").

---

## Comportamento e domesticação (fase 2)

Tudo feito via eventos do Forge, **sem mixins** e sem alterar o mod base.

- **Bebês e domesticados são NEUTROS.** No mod base, o Purusaurus (inclusive bebê)
  mira e ataca o jogador. Este addon torna **bebês** (selvagens e domesticados) e
  **adultos domesticados** neutros: não escolhem o jogador como alvo sozinhos, mas
  **revidam** se forem atacados. O domesticado **nunca revida contra o próprio dono**
  (revida contra outros jogadores, sim). Adultos selvagens continuam hostis como antes.
- **Ataque assistido.** O Purusaurus domesticado ataca **qualquer alvo que o dono
  atacar** (como o lobo), com exceção de **outros animais domesticados** (pets vanilla
  e outros Purusaurus domados).
- **Regeneração natural.** O domesticado recupera **1 HP a cada 5 segundos**.
- **Som ao alimentar.** Alimentar com as carnes (seja domesticando um bebê ou curando
  um domesticado) toca um som de comer em tom grave junto com a animação de mordida.

### Reprodução e obtenção do ovo

- **Reprodução:** alimente dois Purusaurus adultos com o item **`raw_meat`** do mod base
  (o alimento de acasalamento da espécie). Em vez de nascer um bebê, é posto um
  **Ovo de Purusaurus no chão**, que choca normalmente.
- **Drop selvagem:** um Purusaurus **adulto selvagem** morto pelo jogador tem **1,5%**
  de chance de dropar o **ovo como item** (pegável), que o jogador pode colocar onde quiser.
- **Bebês têm metade dos atributos.** Bebês (selvagens e domesticados) têm metade do
  **HP** (100 em vez de 200) e metade do **dano** (6 em vez de 12) dos adultos, via um
  modificador de atributo aplicado enquanto são bebês e removido ao crescerem.
- **Dano variável (adultos).** Cada ataque de um Purusaurus adulto (selvagem ou
  domesticado) tem 10% de chance de causar **15** de dano e 5% de causar **18**, em vez
  dos 12 normais. Não vale para bebês.
### Natação

Vale para selvagens e domesticados.

- **Não se afoga** (oxigênio sempre cheio, dano de afogamento cancelado).
- **Nada em 3D de verdade:** enquanto está na água, um controlador assume o movimento e
  o faz **mergulhar e emergir** indo atrás do seu destino, em vez de só boiar. Isso é
  feito "pilotando" o mob na água — o mod base não é alterado.
- **Dois modos:** nado **lento** (modificador 0.50) e **rápido** (1.00). Ele nada rápido
  ao perseguir um alvo ou seguir um dono que esteja correndo/longe; senão, nada devagar.
  (As velocidades são constantes em `PurusaurusSwimController` e fáceis de ajustar.)
- **Entra na água** para perseguir alvos (que o atacaram/provocaram, ou que o dono atacou)
  e para seguir o dono nadando. Vagar autônomo dentro d'água acontece de forma leve
  quando a navegação entra na água; o foco é perseguir/seguir.
- **Animação:** usa automaticamente a animação de nadar já existente enquanto está na água.
- **Domesticação (só em bebês):** clique com o botão direito no bebê segurando um
  destes itens. Cada um tem sua taxa de sucesso:

  | Item | Taxa |
  |------|------|
  | Carne de vaca (crua) | 30% |
  | Carne de porco (crua) | 25% |
  | Carne de ovelha (crua) | 20% |
  | Carne de coelho (crua) | 15% |
  | Carne de frango (crua) | 10% |
  | Salmão (cru) | 5% |
  | Bacalhau (cru) | 2,5% |
  | Peixe tropical | 1,5% |
  | Carne podre | 1% (só domestica, **não** cura) |

  O item é consumido a cada tentativa (exceto no criativo). **Sucesso** → partículas
  de **coração**; **falha** → partículas de **fumaça** (igual ao lobo).
- **Depois de domesticado**, o Purusaurus fica **completamente passivo ao jogador** e
  **continua passivo quando vira adulto**. O estado de domesticado é salvo na própria
  entidade (persiste ao fechar/abrir o mundo).

> Adultos **não** são domesticáveis — só bebês. Para ter um Purusaurus domesticado,
> consiga um ovo, espere chocar e domestique o filhote.

### Cuidar do Purusaurus domesticado (fase 3)

- **Itens de domesticação não re-domesticam** um Purusaurus já domado.
- **Curar:** com o pet ferido, clique com o botão direito usando um dos itens de
  domesticação. Cada um cura uma quantia (e consome 1 item, exceto no criativo), e
  ele reaproveita a animação de **mordida** como animação de comer:

  | Item | Cura (pontos) |
  |------|---------------|
  | Carne de vaca | 25 |
  | Carne de porco | 24 |
  | Carne de ovelha | 23 |
  | Carne de coelho | 18 |
  | Carne de frango | 17 |
  | Salmão | 8 |
  | Bacalhau | 7 |
  | Peixe tropical | 6 |

- **Modos (Shift + clique direito, com qualquer item):** alterna entre
  **Vagar** (anda sozinho como os selvagens), **Seguir** e **Ficar** (fica parado).
  Uma mensagem na barra de ação (acima da hotbar) mostra o modo atual.
- **Seguir:** segue o dono como o lobo. Anda a 0.39 e **corre** (animação de corrida)
  a 0.77 quando o dono está correndo ou muito longe. Distâncias maiores que as do lobo:
  começa a seguir a 12 blocos, corre a partir de 15 e **teletransporta** a 26 (lobo:
  segue a 10, teleporta a 12).

> Cura e modos funcionam só em Purusaurus **domesticado**. As IAs de Seguir/Ficar são
> injetadas na entidade via um *Access Transformer* (`META-INF/accesstransformer.cfg`),
> que torna o `goalSelector` acessível — por isso o `build.gradle` referencia esse arquivo.

---

## Como gerar o `.jar` pelo GitHub Actions (recomendado)

Você **não precisa instalar nada** no seu PC. O GitHub compila pra você.

1. Crie uma conta no [github.com](https://github.com) (se ainda não tiver).
2. Crie um **repositório novo** (pode ser privado). Ex.: `purusaurus-addon`.
3. Suba **todos os arquivos desta pasta** para o repositório (incluindo a pasta
   oculta `.github/`). Duas formas:
   - **Pelo site:** botão *Add file → Upload files* e arraste tudo. (O upload pelo
     site às vezes ignora pastas ocultas; se a aba *Actions* não mostrar nada,
     crie o arquivo `.github/workflows/build.yml` manualmente pelo *Add file →
     Create new file* colando o conteúdo.)
   - **Pelo Git:** `git init`, `git add .`, `git commit`, `git push`.
4. Vá na aba **Actions** do repositório. O workflow **"Build mod jar"** roda sozinho
   a cada push; se quiser, clique nele e use **Run workflow** para rodar na hora.
5. Quando terminar (ícone verde), abra a execução e baixe o artefato
   **`purusaurus-addon-jar`** lá embaixo, em *Artifacts*. Dentro dele está o
   `purusaurusaddon-forge-1.20.1-0.1.0.jar`.

Esse `.jar` é o mod pronto.

---

## Como instalar no jogo

Na pasta `mods/` do seu perfil Forge 1.20.1, coloque **os três**:

1. `purusaurusaddon-forge-1.20.1-0.1.0.jar` (este addon)
2. O `.jar` do **Unearthed Journey 2.2.3**
3. O `.jar` do **GeckoLib** para 1.20.1 (dependência do Unearthed Journey)

---

## Compilar localmente (opcional)

Precisa de **JDK 17**. Na pasta do projeto:

```bash
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows
```

O jar sai em `build/libs/`. A primeira compilação baixa o Forge e o Minecraft
(demora alguns minutos).

---

## Ajustes rápidos (onde mexer)

- **Tempo de choco:** `HATCH_TIME` em
  `src/main/java/com/purusaurusaddon/block/entity/PurusaurusEggBlockEntity.java`
  (24000 = 1 dia/noite).
- **Ligar/desligar pisoteio:** `ALLOW_TRAMPLE` em
  `src/main/java/com/purusaurusaddon/block/PurusaurusEggBlock.java`.
- **Tamanho do modelo/hitbox no chão:** constante `SHAPE` no mesmo arquivo, e a
  geometria em `src/main/resources/assets/purusaurusaddon/models/block/*.json`.
- **Texturas:** `src/main/resources/assets/purusaurusaddon/textures/block/`. As
  atuais são feitas do zero (estilo casca amarelada). Se quiser o visual exato do
  ovo de tartaruga, é só substituir pelos PNGs da tartaruga recoloridos de amarelo,
  mantendo os mesmos nomes de arquivo.

---

## Próximos passos (fases seguintes)

- **Como obter o ovo:** definir se vem de craft, de drop, ou do Purusaurus adulto
  "botando" ovo depois de acasalar (estilo tartaruga). As duas últimas opções mexem
  na entidade do mod base e exigem *mixins* (ver abaixo).
- **Domesticável (estilo lobo) e montável (estilo cavalo):** isso **não** dá para
  fazer só com um addon limpo, porque o comportamento mora na classe já compilada
  `PurussaurusBrasiliensisEntity` (que herda de `Animal`, não de `TamableAnimal`
  nem de cavalo). O caminho é usar **Mixins** (coremod Forge) para injetar na
  entidade: interação de domar, dados de dono/sela, e controle de montaria. É mais
  avançado e será uma fase à parte.

---

## Licença / observação

O Unearthed Journey é distribuído como "All Rights Reserved". Por isso este projeto
foi feito como **addon separado**, que **não** redistribui nada do mod original —
ele apenas depende dele em tempo de execução. Se a ideia for publicar o addon,
vale avisar o autor do mod original (Traut) por cortesia.

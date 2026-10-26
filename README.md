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

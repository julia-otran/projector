Projector
=========

Software para projeção de letras em telões.

Sinta-se a vontade para abrir um `pull request`e contribuir conosco.

Build
=====
- Nós usamos o maven para gerar a build.
- Utilizar Java 13

Basta executar o seguinte comando:

```
mvn package
```

O jar para windows será gerado em `windows/target/projector-windows-x.x-jar-with-dependencies.jar`
Do mesmo modo para as demais plataformas.

Instalação
==========

## Windows

1. Instalar o Java 13
2. Instalar o VLC Player (versão 3.x)
   ### Atenção
   - Se você instalou o JDK 64 bits, tem que instalar o VLC 64bits. Fique atento: Por padrão o site do VLC disponibiliza a versão 32bits, que não irá funcionar com o java x64
   - Instalar o VLC no diretório C:\VLC e não dentro da pasta Program Files (Arquivos de Programas)
3. Definir uma variável de ambiente `VLC_PLUGIN_PATH` com valor `C:\VLC\plugins`


Configração de Telas
====================

Você pode criar novos presets, eles ficam salvos em `~/Projector/Window Configs/*.json`

## Exemplo de um preset com todas as features disponíveis:


```
[
  {
    "displayId": ":0.1",
    "virtualScreenId": "main",
    "displayBounds": {
      "x": 0,
      "y": 0,
      "width": 1920,
      "height": 1080
    },
    "project": true,
    "x": 0,
    "y": 0,
    "width": 1920,
    "height": 1080,
    "helpLines": [{ "X1": 0, "X2": 1920, "Y1": 0, "Y2": 0, "lineWidth": 3.0 }],
    "blends": [{ "x": 0, "y": 0, "width": 1920, "height": 200, "direction": 2, "id": 1, "useCurve": true }],
    "blackLevelAdjust": {
      "points": [{ "x": 0, "y": 20 }, { "x": 1000, "y": 100 }, { "x": 600, "y": 600 }, { "x": 100, "y": 500 }],
      "offset": 100
    },
    "whiteBalance": {
      "bright": {
        "r": 0,
        "g": 0,
        "b": 0
      },
      "exposure": {
        "r": 1.0,
        "g": 1.0,
        "b": 1.0
      }
    },
    "colorBalance": {
      "shadows": {
        "r": 0,
        "g": 0,
        "b": 0
      },
      "midtones": {
        "r": 0,
        "g": 0,
        "b": 0
      },
      "highlights": {
        "r": 0,
        "g": 0,
        "b": 0
      },
      "preserveLuminosity": true
    }
  }
]

```

1. displayId: Usado internamente para identificar a tela, *não altere esse valor*
2. virtualScreenId: id da tela virtual:
  Telas virtuais podem ter mais de um monitor, nesse caso, o tamanho da tela será `max(x + width)` e `max(y + height)`
  É possível ter mais de uma tela virtual, e isso é útil quando precisa-se renderizar saídas com tamanhos diferentes.
  Outra solução, é ter uma tela virtual `chroma` que tem um fundo verde para ser usada com um chroma-key para legenda de vídeos.
  Valores possíveis:

  a. main: Tela principal
  b. chroma: Tela com fundo verde
  c. main-chroma: Usar quando há apenas uma tela principal e esta é uma tela `chroma`
  d. qualquer outro valor: cria uma tela virtual com o id nomeado.

3. displayBounds:
  Usado para identificar o monitor, assim como `displayId`. Internamente, os `displayId` podem mudar, e a identificação dos monitores pode não funcionar. Com isso, identificamos o monitor atraves das coordenadas na tela e sua resolução.
  *Não altere esses valores, a menos que a coordenada de um dos monitores mude, ou a resolução mude*

4. project: Se deve ser projetada imagem nessa tela

5. `x` e `y`: Posição da tela virtual que deve ser exibida nessa tela.
  Supondo que você possua dois projetores em um telão, ambos com resolução 1280x720 no seguinte formato:
  +----------------+----------------+
  | Projetor 1     | Projetor 2     |
  +----------------+----------------+
  Você deve usar x = 0 e y = 0 no projetor 1; e x = 1280 e y = 0 no projetor 2.

  Dessa forma, será gerada uma tela virtual com tamanho 2560x720 e ela será dividida em 2 nos projetores.

  Considere também, que você pode usar um `x` menor que 1280 no projetor 2 e ter uma área de `blending` entre os projetores. Exemplo:

  +-------+-------+-------+
  | #1    | #1 #2 | #2    |
  +-------+-------+-------+

  Nesse caso, supondo que sua área de blending seja de 100 pixels, você deve usar:

  #1: x = 0; y = 0; #2 x = 1180; y = 0;

  *Dica: use as linhas de guia para verificar o tamanho da área de blending, quando configurando pela primeira vez*

6. `width` e `height`: São usados para calcular o tamanho da tela virtual. Se usar valores menores ou maiores, será aumentado ou reduzido na projeção para o monitor.
  *Imagino que você não precise alterar esse valor*
  *Esses valores podem ser úteis caso você precise projetar um parte menor da tela virtual em outro monitor diferente*

7. `helpLines`: Você pode criar linhas para auxiliar no alinhamento dos projetores.
  *Dica: crie linhas em cima, em baixo, à esquerda, à direita*
  a. Descobrindo o tamanho da área de blending:
    1. No projetor #1 crie uma linha à direita (x1 = 1280, x2 = 1280, y1 = 0, y2 = 720)
    2. No projetor #2 crie uma linha à esquerda (x1 = 0, x2 = 0, y1 = 0, y2 = 720)
    3. Na linha criada no passo 2, aumente os valores de x1 e x2 até que a linha encontre a do projetor #1
    4. O valor de x1 ou x2 será o tamanho da área de blending.
    *Certifique-se de que o keystone dos projetores esteja correto, do contrário as linhas podem ficar cruzadas. Se isso ocorrer, ajuste o keystone para que elas fiquem paralelas*

  b. Alinhando keystone com múltiplos projetores:
    1. No projetor #1 crie uma linha à direita (x1 = 1280, x2 = 1280, y1 = 0, y2 = 720)
    2. No projetor #1 crie outra linha à direita (dado uma área de blend de 100px -> x1=1180, x2=1180 y1=0, y2=720)
    3. No projetor #2 crie uma linha à esquerda (x1 = 0, x2 = 0, y1 = 0, y2 = 720)
    4. No projetor #2 crie outra linha à esquerda (dado uma área de blend de 100px -> x1=100, x2=100, y1=0, y2=720)
    Alinhe os projetores de forma que as 4 linhas pareçam 2, ou seja a linha do passo 1 coincidindo com a do passo 4 e
    a linha do passo 2 coincidindo com a linha do passo 3

  *Dica: se a linha não aparecer, você pode aumentar o `lineWidth`*

8. `blends`:
  Quando projetando com 2 projetores juntos, você deve usar esses `blends` na área que os projetores se encontram, para que as telas não fiquem com uma faixa mais clara no ponto de junção.
  A ideia é criar um degradê de claro a escuro no projetor 1 e de escuro para claro no projetor 2.

  *dada uma área de blend de 100px*

  1. No projetor 1, crie um `blend` com x=1180, y=0, width=100, height=720, direction=0
  2. No projetor 2, crie um `blend` com x=0, y=0, width=100, height=720, direction=1

9. `blackLevelAdjust`: Se você usa dois projetores, com a tela preta o meio deve ficar mais claro que as pontas.
  Crie um quadrilátero (ou outra forma com mais pontos se necessário) em cada um dos dois projetores e ajuste a forma e o offset (quanto mais clara a forma deve ficar) para ter um preto uniforme.

10. `whiteBalance`: Ajuste o branco (ou se desejar, o brilho e a exposição, colocando o mesmo valor para `R` `G` e `B`)
do projetor.
  *Útil quando há mais de um projetor e o branco está diferente entre cada um deles*

11. `colorBalance`: Balanço de cor do projetor.
  *Útil quando há mais de um projetor, e a cor está diferente entre cada um deles`

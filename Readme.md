Projector
=========

Software para projeção de letras em telões.

Esta versão ainda é um W.I.P.

Sinta-se a vontade para abrir um `pull request`e contribuir conosco.

Build
=====
- Nós usamos o maven para gerar a build.
- Utilizar Java 8

Basta executar o seguinte comando:

```
mvn package
```

O jar será gerado em `target/projector-x.x-jar-with-dependencies.jar`

Instalação
==========

## Windows

1. Instalar o Java 8 (JDK 1.8.x)
2. Instalar o VLC Player (versão 2.x)
   ### Atenção
   - Se você instalou o JDK 64 bits, tem que instalar o VLC 64bits. Por padrão o VLC retorna a versão 32bits, que não irá funcionar com o java x64
   - Instalar o VLC no diretório C:\VLC e não dentro da pasta Program Files (Arquivos de Programas)
3. Definir uma variável de ambiente `VLC_PLUGIN_PATH` com valor `C:\VLC\plugins`


Configração de Telas
====================

Ao iniciar pela primeira vez o Projector, ele cria um arquivo em `~/Projector/window-configs.json`

Lembre-se de excluir e fazer um backup toda vez que você alterar a configuração das telas. (ou trocar de tela)

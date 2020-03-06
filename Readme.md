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
2. Instalar o VLC Player (versão 2.x)
   ### Atenção
   - Se você instalou o JDK 64 bits, tem que instalar o VLC 64bits. Por padrão o VLC retorna a versão 32bits, que não irá funcionar com o java x64
   - Instalar o VLC no diretório C:\VLC e não dentro da pasta Program Files (Arquivos de Programas)
3. Definir uma variável de ambiente `VLC_PLUGIN_PATH` com valor `C:\VLC\plugins`


Configração de Telas
====================

Você pode criar novos presets, eles ficam salvos em `~/Projector/Window Configs/*.json`

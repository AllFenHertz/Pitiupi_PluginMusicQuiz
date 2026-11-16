# `nbproject/` — configuração de build (NetBeans/Ant)

Arquivos do projeto Ant no formato do NetBeans, **copiados do plugin de chat** do Pitiupi com o nome
trocado (`pitiupi.chat` → `pitiupi.musicquiz`). O `build.xml` da pasta pai importa
`nbproject/build-impl.xml`, e é com ele que `scripts/pitiupi.sh` / `pitiupi.cmd` compilam o plugin.

| Arquivo | Para que serve |
| --- | --- |
| `project.properties` | Propriedades do build: diretórios, versão do Java, classpath do núcleo, nome do JAR |
| `project.xml` | Descrição do projeto para o NetBeans (tipo, nome, pastas de código) |
| `build-impl.xml` | Alvos Ant gerados (compilar, empacotar, limpar); não edite à mão |
| `genfiles.properties` | Controle interno do NetBeans sobre os arquivos gerados |

## Resultado do build

O JAR é gerado em `dist/pitiupi.musicquiz.jar`, caminho informado ao Pitiupi pela entrada do
`plugins.registry.json`. As pastas `build/` e `dist/` são geradas; em geral **não** devem ir para o
repositório (confira o `.gitignore`).

## Observações

- Como é cópia do chat, alguns campos herdam valores dele (por exemplo `application.vendor`).
  Ajuste se quiser.
- `genfiles.properties` guarda somas de verificação do NetBeans referentes aos arquivos do chat. Ao
  abrir o projeto no NetBeans ele pode regenerar `build-impl.xml`; isso é esperado.
- A forma como o build encontra as classes do núcleo é herdada do plugin de chat. Se o build
  reclamar de classes `pitiupi.*` inexistentes, compare o `project.properties` com o do chat e
  confira que o núcleo foi compilado antes.

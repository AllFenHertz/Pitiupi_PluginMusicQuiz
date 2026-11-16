# `src/META-INF/services/` — registro do plugin (SPI)

Contém um único arquivo, **`pitiupi.models.Plugin`**, com uma linha:

```
pitiupi.musicquiz.plugin.MusicQuizPlugin
```

## Para que serve

É o mecanismo `ServiceLoader` do Java. O nome do arquivo é o nome completo da classe/interface de
serviço que o núcleo procura (`pitiupi.models.Plugin`); o conteúdo é a classe concreta do plugin que
a implementa. Ao carregar o JAR, o núcleo lê esse arquivo e instancia a classe indicada.

## Cuidados

- O nome do arquivo precisa ser **exatamente** `pitiupi.models.Plugin`, sem extensão.
- Uma classe por linha, com o nome totalmente qualificado. Se você renomear a classe ou o pacote de
  `MusicQuizPlugin`, atualize esta linha, senão o plugin compila mas **não aparece** no Pitiupi.
- O Ant copia esta pasta para dentro do JAR (`META-INF/services/...`) junto com as classes. Confira
  que ela está no JAR final se o plugin não carregar.

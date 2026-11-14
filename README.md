# Music Quiz — plugin do Pitiupi

Quiz de música: uma sala com 2+ jogadores e um host que **não joga**. O host toca a música,
cada jogador digita a resposta e confirma com **OK** (a resposta trava); a rodada termina
quando todos confirmam ou o tempo acaba, e então vem o reveal com respostas e placar.

## Instalar (3 passos)

1. Copie a pasta `plugins/musicquiz` para `plugins/` do repositório.
2. Acrescente o conteúdo de `registry-entry.json` como mais uma entrada em
   `plugins.registry.json` (o arquivo estava fora do zip que recebi, por isso não editei).
3. Na raiz do repositório: `./scripts/pitiupi.sh run --all` (ou `--plugins chat,musicquiz`).

Abra a aba **Music Quiz**; cada pessoa escolhe **Host** ou **Jogador**.

## Pacote de músicas (só o host precisa, a menos que jogadores toquem localmente)

Uma pasta com os áudios e um `songs.tsv` em UTF-8, uma música por linha, separado por TAB:

    01.wav<TAB>Bohemian Rhapsody|bohemian rhapsody queen
    02.wav<TAB>Imagine|imagine john lennon

A primeira resposta é a que aparece no reveal. Use nomes neutros (`01.wav`): o nome do
arquivo viaja no pacote da rodada. Só WAV/AIFF/AU tocam sem biblioteca extra.

## Testar sem o Pitiupi

    cd plugins/musicquiz
    standalone/build.sh test    # testes do motor
    standalone/build.sh demo    # host + Ana + Bia na mesma JVM, com 4 notas de teste

## Estrutura

- `src/pitiupi/musicquiz/` — jogo (motor, telas, áudio). Não depende do núcleo.
- `src/pitiupi/musicquiz/plugin/` — a ponte: `MusicQuizPlugin`, `QuizMessage`,
  `PitiupiTransport`, `MusicQuizPanel`.
- `src/META-INF/services/pitiupi.models.Plugin` — SPI.
- `build.xml` e `nbproject/` — copiados do plugin de chat com o nome trocado.

## Rede e sigilo das respostas

- Estado da partida, entrada e saída da sala: UDP multicast para a sala (padrão do chat).
  O estado nunca contém respostas durante a rodada, só os nomes de quem já deu OK.
- **Respostas: TCP direto ao IP do host**, nunca para a sala. O host anuncia o próprio IP em
  cada pacote de estado (a cada 2s); se o jogador ainda não o conhece, não envia e tenta de novo.
  As respostas só são reveladas a todos no reveal, quando a rodada termina.
- Como UDP perde pacotes, o host reenvia o estado a cada 2s e o jogador reenvia a entrada na
  sala e a resposta até o host confirmar.

Limites do sigilo: o Pitiupi não tem criptografia, então quem capturar pacotes da rede ainda
poderia ler uma resposta; e nada impede um cliente adulterado de se passar pelo host.
Para uma sala de aula isso basta; não é segurança contra um atacante determinado.

## Limitações conhecidas

- A ponte nunca foi compilada contra o núcleo real (o zip tinha só `plugins/chat`); foi
  escrita copiando as chamadas do `ChatPlugin`/`ChatPanel` e conferida contra stubs.
- Host e jogador no **mesmo PC** não funcionam: TCP para o próprio host não é suportado pelo núcleo.
- Não usa o `PluginSessionWorkspace`/abas de sessão do chat; a aba é um painel simples.

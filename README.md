# Music Quiz — plugin do Pitiupi

Quiz de música: uma sala com 2+ jogadores e um host que **não joga**. O host toca a música,
cada jogador digita a resposta e confirma com **OK** (a resposta trava); a rodada termina
quando todos confirmam ou o tempo acaba, e então vem o reveal com respostas e placar.

## Instalar (3 passos)

1. Copie a pasta `plugins/musicquiz` para `plugins/` do repositório.
2. Acrescente o conteúdo de `registry-entry.json` como mais uma entrada em
   `plugins.registry.json`.
3. Na raiz do repositório: `./scripts/pitiupi.sh run --all`.

Abra a aba **Music Quiz**; cada pessoa escolhe **Host** ou **Jogador**.

## Pacote de músicas (só o host precisa, a menos que jogadores toquem localmente)

Uma pasta com os áudios e um `songs.tsv` em UTF-8, uma música por linha, separado por TAB:

    01.wav<TAB>Bohemian Rhapsody|bohemian rhapsody queen
    02.wav<TAB>Imagine|imagine john lennon

A primeira resposta é a que aparece no reveal. Use nomes neutros (`01.wav`): o nome do
arquivo viaja no pacote da rodada. Só WAV/AIFF/AU tocam sem biblioteca extra.

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


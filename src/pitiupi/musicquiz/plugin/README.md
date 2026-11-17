# `pitiupi.musicquiz.plugin` — ponte com o núcleo do Pitiupi

Única parte do projeto que depende da API do núcleo do Pitiupi. Ela adapta o jogo
([`../README.md`](../README.md)) ao que o Pitiupi espera de um plugin e leva os pacotes pela rede
real. Foi escrita seguindo as chamadas do plugin de chat (`ChatPlugin`, `ChatPanel`), que serve de
referência.

## Classes

| Classe | Papel |
| --- | --- |
| `MusicQuizPlugin` | Classe do plugin (`extends pitiupi.models.Plugin`) |
| `QuizMessage` | Mensagem do núcleo que carrega um `QuizPacket` |
| `PitiupiTransport` | Implementação de `QuizTransport` sobre a rede do Pitiupi |
| `MusicQuizPanel` | Painel da aba: escolher Host ou Jogador |

## `MusicQuizPlugin`

| Item | Valor |
| --- | --- |
| Identificador (`getIdentifier`) | `Pitiupi.MusicQuiz` |
| Nome / nome de exibição | `MusicQuiz` / `Music Quiz` |
| Versão | `1` |
| Mensagens declaradas (`getMessages`) | somente `QuizMessage` |
| Ação de peer | `musicquiz.focus` — "Abrir Music Quiz" (abre ou foca a aba) |

Fluxo em `createPlugin`: cria o `PitiupiTransport`, cria o `MusicQuizPanel`, entrega o painel ao
shell com `bindShellWorkspaceContent` e registra a ação de peer. Em `receiveMessage`, qualquer
`QuizMessage` com pacote é repassada ao transporte.

## `QuizMessage`

`extends Message` (que é `Serializable`), com um campo `packet` e `serialVersionUID` fixo. O núcleo
só entrega ao plugin tipos declarados em `getMessages()`, e desserializa com o classloader do JAR do
plugin, por isso `QuizPacket` e `QuizMessage` precisam existir **nas duas pontas, na mesma versão**.

## `PitiupiTransport`

| Método | Como envia |
| --- | --- |
| `send(pacote)` | `MessageDispatcher.send(..., SendTarget.broadcastAll(), TransportMode.MULTICAST_UDP)` — para a sala |
| `sendToHost(pacote)` | `connectionManager.sendTcpAsync(...)` para o IP do host — **TCP direto e privado** |
| `sendToPlayer(pacote, ip)` | `connectionManager.sendTcp(...)` para o IP do jogador — TCP **bloqueante**, usado nos pedaços de áudio (chamar fora da thread da interface) |
| `deliver(pacote)` | Entrega ao consumidor registrado; chamado pelo plugin ao receber |

- Em todo pacote enviado, preenche `senderIp` com o IP local (`LocalNetworkInfo.bestLocalIpv4()`
  normalizado por `PeerIpv4.normalize`).
- **Como o jogador descobre o host:** ao receber qualquer `STATE`, guarda o `senderIp` como IP do
  host. Enquanto não souber, `sendToHost` **não envia nada** (nunca recorre à sala), e o reenvio
  periódico do `PlayerPanel` tenta de novo.
- Falhas de envio são impressas no console (ex.: "resposta não entregue ao host").
- Só um consumidor fica registrado por vez (host **ou** jogador escuta), pois cada aba usa um papel.

## `MusicQuizPanel`

Usa `CardLayout` com três cartões: escolha de papel, `HostPanel` e `PlayerPanel`. Ao escolher, cria
a tela correspondente com o `PitiupiTransport`. O método `shutdown()` libera timers e áudio (e avisa
a saída do jogador), mas **ainda não é chamado** pelo plugin ao fechar a aba.

## Pontos de atenção

- Os nomes de classes e métodos do núcleo usados aqui foram deduzidos do plugin de chat; se uma
  versão futura do núcleo mudar essas assinaturas, a compilação desta pasta é a primeira a quebrar.
- Respostas por TCP assumem que o núcleo entrega mensagens TCP a `receiveMessage` (como descreve
  `docs/plugins.md` do Pitiupi). Em teste real (host em máquina física, jogador em máquina virtual) a
  partida solo funcionou; vale conferir que a rodada fecha assim que o jogador dá OK, sem esperar o
  fim do tempo.
- Host e jogador no mesmo computador não funcionam (TCP para o próprio host não é suportado).

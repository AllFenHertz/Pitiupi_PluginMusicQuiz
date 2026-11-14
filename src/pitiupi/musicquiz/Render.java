package pitiupi.musicquiz;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Transforma um STATE em texto para exibir. */
public final class Render {
    private Render() { }

    public static String describe(QuizPacket p) {
        StringBuilder sb = new StringBuilder();
        switch (p.phase) {
            case LOBBY:
                sb.append("Sala: ").append(p.players.isEmpty() ? "(vazia)" : String.join(", ", p.players));
                break;
            case ROUND:
                sb.append("Rodada ").append(p.round + 1).append("/").append(p.totalRounds)
                  .append(" — prontos: ").append(p.readyPlayers.size()).append("/").append(p.players.size());
                break;
            case REVEAL:
                sb.append("Rodada ").append(p.round + 1).append("/").append(p.totalRounds)
                  .append(" — resposta: ").append(p.correctAnswer).append("\n");
                for (Map.Entry<String, String> e : p.answers.entrySet()) {
                    boolean ok = Boolean.TRUE.equals(p.correct.get(e.getKey()));
                    String txt = e.getValue().isBlank() ? "(sem resposta)" : e.getValue();
                    sb.append("  ").append(ok ? "✔ " : "✘ ").append(e.getKey()).append(": ").append(txt).append("\n");
                }
                sb.append("\n").append(scoreboard(p));
                break;
            case END:
                sb.append("Fim de jogo!\n\n").append(scoreboard(p));
                break;
        }
        return sb.toString();
    }

    public static String scoreboard(QuizPacket p) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(p.scores.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder("Placar:\n");
        int pos = 1;
        for (Map.Entry<String, Integer> e : list) {
            sb.append("  ").append(pos++).append(". ").append(e.getKey()).append(" — ").append(e.getValue()).append("\n");
        }
        return sb.toString();
    }
}

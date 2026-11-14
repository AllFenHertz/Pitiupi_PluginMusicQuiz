package pitiupi.musicquiz;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import pitiupi.musicquiz.QuizPacket.Kind;
import pitiupi.musicquiz.QuizPacket.Phase;

/**
 * Estado da partida, só no host. Não conhece rede nem tela: recebe eventos
 * (join, answer, tick, next) e entrega pacotes STATE por "out".
 * Regra: a rodada termina quando todos os jogadores dão "ok" ou o tempo acaba.
 */
public class HostEngine {
    public static final int POINTS = 1;

    private final String hostName;
    private final LongSupplier clock;
    private final Consumer<QuizPacket> out;

    private List<Song> songs = List.of();
    private long roundMs = 30_000;
    private int minPlayers = 2;

    private Consumer<Song> onPlay = s -> { };
    private Runnable onStop = () -> { };

    private Phase phase = Phase.LOBBY;
    private int round = -1;
    private long deadline;
    private final Set<String> players = new LinkedHashSet<>();
    private Set<String> roundPlayers = new LinkedHashSet<>();
    private final Map<String, Integer> scores = new LinkedHashMap<>();
    private final Map<String, String> answers = new LinkedHashMap<>();
    private final Set<String> ready = new LinkedHashSet<>();
    private Map<String, Boolean> lastCorrect = new LinkedHashMap<>();

    public HostEngine(String hostName, LongSupplier clock, Consumer<QuizPacket> out) {
        this.hostName = hostName;
        this.clock = clock;
        this.out = out;
    }

    public synchronized void setAudioHooks(Consumer<Song> onPlay, Runnable onStop) {
        this.onPlay = onPlay;
        this.onStop = onStop;
    }

    public synchronized String configure(List<Song> songs, long roundMs, int minPlayers) {
        if (phase == Phase.ROUND || phase == Phase.REVEAL) return "Partida em andamento.";
        this.songs = List.copyOf(songs);
        this.roundMs = roundMs;
        this.minPlayers = minPlayers;
        return null;
    }

    public synchronized Phase phase() { return phase; }
    public synchronized int playerCount() { return players.size(); }

    public synchronized long remainingMs() {
        return phase == Phase.ROUND ? Math.max(0, deadline - clock.getAsLong()) : 0;
    }

    public synchronized void join(String name) {
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty() || name.equals(hostName)) return;
        if (players.add(name)) scores.putIfAbsent(name, 0);
        out.accept(snapshot());   // também serve de resposta para quem entra no meio
    }

    public synchronized void leave(String name) {
        if (!players.remove(name)) return;
        if (phase == Phase.ROUND) {
            roundPlayers.remove(name);
            if (roundPlayers.isEmpty() || ready.containsAll(roundPlayers)) {
                finishRound();
                return;
            }
        }
        out.accept(snapshot());
    }

    /** Retorna mensagem de erro ou null se iniciou. */
    public synchronized String start() {
        if (phase == Phase.ROUND || phase == Phase.REVEAL) return "A partida já está em andamento.";
        if (songs.isEmpty()) return "Nenhuma música carregada.";
        if (players.size() < minPlayers)
            return "São necessários pelo menos " + minPlayers + " jogadores (há " + players.size() + ").";
        round = -1;
        for (String p : players) scores.put(p, 0);
        startNextRound();
        return null;
    }

    public synchronized void next() {
        if (phase == Phase.REVEAL) startNextRound();
    }

    public synchronized void backToLobby() {
        onStop.run();
        phase = Phase.LOBBY;
        round = -1;
        out.accept(snapshot());
    }

    public synchronized void onAnswer(String sender, int r, String text, boolean isReady) {
        if (phase != Phase.ROUND || r != round) return;
        if (!roundPlayers.contains(sender) || ready.contains(sender)) return;
        answers.put(sender, text == null ? "" : text);
        if (!isReady) return;
        ready.add(sender);
        if (ready.containsAll(roundPlayers)) finishRound();
        else out.accept(snapshot());
    }

    /** Reenvia o estado atual (redes UDP perdem pacotes). Chamar a cada ~2s. */
    public synchronized void heartbeat() {
        if (!players.isEmpty()) out.accept(snapshot());
    }

    public synchronized void tick() {
        if (phase == Phase.ROUND && clock.getAsLong() >= deadline) finishRound();
    }

    // ---- interno ----

    private void startNextRound() {
        round++;
        if (round >= songs.size()) {
            onStop.run();
            phase = Phase.END;
            out.accept(snapshot());
            return;
        }
        phase = Phase.ROUND;
        deadline = clock.getAsLong() + roundMs;
        answers.clear();
        ready.clear();
        roundPlayers = new LinkedHashSet<>(players);
        onPlay.accept(songs.get(round));
        out.accept(snapshot());
    }

    private void finishRound() {
        Song s = songs.get(round);
        lastCorrect = new LinkedHashMap<>();
        for (String p : roundPlayers) {
            boolean ok = AnswerMatcher.matches(answers.getOrDefault(p, ""), s.accepted);
            lastCorrect.put(p, ok);
            if (ok) scores.merge(p, POINTS, Integer::sum);
        }
        phase = Phase.REVEAL;
        onStop.run();
        out.accept(snapshot());
    }

    private QuizPacket snapshot() {
        QuizPacket p = new QuizPacket(Kind.STATE, hostName);
        p.phase = phase;
        p.totalRounds = songs.size();
        p.round = round;
        p.players.addAll(players);
        p.scores.putAll(scores);
        if (phase == Phase.ROUND) {
            p.durationMs = Math.max(0, deadline - clock.getAsLong());
            p.clip = songs.get(round).file.getName();
            p.readyPlayers.addAll(ready);
        } else if (phase == Phase.REVEAL) {
            p.correctAnswer = songs.get(round).title;
            for (String pl : roundPlayers) p.answers.put(pl, answers.getOrDefault(pl, ""));
            p.correct.putAll(lastCorrect);
        }
        return p;
    }
}

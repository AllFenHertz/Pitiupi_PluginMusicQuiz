package pitiupi.musicquiz;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Único tipo de mensagem do quiz. Dentro do Pitiupi ele viaja embrulhado em
 * UMA classe de mensagem do núcleo (a ponte), então só um tipo precisa ser
 * declarado em getMessages().
 */
public class QuizPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind { JOIN, LEAVE, STATE, ANSWER, AUDIO, AUDIO_STATUS }
    public enum Phase { LOBBY, PREPARING, ROUND, REVEAL, END }

    public Kind kind;
    public String sender;
    public String senderIp;            // preenchido pela ponte; o host é localizado por ele

    // AUDIO (host -> um jogador): pedaço de um arquivo; AUDIO_STATUS (jogador -> host): o que já tem
    public String target;              // jogador a quem o pedaço se destina
    public long gameId;                // identifica a partida (o relógio do host ao iniciar)
    public int chunkIndex;
    public int chunkTotal;
    public byte[] data;
    public List<String> doneClips = new ArrayList<>();
    public int clipCount;              // STATE: quantos arquivos o host vai enviar

    // ANSWER (jogador -> host)
    public int round;
    public String text;
    public boolean ready;

    // STATE (host -> todos)
    public Phase phase;
    public int totalRounds;
    public long durationMs;            // tempo restante da rodada (só em ROUND)
    public String clip;                // nome do arquivo de áudio (só em ROUND)
    public List<String> players = new ArrayList<>();
    public List<String> readyPlayers = new ArrayList<>();
    public String correctAnswer;       // só em REVEAL
    public Map<String, String> answers = new LinkedHashMap<>();
    public Map<String, Boolean> correct = new LinkedHashMap<>();
    public Map<String, Integer> scores = new LinkedHashMap<>();

    public QuizPacket() { }

    public QuizPacket(Kind kind, String sender) {
        this.kind = kind;
        this.sender = sender;
    }

    public static QuizPacket join(String name) { return new QuizPacket(Kind.JOIN, name); }
    public static QuizPacket leave(String name) { return new QuizPacket(Kind.LEAVE, name); }

    public static QuizPacket answer(String name, int round, String text, boolean ready) {
        QuizPacket p = new QuizPacket(Kind.ANSWER, name);
        p.round = round;
        p.text = text;
        p.ready = ready;
        return p;
    }
}

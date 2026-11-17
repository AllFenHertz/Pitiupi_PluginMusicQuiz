package pitiupi.musicquiz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Tela do host: carrega as músicas, controla a partida e não joga. */
public class HostPanel extends JPanel {
    private final QuizTransport transport;
    private final HostEngine engine;
    private final AudioPlayer audio = new AudioPlayer();

    private final JTextField dirField = new JTextField(26);
    private final JSpinner rounds = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
    private final JSpinner seconds = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
    private final JSpinner minPlayers = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
    private final JCheckBox playHere = new JCheckBox("Tocar a música neste PC", true);
    private final JCheckBox sendAudio = new JCheckBox("Enviar as músicas aos jogadores pela rede", true);
    private final JButton skipBtn = new JButton("Começar sem esperar");
    private final Map<String, String> playerIps = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> reported = new ConcurrentHashMap<>();
    private final Set<String> sendingTo = ConcurrentHashMap.newKeySet();
    private final ExecutorService sendPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "quiz-audio-send");
        t.setDaemon(true);
        return t;
    });
    private final JButton startBtn = new JButton("Iniciar");
    private final JButton nextBtn = new JButton("Próxima");
    private final JButton lobbyBtn = new JButton("Voltar ao lobby");
    private final JLabel status = new JLabel("Lobby — aguardando jogadores");
    private final JTextArea log = new JTextArea(16, 40);
    private final Timer timer;
    private int ticks;
    private String lastLogged = "";

    public HostPanel(QuizTransport transport, File defaultDir) {
        this.transport = transport;
        this.engine = new HostEngine(transport.selfName(), System::currentTimeMillis, p -> {
            transport.send(p);
            SwingUtilities.invokeLater(() -> onState(p));
        });
        audio.setWarningHandler(this::log);
        engine.setAudioHooks(s -> { if (playHere.isSelected()) audio.play(s.file); }, audio::stop);
        engine.setPrepareHook(songs -> {
            long gid = engine.gameId();
            for (String name : engine.playerNames()) startSending(name, gid, songs);
        });

        if (defaultDir != null) dirField.setText(defaultDir.getPath());
        log.setEditable(false);

        JButton browse = new JButton("Pasta...");
        browse.addActionListener(e -> {
            JFileChooser c = new JFileChooser(dirField.getText());
            c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) dirField.setText(c.getSelectedFile().getPath());
        });

        JPanel top = new JPanel();
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r1.add(new JLabel("Músicas:")); r1.add(dirField); r1.add(browse);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r2.add(new JLabel("Rodadas:")); r2.add(rounds);
        r2.add(new JLabel("Segundos:")); r2.add(seconds);
        r2.add(new JLabel("Mín. jogadores:")); r2.add(minPlayers);
        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r3.add(playHere); r3.add(startBtn); r3.add(nextBtn); r3.add(lobbyBtn);
        JPanel r4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r4.add(sendAudio); r4.add(skipBtn);
        top.add(r1); top.add(r2); top.add(r3); top.add(r4);
        top.add(status);

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);

        startBtn.addActionListener(e -> start());
        nextBtn.addActionListener(e -> engine.next());
        skipBtn.addActionListener(e -> engine.skipPreparation());
        lobbyBtn.addActionListener(e -> engine.backToLobby());

        transport.setListener(p -> SwingUtilities.invokeLater(() -> receive(p)));
        timer = new Timer(200, e -> tick());
        timer.start();
        refreshButtons();
    }

    private void start() {
        try {
            List<String> warnings = new ArrayList<>();
            List<Song> all = SongPack.load(new File(dirField.getText().trim()), warnings);
            for (String w : warnings) log("Aviso: " + w);
            List<Song> picked = SongPack.pick(all, (Integer) rounds.getValue(), System.nanoTime());
            String err = engine.configure(picked, (Integer) seconds.getValue() * 1000L, (Integer) minPlayers.getValue());
            if (err == null) {
                reported.clear();
                sendingTo.clear();
                engine.setPrepare(sendAudio.isSelected());
                err = engine.start();
            }
            if (err != null) log("Não foi possível iniciar: " + err);
        } catch (Exception ex) {
            log("Erro ao carregar músicas: " + ex.getMessage());
        }
    }

    private void receive(QuizPacket p) {
        switch (p.kind) {
            case JOIN:
                if (p.senderIp != null) playerIps.put(p.sender, p.senderIp);
                engine.join(p.sender);
                lateJoinerAudio(p.sender);
                break;
            case AUDIO_STATUS:
                if (p.gameId == engine.gameId()) {
                    reported.put(p.sender, new HashSet<>(p.doneClips));
                    engine.onAudioStatus(p.sender, p.gameId, p.doneClips);
                }
                break;
            case LEAVE:  engine.leave(p.sender); break;
            case ANSWER: engine.onAnswer(p.sender, p.round, p.text, p.ready); break;
            default:     break; // STATE é nosso próprio eco
        }
    }

    /** Quem entra com a partida já em andamento também recebe as músicas. */
    private void lateJoinerAudio(String name) {
        QuizPacket.Phase ph = engine.phase();
        if (!sendAudio.isSelected() || ph == QuizPacket.Phase.LOBBY || ph == QuizPacket.Phase.END) return;
        if (engine.playerNames().contains(name)) startSending(name, engine.gameId(), engine.currentSongs());
    }

    /**
     * Envia as músicas a um jogador em segundo plano. Até 3 passadas: depois de cada uma espera o
     * jogador informar o que recebeu e reenvia só o que faltar.
     */
    private void startSending(String name, long gid, List<Song> songs) {
        if (!sendingTo.add(name)) return;
        String ip = playerIps.get(name);
        String host = transport.selfName();
        sendPool.execute(() -> {
            AudioSender.Link link = pkt -> transport.sendToPlayer(pkt, ip);
            for (int pass = 0; pass < 3; pass++) {
                Set<String> have = reported.getOrDefault(name, Set.of());
                boolean sentAny = false;
                for (Song s : songs) {
                    if (have.contains(s.file.getName())) continue;
                    sentAny = true;
                    try {
                        if (!AudioSender.sendClip(s.file, gid, host, name, link)) {
                            log("Falha ao enviar " + s.file.getName() + " para " + name);
                        }
                    } catch (IOException ex) {
                        log("Erro lendo " + s.file.getName() + ": " + ex.getMessage());
                    }
                }
                if (!sentAny) return;
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                }
                QuizPacket.Phase ph = engine.phase();
                if (engine.gameId() != gid || ph == QuizPacket.Phase.LOBBY || ph == QuizPacket.Phase.END) return;
            }
        });
    }

    private void onState(QuizPacket p) {
        if (p.phase != QuizPacket.Phase.ROUND) {
            String d = Render.describe(p);
            if (!d.equals(lastLogged)) {
                lastLogged = d;
                log(d);
            }
        }
        refreshButtons();
    }

    private void tick() {
        engine.tick();
        if (++ticks % 10 == 0) engine.heartbeat();   // a cada 2s
        QuizPacket.Phase ph = engine.phase();
        if (ph == QuizPacket.Phase.PREPARING) {
            status.setText("Enviando músicas aos jogadores — limite de espera: " + (engine.remainingMs() / 1000) + "s");
        } else if (ph == QuizPacket.Phase.ROUND) {
            status.setText("Rodada em andamento — " + (engine.remainingMs() / 1000 + 1) + "s");
        } else if (ph == QuizPacket.Phase.REVEAL) {
            status.setText("Resultado da rodada — clique em Próxima");
        } else if (ph == QuizPacket.Phase.END) {
            status.setText("Fim de jogo");
        } else {
            status.setText("Lobby — " + engine.playerCount() + " jogador(es)");
        }
    }

    private void refreshButtons() {
        QuizPacket.Phase ph = engine.phase();
        startBtn.setEnabled(ph == QuizPacket.Phase.LOBBY || ph == QuizPacket.Phase.END);
        skipBtn.setEnabled(ph == QuizPacket.Phase.PREPARING);
        nextBtn.setEnabled(ph == QuizPacket.Phase.REVEAL);
        lobbyBtn.setEnabled(ph != QuizPacket.Phase.LOBBY);
    }

    private void log(String s) {
        SwingUtilities.invokeLater(() -> {
            log.append(s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    public void dispose() {
        timer.stop();
        audio.stop();
        sendPool.shutdownNow();
    }
}

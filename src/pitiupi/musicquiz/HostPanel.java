package pitiupi.musicquiz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
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
        top.add(r1); top.add(r2); top.add(r3);
        top.add(status);

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(log), BorderLayout.CENTER);

        startBtn.addActionListener(e -> start());
        nextBtn.addActionListener(e -> engine.next());
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
            if (err == null) err = engine.start();
            if (err != null) log("Não foi possível iniciar: " + err);
        } catch (Exception ex) {
            log("Erro ao carregar músicas: " + ex.getMessage());
        }
    }

    private void receive(QuizPacket p) {
        switch (p.kind) {
            case JOIN:   engine.join(p.sender); break;
            case LEAVE:  engine.leave(p.sender); break;
            case ANSWER: engine.onAnswer(p.sender, p.round, p.text, p.ready); break;
            default:     break; // STATE é nosso próprio eco
        }
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
        if (ph == QuizPacket.Phase.ROUND) {
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
    }
}

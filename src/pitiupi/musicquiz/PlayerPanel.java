package pitiupi.musicquiz;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Tela do jogador: entra na sala, digita a resposta e confirma com "ok". */
public class PlayerPanel extends JPanel {
    private final QuizTransport transport;
    private final AudioPlayer audio = new AudioPlayer();

    private final JButton joinBtn = new JButton("Entrar na sala");
    private final JLabel status = new JLabel("Fora da sala");
    private final JLabel countdown = new JLabel(" ");
    private final JTextField answer = new JTextField(24);
    private final JButton okBtn = new JButton("OK");
    private final JCheckBox playLocal = new JCheckBox("Tocar a música aqui (pasta:)");
    private final JTextField dirField = new JTextField(14);
    private final JTextArea info = new JTextArea(12, 36);
    private final Timer timer;

    private boolean joined;
    private int currentRound = -1;
    private boolean locked;
    private long deadlineAt;
    private boolean inRoom;
    private boolean roundActive;
    private boolean iAmReady;
    private String lastAnswer = "";
    private long lastResend;

    public PlayerPanel(QuizTransport transport, File localSongsDir) {
        this.transport = transport;
        audio.setWarningHandler(s -> SwingUtilities.invokeLater(() -> info.append(s + "\n")));
        if (localSongsDir != null) dirField.setText(localSongsDir.getPath());
        info.setEditable(false);

        JPanel top = new JPanel();
        top.setLayout(new javax.swing.BoxLayout(top, javax.swing.BoxLayout.Y_AXIS));
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r1.add(new JLabel("Jogador: " + transport.selfName())); r1.add(joinBtn);
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r2.add(status); r2.add(countdown);
        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r3.add(answer); r3.add(okBtn);
        JPanel r4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r4.add(playLocal); r4.add(dirField);
        top.add(r1); top.add(r2); top.add(r3); top.add(r4);

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(info), BorderLayout.CENTER);

        setAnswering(false);
        joinBtn.addActionListener(e -> join());
        okBtn.addActionListener(e -> confirm());
        answer.addActionListener(e -> confirm());

        transport.setListener(p -> SwingUtilities.invokeLater(() -> receive(p)));
        timer = new Timer(250, e -> updateCountdown());
        timer.start();
    }

    private void join() {
        joined = true;
        joinBtn.setEnabled(false);
        status.setText("Entrando...");
        transport.send(QuizPacket.join(transport.selfName()));
    }

    private void confirm() {
        if (!okBtn.isEnabled() || locked) return;
        locked = true;
        lastAnswer = answer.getText();
        transport.sendToHost(QuizPacket.answer(transport.selfName(), currentRound, lastAnswer, true));
        setAnswering(false);
        status.setText("Resposta enviada — aguardando os outros");
    }

    private void receive(QuizPacket p) {
        if (p.kind != QuizPacket.Kind.STATE || !joined) return;
        inRoom = p.players.contains(transport.selfName());
        roundActive = p.phase == QuizPacket.Phase.ROUND;
        iAmReady = roundActive && p.readyPlayers.contains(transport.selfName());
        switch (p.phase) {
            case LOBBY:
                currentRound = -1;
                locked = false;
                audio.stop();
                setAnswering(false);
                countdown.setText(" ");
                status.setText("Na sala");
                info.setText(Render.describe(p));
                break;
            case ROUND:
                if (p.round != currentRound) {
                    currentRound = p.round;
                    locked = false;
                    answer.setText("");
                    setAnswering(true);
                    answer.requestFocusInWindow();
                    startLocalAudio(p.clip);
                }
                deadlineAt = System.currentTimeMillis() + p.durationMs;
                if (!locked) status.setText("Rodada " + (p.round + 1) + "/" + p.totalRounds + " — qual é a música?");
                info.setText(Render.describe(p));
                break;
            case REVEAL:
                audio.stop();
                setAnswering(false);
                countdown.setText(" ");
                status.setText("Resultado");
                info.setText(Render.describe(p));
                break;
            case END:
                audio.stop();
                setAnswering(false);
                countdown.setText(" ");
                status.setText("Fim de jogo");
                info.setText(Render.describe(p));
                currentRound = -1;
                break;
        }
    }

    private void startLocalAudio(String clip) {
        if (!playLocal.isSelected() || clip == null) return;
        File f = new File(dirField.getText().trim(), clip);
        if (f.isFile()) audio.play(f);
        else info.append("Arquivo local não encontrado: " + f + "\n");
    }

    private void updateCountdown() {
        resendIfNeeded();
        if (okBtn.isEnabled() || (locked && currentRound >= 0)) {
            long left = Math.max(0, deadlineAt - System.currentTimeMillis());
            countdown.setText((left / 1000 + 1) + "s");
        }
    }

    /** UDP perde pacotes: reenvia JOIN/resposta a cada 2s até o host confirmar. */
    private void resendIfNeeded() {
        long now = System.currentTimeMillis();
        if (!joined || now - lastResend < 2000) return;
        lastResend = now;
        if (!inRoom) {
            transport.send(QuizPacket.join(transport.selfName()));
        } else if (locked && roundActive && !iAmReady) {
            transport.sendToHost(QuizPacket.answer(transport.selfName(), currentRound, lastAnswer, true));
        }
    }

    private void setAnswering(boolean on) {
        answer.setEnabled(on);
        okBtn.setEnabled(on);
    }

    public void leave() {
        if (joined) transport.send(QuizPacket.leave(transport.selfName()));
    }

    public void dispose() {
        timer.stop();
        audio.stop();
    }
}

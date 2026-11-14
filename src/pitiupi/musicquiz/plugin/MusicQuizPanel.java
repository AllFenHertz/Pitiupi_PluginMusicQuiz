package pitiupi.musicquiz.plugin;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pitiupi.musicquiz.HostPanel;
import pitiupi.musicquiz.PlayerPanel;
import pitiupi.musicquiz.QuizTransport;

/** Painel da aba: o usuário escolhe ser host (não joga) ou jogador. */
public class MusicQuizPanel extends JPanel {

    private static final String CHOOSE = "choose";
    private static final String HOST = "host";
    private static final String PLAYER = "player";

    private final QuizTransport transport;
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private HostPanel hostPanel;
    private PlayerPanel playerPanel;

    public MusicQuizPanel(QuizTransport transport) {
        this.transport = transport;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel choose = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        choose.add(new JLabel("Você é:"));
        JButton hostBtn = new JButton("Host (não joga)");
        JButton playerBtn = new JButton("Jogador");
        choose.add(hostBtn);
        choose.add(playerBtn);
        body.add(choose, CHOOSE);
        add(body, BorderLayout.CENTER);

        hostBtn.addActionListener(e -> {
            hostPanel = new HostPanel(transport, null);
            body.add(hostPanel, HOST);
            cards.show(body, HOST);
        });
        playerBtn.addActionListener(e -> {
            playerPanel = new PlayerPanel(transport, null);
            body.add(playerPanel, PLAYER);
            cards.show(body, PLAYER);
        });
    }

    /** Libera timers e áudio (chamar ao fechar a aba). */
    public void shutdown() {
        if (hostPanel != null) hostPanel.dispose();
        if (playerPanel != null) {
            playerPanel.leave();
            playerPanel.dispose();
        }
    }
}

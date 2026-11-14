package pitiupi.musicquiz.plugin;

import java.util.List;
import javax.swing.SwingUtilities;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;

public class MusicQuizPlugin extends Plugin {

    private PluginWorkspace workspace;
    private PitiupiTransport transport;
    private MusicQuizPanel panel;

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.workspace = workspace;
        this.transport = new PitiupiTransport(connectionManager);
        this.panel = new MusicQuizPanel(transport);
        bindShellWorkspaceContent(panel);
        installPeerActions(workspace.peerActions());
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "musicquiz.focus",
                "Abrir Music Quiz",
                PeerActionTarget.ALL_PEERS,
                target -> workspace.openOrFocusTab(getIdentifier(), getDisplayName())
        ));
    }

    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of(QuizMessage.class);
    }

    @Override
    public void receiveMessage(Message message) {
        if (message instanceof QuizMessage msg && msg.getPacket() != null && transport != null) {
            transport.deliver(msg.getPacket());
        }
    }

    @Override
    public String getName() {
        return "MusicQuiz";
    }

    @Override
    public String getDisplayName() {
        return "Music Quiz";
    }

    @Override
    public String getAuthor() {
        return "";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public String getIdentifier() {
        return "Pitiupi.MusicQuiz";
    }
}

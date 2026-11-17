package pitiupi.musicquiz.plugin;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import pitiupi.application.messaging.MessageDispatcher;
import pitiupi.application.messaging.SendTarget;
import pitiupi.application.messaging.TransportMode;
import pitiupi.control.ConnectionManager;
import pitiupi.musicquiz.QuizPacket;
import pitiupi.musicquiz.QuizTransport;
import pitiupi.net.tcp.TcpAttemptListener;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.presentation.shell.PeerIpv4;

/**
 * Liga o quiz à rede do Pitiupi.
 * - Estado, entrada e saída da sala: UDP multicast para a sala (como o chat).
 * - Respostas: TCP direto ao IP do host (privado e confiável). Nunca vão para a sala.
 */
public class PitiupiTransport implements QuizTransport {

    private final ConnectionManager connectionManager;
    private final List<Consumer<QuizPacket>> listeners = new CopyOnWriteArrayList<>();
    private volatile String hostIp;

    public PitiupiTransport(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public String selfName() {
        return connectionManager.getUserName();
    }

    @Override
    public void send(QuizPacket p) {
        try {
            p.senderIp = PeerIpv4.normalize(LocalNetworkInfo.bestLocalIpv4());
            MessageDispatcher.send(connectionManager, new QuizMessage(p),
                    SendTarget.broadcastAll(), TransportMode.MULTICAST_UDP);
        } catch (Exception ex) {
            System.out.println("MusicQuiz: falha ao enviar " + p.kind + ": " + ex);
        }
    }

    @Override
    public void sendToHost(QuizPacket p) {
        String ip = hostIp;
        if (ip == null || ip.isEmpty()) {
            return;   // ainda não sei quem é o host: o jogador tenta de novo em 2s
        }
        try {
            p.senderIp = PeerIpv4.normalize(LocalNetworkInfo.bestLocalIpv4());
            connectionManager.sendTcpAsync(new QuizMessage(p), ip, new TcpAttemptListener() {
                @Override
                public void onConnecting(String endpoint) {
                }

                @Override
                public void onSuccess(TcpConnection connection) {
                }

                @Override
                public void onFailure(String message) {
                    System.out.println("MusicQuiz: resposta não entregue ao host: " + message);
                }
            });
        } catch (Exception ex) {
            System.out.println("MusicQuiz: falha ao enviar ao host: " + ex);
        }
    }

    /**
     * Envio bloqueante por TCP a um IP (usado nos pedaços de áudio). Retorna {@code false} se não
     * foi possível entregar. Chamar fora da thread da interface.
     */
    @Override
    public boolean sendToPlayer(QuizPacket p, String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        try {
            p.senderIp = PeerIpv4.normalize(LocalNetworkInfo.bestLocalIpv4());
            return connectionManager.sendTcp(new QuizMessage(p), InetAddress.getByName(ip));
        } catch (Exception ex) {
            System.out.println("MusicQuiz: falha ao enviar áudio a " + ip + ": " + ex);
            return false;
        }
    }

    @Override
    public void setListener(Consumer<QuizPacket> listener) {
        listeners.clear();          // só a tela ativa (host OU jogador) escuta
        listeners.add(listener);
    }

    /** Chamado pelo plugin quando chega uma QuizMessage. */
    public void deliver(QuizPacket p) {
        if (p.kind == QuizPacket.Kind.STATE && p.senderIp != null) {
            hostIp = p.senderIp;    // o host se anuncia em todo STATE (a cada 2s)
        }
        for (Consumer<QuizPacket> l : listeners) {
            l.accept(p);
        }
    }
}

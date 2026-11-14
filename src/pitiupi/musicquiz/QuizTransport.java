package pitiupi.musicquiz;

import java.util.function.Consumer;

/**
 * Fronteira entre o quiz e a rede. Dentro do Pitiupi, a implementação usa o
 * MessageDispatcher/receiveMessage do núcleo. Nos testes, usa Loopback.
 */
public interface QuizTransport {
    String selfName();

    /** Envia para a sala (todos). */
    void send(QuizPacket p);

    /**
     * Envia SOMENTE ao host (jogador -> host). Usado para as respostas: elas nunca
     * podem ir para a sala, senão outro jogador poderia ver e copiar.
     * Se o endereço do host ainda não é conhecido, não envia (e nunca cai para a sala).
     */
    void sendToHost(QuizPacket p);

    /** Registra quem recebe pacotes. Pode ser chamado em qualquer thread. */
    void setListener(Consumer<QuizPacket> listener);
}

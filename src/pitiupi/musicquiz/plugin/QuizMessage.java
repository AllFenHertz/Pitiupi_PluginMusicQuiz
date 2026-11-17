package pitiupi.musicquiz.plugin;

import pitiupi.models.Messages.Message;
import pitiupi.musicquiz.QuizPacket;

/** Mensagem do núcleo que carrega um QuizPacket. É o único tipo declarado em getMessages(). */
public class QuizMessage extends Message {

    private static final long serialVersionUID = 1L;

    private QuizPacket packet;

    public QuizMessage() {
    }

    public QuizMessage(QuizPacket packet) {
        this.packet = packet;
    }

    public QuizPacket getPacket() {
        return packet;
    }

    public void setPacket(QuizPacket packet) {
        this.packet = packet;
    }
}

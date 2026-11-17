package pitiupi.musicquiz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Lado do host: corta um arquivo de áudio em pedaços e envia a um jogador. */
public final class AudioSender {
    public static final int CHUNK_SIZE = 128 * 1024;
    private static final int ATTEMPTS = 3;

    /** Envio bloqueante de um pacote; {@code false} se falhou. Chamar fora da thread da interface. */
    public interface Link {
        boolean send(QuizPacket p);
    }

    private AudioSender() { }

    /** @return {@code true} se todos os pedaços foram entregues à rede (cada um com até 3 tentativas). */
    public static boolean sendClip(File file, long gameId, String hostName, String target, Link link)
            throws IOException {
        long len = file.length();
        int total = (int) Math.max(1, (len + CHUNK_SIZE - 1) / CHUNK_SIZE);
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            for (int i = 0; i < total; i++) {
                QuizPacket p = new QuizPacket(QuizPacket.Kind.AUDIO, hostName);
                p.target = target;
                p.gameId = gameId;
                p.clip = file.getName();
                p.chunkIndex = i;
                p.chunkTotal = total;
                p.data = in.readNBytes(CHUNK_SIZE);
                boolean ok = false;
                for (int attempt = 0; attempt < ATTEMPTS && !ok; attempt++) {
                    ok = link.send(p);
                    if (!ok && attempt < ATTEMPTS - 1) {
                        try {
                            Thread.sleep(300L * (attempt + 1));
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                }
                if (!ok) {
                    return false;
                }
            }
        }
        return true;
    }
}

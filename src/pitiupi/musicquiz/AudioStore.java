package pitiupi.musicquiz;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lado do jogador: remonta os arquivos de áudio enviados pelo host em pedaços.
 * Os pedaços podem chegar fora de ordem ou repetidos. Os arquivos ficam numa pasta temporária
 * e são descartados quando começa um jogo novo ({@code gameId} maior) ou em {@link #clear()}.
 */
public final class AudioStore {
    private final File dir;
    private long gameId = Long.MIN_VALUE;
    private final Map<String, byte[][]> partial = new HashMap<>();
    private final Map<String, Integer> counts = new HashMap<>();
    private final Map<String, File> done = new LinkedHashMap<>();

    public AudioStore() {
        try {
            dir = Files.createTempDirectory("musicquiz-").toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        dir.deleteOnExit();
    }

    /** Passa para um jogo mais novo, descartando o que era do anterior. Ids antigos são ignorados. */
    public synchronized void advanceTo(long newGameId) {
        if (newGameId <= gameId) {
            return;
        }
        deleteFiles();
        partial.clear();
        counts.clear();
        done.clear();
        gameId = newGameId;
    }

    /** @return {@code true} se este pedaço completou um arquivo. */
    public synchronized boolean accept(QuizPacket p) {
        if (p.data == null || p.clip == null || p.chunkTotal <= 0
                || p.chunkIndex < 0 || p.chunkIndex >= p.chunkTotal) {
            return false;
        }
        String name = safeName(p.clip);
        if (name == null) {
            return false;
        }
        advanceTo(p.gameId);
        if (p.gameId != gameId || done.containsKey(name)) {
            return false;
        }
        byte[][] parts = partial.computeIfAbsent(name, k -> new byte[p.chunkTotal][]);
        if (parts.length != p.chunkTotal) {
            return false;
        }
        if (parts[p.chunkIndex] == null) {
            parts[p.chunkIndex] = p.data;
            counts.merge(name, 1, Integer::sum);
        }
        if (counts.get(name) < parts.length) {
            return false;
        }
        File out = new File(dir, name);
        try {
            try (java.io.OutputStream os = new java.io.BufferedOutputStream(new java.io.FileOutputStream(out))) {
                for (byte[] part : parts) {
                    os.write(part);
                }
            }
        } catch (IOException e) {
            partial.remove(name);
            counts.remove(name);
            return false;
        }
        out.deleteOnExit();
        done.put(name, out);
        partial.remove(name);
        counts.remove(name);
        return true;
    }

    public synchronized boolean has(String clip) {
        return clip != null && done.containsKey(clip);
    }

    public synchronized File file(String clip) {
        return done.get(clip);
    }

    public synchronized List<String> doneClips() {
        return new ArrayList<>(done.keySet());
    }

    public synchronized long gameId() {
        return gameId;
    }

    public synchronized void clear() {
        deleteFiles();
        partial.clear();
        counts.clear();
        done.clear();
        gameId = Long.MIN_VALUE;
        dir.delete();
    }

    private void deleteFiles() {
        for (File f : done.values()) {
            f.delete();
        }
    }

    /** Só aceita nome de arquivo simples, para um pacote malformado não escrever fora da pasta. */
    static String safeName(String clip) {
        if (clip.isBlank() || clip.contains("/") || clip.contains("\\") || clip.contains("..")) {
            return null;
        }
        return clip;
    }
}

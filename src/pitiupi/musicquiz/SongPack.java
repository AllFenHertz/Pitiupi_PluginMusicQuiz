package pitiupi.musicquiz;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Lê uma pasta com songs.tsv. Cada linha: arquivo TAB resposta1|resposta2|...
 * Linhas vazias e iniciadas por # são ignoradas.
 */
public final class SongPack {
    private SongPack() { }

    public static List<Song> load(File dir, List<String> warnings) throws IOException {
        File tsv = new File(dir, "songs.tsv");
        if (!tsv.isFile()) throw new IOException("songs.tsv não encontrado em " + dir);
        List<Song> songs = new ArrayList<>();
        int n = 0;
        for (String line : Files.readAllLines(tsv.toPath(), StandardCharsets.UTF_8)) {
            n++;
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] parts = line.split("\t", 2);
            if (parts.length < 2 || parts[1].isBlank()) {
                warnings.add("linha " + n + ": formato inválido");
                continue;
            }
            File f = new File(dir, parts[0].trim());
            if (!f.isFile()) {
                warnings.add("linha " + n + ": arquivo não existe (" + parts[0].trim() + ")");
                continue;
            }
            List<String> acc = new ArrayList<>();
            for (String a : parts[1].split("\\|")) {
                if (!a.isBlank()) acc.add(a.trim());
            }
            if (acc.isEmpty()) {
                warnings.add("linha " + n + ": sem respostas aceitas");
                continue;
            }
            songs.add(new Song(f, acc));
        }
        return songs;
    }

    /** Embaralha e limita a quantidade de rodadas. */
    public static List<Song> pick(List<Song> all, int rounds, long seed) {
        List<Song> copy = new ArrayList<>(all);
        Collections.shuffle(copy, new Random(seed));
        return new ArrayList<>(copy.subList(0, Math.min(rounds, copy.size())));
    }

    static List<String> list(String... s) { return Arrays.asList(s); }
}

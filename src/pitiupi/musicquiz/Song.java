package pitiupi.musicquiz;

import java.io.File;
import java.util.List;

public final class Song {
    public final File file;
    public final String title;          // primeira resposta aceita (mostrada no reveal)
    public final List<String> accepted;

    public Song(File file, List<String> accepted) {
        this.file = file;
        this.accepted = accepted;
        this.title = accepted.get(0);
    }
}

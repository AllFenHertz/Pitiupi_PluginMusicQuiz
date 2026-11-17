package pitiupi.musicquiz;

import java.io.File;
import java.util.function.Consumer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/** Toca um arquivo por vez. Nativamente só WAV/AIFF/AU (MP3 exige biblioteca extra). */
public class AudioPlayer {
    private Clip clip;
    private Consumer<String> warn = s -> { };

    public void setWarningHandler(Consumer<String> warn) { this.warn = warn; }

    public synchronized void play(File f) {
        stop();
        try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
            clip = AudioSystem.getClip();
            clip.open(in);
            clip.start();
        } catch (Exception e) {
            warn.accept("Não foi possível tocar " + f.getName() + ": " + e.getMessage());
        }
    }

    public synchronized void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }
}

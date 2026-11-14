package pitiupi.musicquiz;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

/** Compara a resposta digitada com os nomes aceitos de uma música. */
public final class AnswerMatcher {
    private AnswerMatcher() { }

    public static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    public static boolean matches(String answer, Collection<String> accepted) {
        String a = normalize(answer);
        if (a.isEmpty()) return false;
        for (String acc : accepted) {
            String b = normalize(acc);
            if (b.isEmpty()) continue;
            if (a.equals(b)) return true;
            int tolerance = b.length() >= 10 ? 2 : (b.length() >= 5 ? 1 : 0);
            if (tolerance > 0 && levenshtein(a, b) <= tolerance) return true;
        }
        return false;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}

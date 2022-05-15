package commons;

public class StringUtils {
    public static String stripAccents(String text) {
        StringBuilder newText = new StringBuilder();
        String[] letters = text.split("(?U)");
        for (String letter: letters) {
            if (!letter.matches("\\p{M}")) {
                newText.append(letter);
            }
        }
        return newText.toString();
    }
}

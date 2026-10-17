package ee.kerrete.ainterview.aireadiness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Jagamislingi sisu, sama vorming mis frontendi share-link.ts: "p:accountant;a:routine.lt20,people.lot;s:30,70".
 * Ameti id, iga vastuse variandi id ja soovi korral ülesannete osakaalud tulemuse ülesannete järjekorras.
 */
public record ShareLink(String professionId, Map<String, String> answers, List<Integer> shares) {

    static final int MAX_LENGTH = 600;
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    /** Loeb lingi sisu; tühi, kui see pole korrektne jagamislink. Vigased osakaalud jäetakse välja nagu frontendis. */
    public static Optional<ShareLink> parse(String body) {
        if (body == null || body.isBlank() || body.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (String part : body.split(";")) {
            int i = part.indexOf(':');
            if (i > 0) {
                fields.put(part.substring(0, i), part.substring(i + 1));
            }
        }
        String professionId = fields.get("p");
        if (professionId == null || !ID.matcher(professionId).matches()) {
            return Optional.empty();
        }
        Map<String, String> answers = new LinkedHashMap<>();
        for (String pair : fields.getOrDefault("a", "").split(",")) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] qo = pair.split("\\.", -1);
            if (qo.length != 2 || !ID.matcher(qo[0]).matches() || !ID.matcher(qo[1]).matches()) {
                return Optional.empty();
            }
            answers.put(qo[0], qo[1]);
        }
        List<Integer> shares = null;
        String s = fields.get("s");
        if (s != null && !s.isEmpty()) {
            try {
                List<Integer> parsed = java.util.Arrays.stream(s.split(",")).map(Integer::valueOf).toList();
                boolean valid = parsed.stream().allMatch(v -> v >= 0 && v <= 100)
                        && parsed.stream().mapToInt(Integer::intValue).sum() == 100;
                shares = valid ? parsed : null;
            } catch (NumberFormatException e) {
                shares = null;
            }
        }
        return Optional.of(new ShareLink(professionId, answers, shares));
    }
}

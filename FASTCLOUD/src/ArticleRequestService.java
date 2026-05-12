import java.net.URI;
import java.net.http.*;
import java.util.*;

/**
 * Firestore-backed service for managing user article requests.
 * Collection: articleRequests
 * Fields: topic, preferredBotId, preferredBotName, requestedBy, status, articleId, createdAt
 */
public class ArticleRequestService {

    private static final String PROJECT_ID = "aqalnama-9d5f2";
    private static final String BASE_URL   =
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
        "/databases/(default)/documents/articleRequests";

    private final HttpClient http = HttpClient.newHttpClient();

    // =========================================================================
    //  Save a new article request to Firestore
    // =========================================================================
    public String saveRequest(String topic, String preferredBotId,
                              String preferredBotName, String requestedBy) {
        try {
            String docId = "req_" + System.currentTimeMillis();
            String url   = BASE_URL + "/" + docId;
            String body  = "{\"fields\":{"
                + "\"topic\":" + sv(topic) + ","
                + "\"preferredBotId\":" + sv(preferredBotId) + ","
                + "\"preferredBotName\":" + sv(preferredBotName) + ","
                + "\"requestedBy\":" + sv(requestedBy) + ","
                + "\"status\":" + sv("PENDING") + ","
                + "\"articleId\":" + sv("") + ","
                + "\"createdAt\":{\"integerValue\":\"" + System.currentTimeMillis() + "\"}"
                + "}}";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                System.out.println("[ArticleRequestService] Saved request: " + topic);
                return docId;
            } else {
                System.out.println("[ArticleRequestService] Save failed: " + res.body());
                return null;
            }
        } catch (Exception e) {
            System.out.println("[ArticleRequestService] Error: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    //  Update request status (PENDING → GENERATING → COMPLETED / FAILED)
    // =========================================================================
    public boolean updateRequestStatus(String requestId, String newStatus, String articleId) {
        try {
            String url = BASE_URL + "/" + requestId
                + "?updateMask.fieldPaths=status&updateMask.fieldPaths=articleId";
            String body = "{\"fields\":{"
                + "\"status\":" + sv(newStatus) + ","
                + "\"articleId\":" + sv(articleId == null ? "" : articleId)
                + "}}";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("[ArticleRequestService] Update error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    //  Fetch all article requests from Firestore
    // =========================================================================
    public List<Map<String, String>> getAllRequests() {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return list;
            parseRequests(res.body(), list);
        } catch (Exception e) {
            System.out.println("[ArticleRequestService] Fetch error: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    //  Fetch requests by a specific user
    // =========================================================================
    public List<Map<String, String>> getRequestsByUser(String username) {
        List<Map<String, String>> all = getAllRequests();
        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> r : all) {
            if (username.equals(r.get("requestedBy"))) filtered.add(r);
        }
        // Sort by createdAt descending
        filtered.sort((a, b) -> {
            long aTime = 0, bTime = 0;
            try { aTime = Long.parseLong(a.getOrDefault("createdAt", "0")); } catch (Exception ignored) {}
            try { bTime = Long.parseLong(b.getOrDefault("createdAt", "0")); } catch (Exception ignored) {}
            return Long.compare(bTime, aTime);
        });
        return filtered;
    }

    // =========================================================================
    //  JSON PARSING — manual like the rest of the project
    // =========================================================================
    private void parseRequests(String json, List<Map<String, String>> list) {
        int docStart = findNextDocument(json, "/documents/articleRequests/", 0);
        while (docStart != -1) {
            try {
                int docPathStart = json.indexOf(":", docStart) + 1;
                docPathStart = skipWhitespace(json, docPathStart);
                String docPath = readJsonString(json, docPathStart + 1);
                int docPathEnd = findStringEnd(json, docPathStart + 1);
                int nextDoc = findNextDocument(json, "/documents/articleRequests/", docPathEnd + 1);
                String block = json.substring(docStart, nextDoc == -1 ? json.length() : nextDoc);

                String id = docPath.substring(docPath.lastIndexOf("/") + 1);

                Map<String, String> req = new HashMap<>();
                req.put("id", id);
                req.put("topic", extractField(block, "topic"));
                req.put("preferredBotId", extractField(block, "preferredBotId"));
                req.put("preferredBotName", extractField(block, "preferredBotName"));
                req.put("requestedBy", extractField(block, "requestedBy"));
                req.put("status", extractField(block, "status"));
                req.put("articleId", extractField(block, "articleId"));
                req.put("createdAt", String.valueOf(parseLong(block, "createdAt")));

                list.add(req);
            } catch (Exception ignored) {}
            docStart = findNextDocument(json, "/documents/articleRequests/", docStart + 1);
        }
    }

    // =========================================================================
    //  Helpers (same pattern as ArticleService / BotService)
    // =========================================================================
    private String extractField(String block, String key) {
        int fieldStart = findFirestoreField(block, key);
        if (fieldStart == -1) return "";
        int valueKey = block.indexOf("\"stringValue\"", fieldStart);
        if (valueKey == -1) return "";
        int colon = block.indexOf(":", valueKey);
        if (colon == -1) return "";
        int valueStart = skipWhitespace(block, colon + 1);
        if (valueStart >= block.length() || block.charAt(valueStart) != '"') return "";
        String value = readJsonString(block, valueStart + 1);
        return value == null ? "" : value;
    }

    private long parseLong(String block, String key) {
        int fieldStart = findFirestoreField(block, key);
        if (fieldStart == -1) return 0L;
        int valueKey = block.indexOf("\"integerValue\"", fieldStart);
        if (valueKey == -1) return 0L;
        int colon = block.indexOf(":", valueKey);
        if (colon == -1) return 0L;
        int valueStart = skipWhitespace(block, colon + 1);
        if (valueStart >= block.length() || block.charAt(valueStart) != '"') return 0L;
        String value = readJsonString(block, valueStart + 1);
        try { return value == null ? 0L : Long.parseLong(value); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private int findFirestoreField(String block, String key) {
        int pos = 0;
        String needle = "\"" + key + "\"";
        while (pos >= 0 && pos < block.length()) {
            int fieldStart = block.indexOf(needle, pos);
            if (fieldStart == -1) return -1;
            int colon = block.indexOf(":", fieldStart);
            if (colon == -1) return -1;
            int valueStart = skipWhitespace(block, colon + 1);
            if (valueStart < block.length() && block.charAt(valueStart) == '{') return fieldStart;
            pos = fieldStart + needle.length();
        }
        return -1;
    }

    private int findNextDocument(String json, String collectionPath, int from) {
        int pos = from;
        while (pos >= 0 && pos < json.length()) {
            int nameKey = json.indexOf("\"name\"", pos);
            if (nameKey == -1) return -1;
            int colon = json.indexOf(":", nameKey);
            if (colon == -1) return -1;
            int valueStart = skipWhitespace(json, colon + 1);
            if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                String value = readJsonString(json, valueStart + 1);
                if (value != null && value.contains(collectionPath)) return nameKey;
            }
            pos = nameKey + 6;
        }
        return -1;
    }

    private int skipWhitespace(String s, int index) {
        while (index < s.length() && Character.isWhitespace(s.charAt(index))) index++;
        return index;
    }

    private int findStringEnd(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') i++;
            else if (c == '"') return i;
        }
        return json.length() - 1;
    }

    private String readJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == 'n')       { sb.append('\n'); i++; }
                else if (next == 'r')  { i++; }
                else if (next == 't')  { sb.append('\t'); i++; }
                else if (next == '"')  { sb.append('"');  i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else sb.append(next);
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private String sv(String val) {
        String safe = val == null ? "" : val
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
        return "{\"stringValue\":\"" + safe + "\"}";
    }
}

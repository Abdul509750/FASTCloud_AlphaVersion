import java.net.URI;
import java.net.http.*;
import java.util.*;

public class BotService {

    private static final String PROJECT_ID  = "aqalnama-9d5f2";
    private static final String WEB_API_KEY = "AIzaSyB2MsjsrGqaWMcfpB2xUo_Zjdzmuap-DGE";
    private static final String BOTS_URL    =
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
        "/databases/(default)/documents/bots";
    private static final String USERS_URL   =
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
        "/databases/(default)/documents/users";
    private static final String AUTH_URL    =
        "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + WEB_API_KEY;

    private final HttpClient http = HttpClient.newHttpClient();

    // =========================================================================
    //  Register bot in Firebase Auth + Firestore users + Firestore bots
    // =========================================================================
    public BotPersona registerBot(String name, String topic, String style, int intervalMins) {
        try {
            // 1. Generate bot email
            String safeName = name.toLowerCase().replaceAll("[^a-z0-9]", "");
            String email    = safeName + "_bot@aqalnama.com";
            String password = "AqalBot@" + System.currentTimeMillis();

            // 2. Register in Firebase Auth
            String authBody = "{\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\","
                + "\"returnSecureToken\":true}";

            HttpRequest authReq = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(authBody))
                .build();

            HttpResponse<String> authRes = http.send(authReq, HttpResponse.BodyHandlers.ofString());
            String uid = extractJson(authRes.body(), "localId");
            if (uid == null || uid.isEmpty()) {
                System.out.println("[BotService] Auth registration failed: " + authRes.body());
                return null;
            }
            System.out.println("[BotService] Bot registered in Auth: " + uid);

            // 3. Save in Firestore users/{uid} with role=BOT
            String userDoc = "{\"fields\":{"
                + "\"username\":{\"stringValue\":\"" + name + "\"},"
                + "\"role\":{\"stringValue\":\"BOT\"},"
                + "\"status\":{\"stringValue\":\"ACTIVE\"}"
                + "}}";
            HttpRequest userReq = HttpRequest.newBuilder()
                .uri(URI.create(USERS_URL + "/" + uid))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(userDoc))
                .build();
            http.send(userReq, HttpResponse.BodyHandlers.ofString());

            // 4. Save in Firestore bots/{uid}
            BotPersona bot = new BotPersona(uid, name, topic, style, intervalMins);
            String botDoc = "{\"fields\":{"
                + "\"name\":" + sv(name) + ","
                + "\"topic\":" + sv(topic) + ","
                + "\"style\":" + sv(style) + ","
                + "\"intervalMins\":{\"integerValue\":\"" + intervalMins + "\"},"
                + "\"status\":{\"stringValue\":\"ACTIVE\"},"
                + "\"lastRun\":{\"integerValue\":\"0\"},"
                + "\"articleCount\":{\"integerValue\":\"0\"},"
                + "\"flagged\":{\"booleanValue\":false},"
                + "\"flagReason\":" + sv("")
                + "}}";
            HttpRequest botReq = HttpRequest.newBuilder()
                .uri(URI.create(BOTS_URL + "/" + uid))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(botDoc))
                .build();
            http.send(botReq, HttpResponse.BodyHandlers.ofString());

            System.out.println("[BotService] Bot fully registered: " + name);
            return bot;

        } catch (Exception e) {
            System.out.println("[BotService] Error: " + e.getMessage());
            return null;
        }
    }

    // =========================================================================
    //  Fetch all bots from Firestore
    // =========================================================================
    public List<BotPersona> getAllBots() {
        List<BotPersona> list = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BOTS_URL))
                .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return list;
            parseBots(res.body(), list);
        } catch (Exception e) {
            System.out.println("[BotService] Fetch error: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    //  Update bot status (ACTIVE / PAUSED)
    // =========================================================================
    public boolean updateBotStatus(String botId, String status) {
        try {
            String url  = BOTS_URL + "/" + botId + "?updateMask.fieldPaths=status";
            String body = "{\"fields\":{\"status\":{\"stringValue\":\"" + status + "\"}}}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public boolean updateBotFlag(String botId, boolean flagged, String reason) {
        try {
            String url = BOTS_URL + "/" + botId
                + "?updateMask.fieldPaths=flagged&updateMask.fieldPaths=flagReason";
            String body = "{\"fields\":{"
                + "\"flagged\":{\"booleanValue\":" + flagged + "},"
                + "\"flagReason\":" + sv(flagged ? reason : "")
                + "}}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                System.out.println("[BotService] Flag update failed: " + res.body());
            }
            return res.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("[BotService] Flag update error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteBot(String botId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BOTS_URL + "/" + botId))
                .DELETE()
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                System.out.println("[BotService] Delete failed: " + res.body());
            }
            return res.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("[BotService] Delete error: " + e.getMessage());
            return false;
        }
    }

    public boolean updateBotArticleCount(String botId, int newCount) {
        try {
            String url = BOTS_URL + "/" + botId + "?updateMask.fieldPaths=articleCount";
            String body = "{\"fields\":{"
                + "\"articleCount\":{\"integerValue\":\"" + Math.max(0, newCount) + "\"}"
                + "}}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                System.out.println("[BotService] Article count update failed: " + res.body());
            }
            return res.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("[BotService] Article count update error: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    //  Update bot lastRun + articleCount
    // =========================================================================
    public void updateBotAfterWrite(String botId, int newCount) {
        try {
            String url  = BOTS_URL + "/" + botId
                + "?updateMask.fieldPaths=lastRun&updateMask.fieldPaths=articleCount";
            String body = "{\"fields\":{"
                + "\"lastRun\":{\"integerValue\":\"" + System.currentTimeMillis() + "\"},"
                + "\"articleCount\":{\"integerValue\":\"" + newCount + "\"}"
                + "}}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  Helpers
    // =========================================================================
    private void parseBots(String json, List<BotPersona> list) {
        int docStart = findNextBotDocument(json, 0);
        while (docStart != -1) {
            try {
                int docPathStart = json.indexOf(":", docStart) + 1;
                docPathStart = skipWhitespace(json, docPathStart);
                if (docPathStart >= json.length() || json.charAt(docPathStart) != '"') {
                    docStart = findNextBotDocument(json, docStart + 1);
                    continue;
                }

                String docPath = readJsonString(json, docPathStart + 1);
                int docPathEnd = findStringEnd(json, docPathStart + 1);
                int nextDoc = findNextBotDocument(json, docPathEnd + 1);
                String block = json.substring(docStart, nextDoc == -1 ? json.length() : nextDoc);

                String id     = docPath.substring(docPath.lastIndexOf("/") + 1);
                String name   = extractField(block, "name");
                String topic  = extractField(block, "topic");
                String style  = extractField(block, "style");
                String status = extractField(block, "status");
                int    mins   = parseInt(block, "intervalMins");
                int    count  = parseInt(block, "articleCount");
                boolean flagged = parseBoolean(block, "flagged");
                String flagReason = extractField(block, "flagReason");

                if (name.isBlank() || topic.isBlank()) {
                    docStart = nextDoc;
                    continue;
                }
                if (status.isBlank()) status = "PAUSED";
                if (mins <= 0) mins = 1;

                BotPersona bp = new BotPersona(id, name, topic, style, mins);
                bp.setStatus(status);
                bp.setArticleCount(count);
                bp.setFlagged(flagged);
                bp.setFlagReason(flagReason);
                list.add(bp);
            } catch (Exception ignored) {}
            docStart = findNextBotDocument(json, docStart + 1);
        }
    }

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

    private int parseInt(String block, String key) {
        int fieldStart = findFirestoreField(block, key);
        if (fieldStart == -1) return 0;
        int valueKey = block.indexOf("\"integerValue\"", fieldStart);
        if (valueKey == -1) return 0;
        int colon = block.indexOf(":", valueKey);
        if (colon == -1) return 0;
        int valueStart = skipWhitespace(block, colon + 1);
        if (valueStart >= block.length() || block.charAt(valueStart) != '"') return 0;
        String value = readJsonString(block, valueStart + 1);
        try { return value == null ? 0 : Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private boolean parseBoolean(String block, String key) {
        int fieldStart = findFirestoreField(block, key);
        if (fieldStart == -1) return false;
        int valueKey = block.indexOf("\"booleanValue\"", fieldStart);
        if (valueKey == -1) return false;
        int colon = block.indexOf(":", valueKey);
        if (colon == -1) return false;
        int valueStart = skipWhitespace(block, colon + 1);
        return block.startsWith("true", valueStart);
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

    private String extractJson(String json, String key) {
        String[] patterns = {
            "\"" + key + "\":\"",
            "\"" + key + "\": \""
        };
        for (String s : patterns) {
            int start = json.indexOf(s);
            if (start != -1) {
                start += s.length();
                int end = json.indexOf("\"", start);
                if (end != -1) return json.substring(start, end);
            }
        }
        return null;
    }

    private int findNextBotDocument(String json, int from) {
        int pos = from;
        while (pos >= 0 && pos < json.length()) {
            int nameKey = json.indexOf("\"name\"", pos);
            if (nameKey == -1) return -1;
            int colon = json.indexOf(":", nameKey);
            if (colon == -1) return -1;
            int valueStart = skipWhitespace(json, colon + 1);
            if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                String value = readJsonString(json, valueStart + 1);
                if (value != null && value.contains("/documents/bots/")) return nameKey;
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

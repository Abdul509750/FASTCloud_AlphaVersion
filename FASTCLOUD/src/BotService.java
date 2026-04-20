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
                + "\"name\":{\"stringValue\":\"" + name + "\"},"
                + "\"topic\":{\"stringValue\":\"" + topic + "\"},"
                + "\"style\":{\"stringValue\":\"" + style + "\"},"
                + "\"intervalMins\":{\"integerValue\":\"" + intervalMins + "\"},"
                + "\"status\":{\"stringValue\":\"ACTIVE\"},"
                + "\"lastRun\":{\"integerValue\":\"0\"},"
                + "\"articleCount\":{\"integerValue\":\"0\"}"
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
        String[] docs = json.split("\"name\":");
        for (int i = 1; i < docs.length; i++) {
            try {
                String block  = docs[i];
                String namePart = block.substring(1, block.indexOf("\"", 1));
                String id     = namePart.substring(namePart.lastIndexOf("/") + 1);
                String name   = extractField(block, "name");
                String topic  = extractField(block, "topic");
                String style  = extractField(block, "style");
                String status = extractField(block, "status");
                int    mins   = parseInt(block, "intervalMins");
                int    count  = parseInt(block, "articleCount");
                BotPersona bp = new BotPersona(id, name, topic, style, mins);
                bp.setStatus(status);
                bp.setArticleCount(count);
                list.add(bp);
            } catch (Exception ignored) {}
        }
    }

    private String extractField(String block, String key) {
        String[] searches = {
            "\"" + key + "\":{\"stringValue\":\"",
            "\"" + key + "\": {\"stringValue\": \""
        };
        for (String s : searches) {
            int start = block.indexOf(s);
            if (start != -1) {
                start += s.length();
                int end = block.indexOf("\"", start);
                if (end != -1) return block.substring(start, end);
            }
        }
        return "";
    }

    private int parseInt(String block, String key) {
        String[] searches = {
            "\"" + key + "\":{\"integerValue\":\"",
            "\"" + key + "\": {\"integerValue\": \""
        };
        for (String s : searches) {
            int start = block.indexOf(s);
            if (start != -1) {
                start += s.length();
                int end = block.indexOf("\"", start);
                if (end != -1) {
                    try { return Integer.parseInt(block.substring(start, end)); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
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
}

import java.net.URI;
import java.net.http.*;
import java.util.*;

public class ArticleService {

    private static final String PROJECT_ID = "aqalnama-9d5f2";
    private static final String BASE_URL   =
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
        "/databases/(default)/documents/articles";

    private final HttpClient http = HttpClient.newHttpClient();

    // ── Save new article ──────────────────────────────────────────────────────
    public String saveArticle(Article article) {
        try {
            String docId = "article_" + System.currentTimeMillis();
            String url   = BASE_URL + "/" + docId;
            String body  = buildArticleJson(article);

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                System.out.println("[ArticleService] Saved: " + article.getTitle());
                return docId;
            } else {
                System.out.println("[ArticleService] Save failed: " + res.body());
                return null;
            }
        } catch (Exception e) {
            System.out.println("[ArticleService] Error: " + e.getMessage());
            return null;
        }
    }

    // ── Fetch all articles ────────────────────────────────────────────────────
    public List<Article> getAllArticles() {
        List<Article> list = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET().build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return list;
            parseArticles(res.body(), list);
        } catch (Exception e) {
            System.out.println("[ArticleService] Fetch error: " + e.getMessage());
        }
        return list;
    }

    // ── Fetch articles by status ──────────────────────────────────────────────
    public List<Article> getArticlesByStatus(String status) {
        List<Article> all = getAllArticles();
        List<Article> filtered = new ArrayList<>();
        for (Article a : all)
            if (status.equals(a.getStatus())) filtered.add(a);
        return filtered;
    }

    // ── Update article status ─────────────────────────────────────────────────
    public boolean updateStatus(String articleId, String newStatus) {
        try {
            String url  = BASE_URL + "/" + articleId + "?updateMask.fieldPaths=status";
            String body = "{\"fields\":{\"status\":{\"stringValue\":\"" + newStatus + "\"}}}";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200;
        } catch (Exception e) {
            System.out.println("[ArticleService] Update error: " + e.getMessage());
            return false;
        }
    }

    // ── Build JSON ────────────────────────────────────────────────────────────
    private String buildArticleJson(Article a) {
        return "{"
            + "\"fields\":{"
            + "\"title\":"       + sv(a.getTitle())   + ","
            + "\"content\":"     + sv(a.getContent())  + ","
            + "\"botId\":"       + sv(a.getBotId())    + ","
            + "\"botName\":"     + sv(a.getBotName())  + ","
            + "\"topic\":"       + sv(a.getTopic())    + ","
            + "\"status\":"      + sv(a.getStatus())   + ","
            + "\"createdAt\":"   + "{\"integerValue\":\"" + a.getCreatedAt() + "\"}"
            + "}}";
    }

    private String sv(String val) {
        // Escape quotes/newlines for JSON
        String safe = val == null ? "" : val
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "");
        return "{\"stringValue\":\"" + safe + "\"}";
    }

    // ── Parse Firestore response ──────────────────────────────────────────────
    private void parseArticles(String json, List<Article> list) {
        // Split on document entries
        String[] docs = json.split("\"name\":");
        for (int i = 1; i < docs.length; i++) {
            try {
                String block = docs[i];
                // Extract doc ID from path
                String namePart = block.substring(1, block.indexOf("\"", 1));
                String id = namePart.substring(namePart.lastIndexOf("/") + 1);

                String title    = extractField(block, "title");
                String content  = extractField(block, "content");
                String botId    = extractField(block, "botId");
                String botName  = extractField(block, "botName");
                String topic    = extractField(block, "topic");
                String status   = extractField(block, "status");

                Article art = new Article(id, title, content, botId, botName, topic, status,
                    System.currentTimeMillis());
                list.add(art);
            } catch (Exception ignored) {}
        }
    }

    private String extractField(String block, String key) {
        // Matches both "key":{"stringValue":"val"} with or without spaces
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
}

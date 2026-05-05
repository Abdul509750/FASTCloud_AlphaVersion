import java.net.URI;
import java.net.http.*;
import java.util.*;

public class ArticleService {

    private static final String PROJECT_ID = "aqalnama-9d5f2";
    private static final String BASE_URL   =
        "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID +
        "/databases/(default)/documents/articles";

    private final HttpClient http = HttpClient.newHttpClient();

    // Saving the new article 
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

    // Fetch all the articles from the database  
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

    //  Fetching articles by status 
    public List<Article> getArticlesByStatus(String status) {
        List<Article> all = getAllArticles();
        List<Article> filtered = new ArrayList<>();
        for (Article a : all)
            if (status.equals(a.getStatus())) filtered.add(a);
        return filtered;
    }

    public List<Article> getPublishedArticles() {
        List<Article> published = getArticlesByStatus("PUBLISHED");
        published.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        return published;
    }

    // Update the article status 
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

    //  Deletion of the article 
    public boolean deleteArticle(String articleId) {
        if (articleId == null || articleId.isBlank()) return false;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + articleId))
                .DELETE()
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) return true;
            System.out.println("[ArticleService] Delete failed: " + res.body());
            return false;
        } catch (Exception e) {
            System.out.println("[ArticleService] Delete error: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteArticlesByBot(String botId) {
        if (botId == null || botId.isBlank()) return false;
        boolean allDeleted = true;
        for (Article article : getAllArticles()) {
            if (botId.equals(article.getBotId())) {
                allDeleted &= deleteArticle(article.getId());
            }
        }
        return allDeleted;
    }

    // Build JSON for the article to fee it to the database
    /* basically our database will receive the article in the form of the json */ 
    private String buildArticleJson(Article a) {
        return "{"
            + "\"fields\":{"
            + "\"title\":"       + sv(a.getTitle())   + ","
            + "\"content\":"     + sv(a.getContent())  + ","
            + "\"botId\":"       + sv(a.getBotId())    + ","
            + "\"botName\":"     + sv(a.getBotName())  + ","
            + "\"topic\":"       + sv(a.getTopic())    + ","
            + "\"status\":"      + sv(a.getStatus())   + ","
            + "\"createdAt\":"   + "{\"integerValue\":\"" + a.getCreatedAt() + "\"},"
            + "\"ratingSum\":"   + "{\"integerValue\":\"" + a.getRatingSum() + "\"},"
            + "\"ratingCount\":" + "{\"integerValue\":\"" + a.getRatingCount() + "\"},"
            + "\"reportCount\":" + "{\"integerValue\":\"" + a.getReportCount() + "\"}"
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

    // parsing the reponse from firestore
    private void parseArticles(String json, List<Article> list) {
        int docStart = findNextDocument(json, "/documents/articles/", 0);
        while (docStart != -1) {
            try {
                int docPathStart = json.indexOf(":", docStart) + 1;
                docPathStart = skipWhitespace(json, docPathStart);
                String docPath = readJsonString(json, docPathStart + 1);
                int docPathEnd = findStringEnd(json, docPathStart + 1);
                int nextDoc = findNextDocument(json, "/documents/articles/", docPathEnd + 1);
                String block = json.substring(docStart, nextDoc == -1 ? json.length() : nextDoc);

                String id = docPath.substring(docPath.lastIndexOf("/") + 1);

                String title    = extractField(block, "title");
                String content  = extractField(block, "content");
                String botId    = extractField(block, "botId");
                String botName  = extractField(block, "botName");
                String topic    = extractField(block, "topic");
                String status   = extractField(block, "status");
                long createdAt  = parseLong(block, "createdAt");
                int ratingSum   = (int) parseLong(block, "ratingSum");
                int ratingCount = (int) parseLong(block, "ratingCount");
                int reportCount = (int) parseLong(block, "reportCount");

                Article art = new Article(id, title, content, botId, botName, topic, status, createdAt);
                art.setRatingSum(ratingSum);
                art.setRatingCount(ratingCount);
                art.setReportCount(reportCount);
                list.add(art);
            } catch (Exception ignored) {}
            docStart = findNextDocument(json, "/documents/articles/", docStart + 1);
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

    public boolean rateArticle(Article article, int rating) {
        int safeRating = Math.max(1, Math.min(5, rating));
        int newSum = article.getRatingSum() + safeRating;
        int newCount = article.getRatingCount() + 1;
        try {
            String url = BASE_URL + "/" + article.getId()
                + "?updateMask.fieldPaths=ratingSum&updateMask.fieldPaths=ratingCount";
            String body = "{\"fields\":{"
                + "\"ratingSum\":{\"integerValue\":\"" + newSum + "\"},"
                + "\"ratingCount\":{\"integerValue\":\"" + newCount + "\"}"
                + "}}";
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                article.setRatingSum(newSum);
                article.setRatingCount(newCount);
                return true;
            }
            System.out.println("[ArticleService] Rating failed: " + res.body());
            return false;
        } catch (Exception e) {
            System.out.println("[ArticleService] Rating error: " + e.getMessage());
            return false;
        }
    }

    public boolean reportArticle(Article article, String reporter, String reason) {
        int newCount = article.getReportCount() + 1;
        try {
            String reportId = "report_" + System.currentTimeMillis();
            String reportsUrl = "https://firestore.googleapis.com/v1/projects/" + PROJECT_ID
                + "/databases/(default)/documents/articleReports/" + reportId;
            String reportBody = "{\"fields\":{"
                + "\"articleId\":" + sv(article.getId()) + ","
                + "\"title\":" + sv(article.getTitle()) + ","
                + "\"botId\":" + sv(article.getBotId()) + ","
                + "\"botName\":" + sv(article.getBotName()) + ","
                + "\"reporter\":" + sv(reporter) + ","
                + "\"reason\":" + sv(reason) + ","
                + "\"createdAt\":{\"integerValue\":\"" + System.currentTimeMillis() + "\"}"
                + "}}";
            HttpRequest reportReq = HttpRequest.newBuilder()
                .uri(URI.create(reportsUrl))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(reportBody))
                .build();
            HttpResponse<String> reportRes = http.send(reportReq, HttpResponse.BodyHandlers.ofString());
            if (reportRes.statusCode() != 200) {
                System.out.println("[ArticleService] Report save failed: " + reportRes.body());
                return false;
            }

            String articleUrl = BASE_URL + "/" + article.getId() + "?updateMask.fieldPaths=reportCount";
            String articleBody = "{\"fields\":{\"reportCount\":{\"integerValue\":\"" + newCount + "\"}}}";
            HttpRequest articleReq = HttpRequest.newBuilder()
                .uri(URI.create(articleUrl))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(articleBody))
                .build();
            HttpResponse<String> articleRes = http.send(articleReq, HttpResponse.BodyHandlers.ofString());
            if (articleRes.statusCode() == 200) {
                article.setReportCount(newCount);
                return true;
            }
            System.out.println("[ArticleService] Report count update failed: " + articleRes.body());
            return false;
        } catch (Exception e) {
            System.out.println("[ArticleService] Report error: " + e.getMessage());
            return false;
        }
    }
}

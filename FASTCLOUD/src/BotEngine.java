import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

public class BotEngine {


    private static final String GEMINI_API_KEY = "AIzaSyBAdjJKEHiVjy7rg4_co93tEVPE2vDqV4M";
    // ══════════════════════════════════════════════════════════════════════════

    private static final String GEMINI_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/" +
    "gemini-2.0-flash:generateContent?key=" + GEMINI_API_KEY;

    private final ArticleService articleService = new ArticleService();
    private final BotService     botService     = new BotService();

    // botId → its scheduled future (so we can cancel/pause it)
    private final Map<String, ScheduledFuture<?>> scheduledBots = new ConcurrentHashMap<>();
    private final ScheduledExecutorService        scheduler     =
        Executors.newScheduledThreadPool(10);

    // Listener so AdminDashboard can refresh when a new article arrives
    public interface OnArticleWritten {
        void onArticle(BotPersona bot, Article article);
    }
    private OnArticleWritten listener;
    public void setOnArticleWritten(OnArticleWritten l) { this.listener = l; }

    // =========================================================================
    //  START a bot on its schedule
    // =========================================================================
    public void startBot(BotPersona bot) {
        if (scheduledBots.containsKey(bot.getId())) {
            System.out.println("[BotEngine] Bot already running: " + bot.getName());
            return;
        }
        long delayMs    = bot.getIntervalMins() * 60_000L;
        // Fire once immediately then on interval
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> runBot(bot), 0, delayMs, TimeUnit.MILLISECONDS
        );
        scheduledBots.put(bot.getId(), future);
        System.out.println("[BotEngine] Started: " + bot.getName()
            + " every " + bot.getIntervalMins() + " min");
    }

    // =========================================================================
    //  PAUSE / RESUME
    // =========================================================================
    public void pauseBot(BotPersona bot) {
        ScheduledFuture<?> f = scheduledBots.remove(bot.getId());
        if (f != null) f.cancel(false);
        botService.updateBotStatus(bot.getId(), "PAUSED");
        System.out.println("[BotEngine] Paused: " + bot.getName());
    }

    public void resumeBot(BotPersona bot) {
        bot.setStatus("ACTIVE");
        botService.updateBotStatus(bot.getId(), "ACTIVE");
        startBot(bot);
    }

    // =========================================================================
    //  STOP all bots (call on app close)
    // =========================================================================
    public void stopAll() {
        for (ScheduledFuture<?> f : scheduledBots.values()) f.cancel(false);
        scheduledBots.clear();
        scheduler.shutdownNow();
        System.out.println("[BotEngine] All bots stopped.");
    }

    public boolean isRunning(String botId) {
        return scheduledBots.containsKey(botId);
    }

    // =========================================================================
    //  CORE: bot wakes up and writes an article via Gemini
    // =========================================================================
    private void runBot(BotPersona bot) {
        System.out.println("[BotEngine] Bot woke up: " + bot.getName()
            + " | Topic: " + bot.getTopic());
        try {
            // 1. Ask Gemini to write an article
            String prompt = buildPrompt(bot);
            String raw    = callGemini(prompt);
            if (raw == null || raw.isEmpty()) {
                System.out.println("[BotEngine] Gemini returned empty for " + bot.getName());
                return;
            }

            // 2. Parse title + content from Gemini response
            String[] parsed  = parseTitleAndContent(raw);
            String   title   = parsed[0];
            String   content = parsed[1];

            // 3. Save as DRAFT article in Firestore
            Article article = new Article(
                null, title, content,
                bot.getId(), bot.getName(), bot.getTopic(),
                "DRAFT", System.currentTimeMillis()
            );
            String newId = articleService.saveArticle(article);
            if (newId == null) return;
            article.setId(newId);

            // 4. Update bot stats in Firestore
            int newCount = bot.getArticleCount() + 1;
            bot.setArticleCount(newCount);
            bot.setLastRun(System.currentTimeMillis());
            botService.updateBotAfterWrite(bot.getId(), newCount);

            // 5. Notify AdminDashboard on JavaFX thread
            if (listener != null) {
                javafx.application.Platform.runLater(() ->
                    listener.onArticle(bot, article));
            }

            System.out.println("[BotEngine] Article saved: " + title);

        } catch (Exception e) {
            System.out.println("[BotEngine] Error in " + bot.getName() + ": " + e.getMessage());
        }
    }

    // =========================================================================
    //  BUILD GEMINI PROMPT
    // =========================================================================
    private String buildPrompt(BotPersona bot) {
        return "You are " + bot.getName() + ", an AI author for Aqalnama — "
            + "an AI-driven encyclopedia. Write a detailed, well-structured "
            + "encyclopedia article about a specific topic within the field of "
            + bot.getTopic() + ". "
            + "Writing style: " + bot.getStyle() + ". "
            + "Format your response EXACTLY as:\n"
            + "TITLE: [article title here]\n"
            + "CONTENT:\n[full article content here, minimum 200 words, "
            + "structured with clear paragraphs, no markdown symbols like ** or #]";
    }

    // =========================================================================
    //  CALL GEMINI API
    // =========================================================================
    private String callGemini(String prompt) throws Exception {
        String body = "{"
            + "\"contents\":[{"
            + "\"parts\":[{\"text\":" + jsonString(prompt) + "}]"
            + "}],"
            + "\"generationConfig\":{"
            + "\"temperature\":0.7,"
            + "\"maxOutputTokens\":1024"
            + "}}";

        HttpClient  client = HttpClient.newHttpClient();
        HttpRequest req    = HttpRequest.newBuilder()
            .uri(URI.create(GEMINI_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("[BotEngine] Gemini status: " + res.statusCode());

        if (res.statusCode() != 200) {
            System.out.println("[BotEngine] Gemini error: " + res.body());
            return null;
        }

        // Extract text from Gemini JSON response
        return extractGeminiText(res.body());
    }

    private String extractGeminiText(String json) {
        // Gemini response: ...{"text":"..."}...
        String marker = "\"text\":\"";
        int start = json.indexOf(marker);
        if (start == -1) return null;
        start += marker.length();
        // Find closing quote that isn't escaped
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == 'n')       { sb.append('\n'); i++; }
                else if (next == '"')  { sb.append('"');  i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else sb.append(c);
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    // =========================================================================
    //  PARSE TITLE + CONTENT FROM GEMINI OUTPUT
    // =========================================================================
    private String[] parseTitleAndContent(String raw) {
        String title   = "Untitled Article";
        String content = raw;

        // Look for TITLE: line
        int tIdx = raw.indexOf("TITLE:");
        if (tIdx != -1) {
            int tEnd = raw.indexOf("\n", tIdx);
            if (tEnd != -1) {
                title = raw.substring(tIdx + 6, tEnd).trim();
            }
        }

        // Look for CONTENT: line
        int cIdx = raw.indexOf("CONTENT:");
        if (cIdx != -1) {
            content = raw.substring(cIdx + 8).trim();
        } else if (tIdx != -1) {
            // Fallback: everything after the title line
            int tEnd = raw.indexOf("\n", tIdx);
            if (tEnd != -1) content = raw.substring(tEnd).trim();
        }

        return new String[]{title, content};
    }

    // =========================================================================
    //  JSON helper
    // =========================================================================
    private String jsonString(String s) {
        return "\"" + s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "") + "\"";
    }
}

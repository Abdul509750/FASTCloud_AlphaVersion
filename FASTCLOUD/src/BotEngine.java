import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class BotEngine {

    private static final String GROQ_API_KEY = resolveGroqApiKey();
    private static final String GROQ_URL     =
        "https://api.groq.com/openai/v1/chat/completions";

    private final ArticleService articleService = new ArticleService();
    private final BotService     botService     = new BotService();

    private final Map<String, ScheduledFuture<?>> scheduledBots = new ConcurrentHashMap<>();
    private final ScheduledExecutorService        scheduler     =
        Executors.newScheduledThreadPool(10);

    public interface OnArticleWritten {
        void onArticle(BotPersona bot, Article article);
    }
    private OnArticleWritten listener;
    public void setOnArticleWritten(OnArticleWritten l) { this.listener = l; }

    // =========================================================================
    //  START a bot on its schedule i.e every 5min or every 1min etccc
    // =========================================================================
    public void startBot(BotPersona bot) {
        if (scheduledBots.containsKey(bot.getId())) {
            System.out.println("[BotEngine] Bot already running: " + bot.getName());
            return;
        }
        int  intervalMins = Math.max(1, bot.getIntervalMins());
        long delayMs      = intervalMins * 60_000L;
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> runBot(bot), 0, delayMs, TimeUnit.MILLISECONDS
        );
        scheduledBots.put(bot.getId(), future);
        System.out.println("[BotEngine] Started: " + bot.getName()
            + " every " + intervalMins + " min");
    }

    // =========================================================================
    //  PAUSE or RESUME the botss
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
    //  STOP all bots
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
    // bot wakes up and writes an article via Groq api request we feed
    // =========================================================================
    private void runBot(BotPersona bot) {
        System.out.println("[BotEngine] Bot woke up: " + bot.getName()
            + " | Topic: " + bot.getTopic());
        try {
            String prompt = buildPrompt(bot);
            String raw    = callGroq(prompt);
            if (raw == null || raw.isEmpty()) {
                System.out.println("[BotEngine] No article produced for "
                    + bot.getName() + ". Check Groq status/error above.");
                return;
            }

            String[] parsed  = parseTitleAndContent(raw);
            String   title   = parsed[0];
            String   content = parsed[1];

            Article article = new Article(
                null, title, content,
                bot.getId(), bot.getName(), bot.getTopic(),
                "DRAFT", System.currentTimeMillis()
            );
            String newId = articleService.saveArticle(article);
            if (newId == null) return;
            article.setId(newId);

            int newCount = bot.getArticleCount() + 1;
            bot.setArticleCount(newCount);
            bot.setLastRun(System.currentTimeMillis());
            botService.updateBotAfterWrite(bot.getId(), newCount);

            if (listener != null) {
                javafx.application.Platform.runLater(() ->
                    listener.onArticle(bot, article));
            }

            System.out.println("[BotEngine] Article saved: " + title);

        } catch (Exception e) {
            System.out.println("[BotEngine] Error in " + bot.getName()
                + ": " + e.getMessage());
        }
    }

    // =========================================================================
    //  BUILD PROMPT for the bg llm
    // =========================================================================
    private String buildPrompt(BotPersona bot) {
        return "You are " + bot.getName() + ", an AI author for Aqalnama "
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
    //  calling the  GROQ API
    // =========================================================================
    private String callGroq(String prompt) throws Exception {
        if (GROQ_API_KEY == null || GROQ_API_KEY.isBlank()) {
            System.out.println("[BotEngine] Missing FASTCLOUD_API_KEY in .env");
            return null;
        }

        String body = "{"
            + "\"model\":\"llama-3.1-8b-instant\","
            + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(prompt) + "}],"
            + "\"max_tokens\":1024,"
            + "\"temperature\":0.7"
            + "}";

        HttpClient  client = HttpClient.newHttpClient();
        HttpRequest req    = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + GROQ_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("[BotEngine] Groq status: " + res.statusCode());

        if (res.statusCode() != 200) {
            System.out.println("[BotEngine] Groq error: " + res.body());
            if (res.statusCode() == 429) {
                System.out.println("[BotEngine] Groq rate limited — retrying in 15s...");
                Thread.sleep(15_000);
                res = client.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[BotEngine] Groq retry status: " + res.statusCode());
                if (res.statusCode() == 200) return extractGroqText(res.body());
                System.out.println("[BotEngine] Groq retry failed: " + res.body());
            }
            return null;
        }

        return extractGroqText(res.body());
    }

    // =========================================================================
    //  EXTRACT TEXT FROM GROQ RESPONSE
    //  Groq returns: {"choices":[{"message":{"role":"assistant","content":"..."}}]}
    // =========================================================================
    private String extractGroqText(String json) {
        String key = "\"content\":";
        int idx = json.indexOf(key);
        if (idx == -1) {
            System.out.println("[BotEngine] Could not find content in Groq response.");
            return null;
        }
        int valStart = skipWhitespace(json, idx + key.length());
        if (valStart >= json.length() || json.charAt(valStart) != '"') return null;
        String text = readJsonString(json, valStart + 1);
        return text == null ? null : text.trim();
    }

    // =========================================================================
    //  PARSE TITLE + CONTENT
    // =========================================================================
    private String[] parseTitleAndContent(String raw) {
        String title   = "Untitled Article";
        String content = raw;

        int tIdx = raw.indexOf("TITLE:");
        if (tIdx != -1) {
            int tEnd = raw.indexOf("\n", tIdx);
            if (tEnd != -1) title = raw.substring(tIdx + 6, tEnd).trim();
        }

        int cIdx = raw.indexOf("CONTENT:");
        if (cIdx != -1) {
            content = raw.substring(cIdx + 8).trim();
        } else if (tIdx != -1) {
            int tEnd = raw.indexOf("\n", tIdx);
            if (tEnd != -1) content = raw.substring(tEnd).trim();
        }

        return new String[]{title, content};
    }

    // =========================================================================
    //  JSON HELPERS
    // =========================================================================
    private String jsonString(String s) {
        return "\"" + s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "") + "\"";
    }

    private static int skipWhitespace(String s, int index) {
        while (index < s.length() && Character.isWhitespace(s.charAt(index))) index++;
        return index;
    }

    private static String readJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if      (next == 'n')  { sb.append('\n'); i++; }
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

    // =========================================================================
    //  RESOLVE API KEY FROM .env OR ENVIRONMENT
    // =========================================================================
    private static String resolveGroqApiKey() {
        String envKey = System.getenv("FASTCLOUD_API_KEY");
        if (envKey != null && !envKey.isBlank()) return envKey.trim();

        Path[] candidates = {
            Path.of(".env"),
            Path.of("FASTCLOUD", ".env")
        };
        for (Path p : candidates) {
            try {
                if (!Files.exists(p)) continue;
                for (String line : Files.readAllLines(p)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq == -1) continue;
                    String key = trimmed.substring(0, eq).trim();
                    if (!"FASTCLOUD_API_KEY".equals(key)) continue;
                    String value = trimmed.substring(eq + 1).trim();
                    if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    if (!value.isBlank()) return value;
                }
            } catch (Exception e) {
                System.out.println("[BotEngine] Could not read " + p
                    + ": " + e.getMessage());
            }
        }
        return "";
    }
}
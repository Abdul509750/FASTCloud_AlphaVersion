public class Article {
    private String id;
    private String title;
    private String content;
    private String botId;
    private String botName;
    private String topic;
    private String status; // DRAFT, AUTHENTICATED, PUBLISHED
    private long   createdAt;

    public Article() {}

    public Article(String id, String title, String content,
                   String botId, String botName, String topic,
                   String status, long createdAt) {
        this.id        = id;
        this.title     = title;
        this.content   = content;
        this.botId     = botId;
        this.botName   = botName;
        this.topic     = topic;
        this.status    = status;
        this.createdAt = createdAt;
    }

    public String getId()        { return id; }
    public String getTitle()     { return title; }
    public String getContent()   { return content; }
    public String getBotId()     { return botId; }
    public String getBotName()   { return botName; }
    public String getTopic()     { return topic; }
    public String getStatus()    { return status; }
    public long   getCreatedAt() { return createdAt; }

    public void setId(String id)           { this.id = id; }
    public void setTitle(String title)     { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setStatus(String status)   { this.status = status; }
}

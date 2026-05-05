public class Article {
    private String id;
    private String title;
    private String content;
    private String botId;
    private String botName;
    private String topic;
    private String status; // DRAFT, AUTHENTICATED, PUBLISHED
    private long   createdAt;
    private int    ratingSum;
    private int    ratingCount;
    private int    reportCount;

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
        this.ratingSum = 0;
        this.ratingCount = 0;
        this.reportCount = 0;
    }

    public String getId()        { return id; }
    public String getTitle()     { return title; }
    public String getContent()   { return content; }
    public String getBotId()     { return botId; }
    public String getBotName()   { return botName; }
    public String getTopic()     { return topic; }
    public String getStatus()    { return status; }
    public long   getCreatedAt() { return createdAt; }
    public int    getRatingSum() { return ratingSum; }
    public int    getRatingCount() { return ratingCount; }
    public int    getReportCount() { return reportCount; }
    public double getAverageRating() {
        return ratingCount == 0 ? 0.0 : ratingSum / (double) ratingCount;
    }

    public void setId(String id)           { this.id = id; }
    public void setTitle(String title)     { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setStatus(String status)   { this.status = status; }
    public void setRatingSum(int ratingSum) { this.ratingSum = ratingSum; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public void setReportCount(int reportCount) { this.reportCount = reportCount; }
}

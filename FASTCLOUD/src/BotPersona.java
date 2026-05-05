public class BotPersona {
    private String id;
    private String name;
    private String topic;
    private String style;       // e.g. "academic", "casual", "technical"
    private int    intervalMins; // how often bot writes (in minutes)
    private String status;      // ACTIVE, PAUSED, STOPPED
    private long   lastRun;     // epoch ms of last article write
    private int    articleCount;
    private boolean flagged;
    private String  flagReason;

    public BotPersona() {}

    public BotPersona(String id, String name, String topic,
                      String style, int intervalMins) {
        this.id           = id;
        this.name         = name;
        this.topic        = topic;
        this.style        = style;
        this.intervalMins = intervalMins;
        this.status       = "ACTIVE";
        this.lastRun      = 0;
        this.articleCount = 0;
        this.flagged      = false;
        this.flagReason   = "";
    }

    public String getId()           { return id; }
    public String getName()         { return name; }
    public String getTopic()        { return topic; }
    public String getStyle()        { return style; }
    public int    getIntervalMins() { return intervalMins; }
    public String getStatus()       { return status; }
    public long   getLastRun()      { return lastRun; }
    public int    getArticleCount() { return articleCount; }
    public boolean isFlagged()      { return flagged; }
    public String getFlagReason()   { return flagReason; }

    public void setId(String id)               { this.id = id; }
    public void setStatus(String status)       { this.status = status; }
    public void setLastRun(long lastRun)       { this.lastRun = lastRun; }
    public void setArticleCount(int n)         { this.articleCount = n; }
    public void setIntervalMins(int mins)      { this.intervalMins = mins; }
    public void setFlagged(boolean flagged)    { this.flagged = flagged; }
    public void setFlagReason(String reason)   { this.flagReason = reason; }
}

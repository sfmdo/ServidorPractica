package Models;

public class GlobalMessage {
    private String fromId;
    private String toId;
    private String text;
    private long timestamp;

    public GlobalMessage(String fromId, String toId, String text) {
        this.fromId = fromId;
        this.toId = toId;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getFromId() { return fromId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
}
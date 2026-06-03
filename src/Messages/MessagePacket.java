/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Messages;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sfmdo
 */
public class MessagePacket {
    private String type;
    private String action;
    private String token;
    private Map<String, Object> payload = new HashMap<>();;
    
    public String getParam(String key) {
        Object value = payload.get(key);
        return (value != null) ? value.toString() : null;
    }
    
    public Integer getIntParam(String key) {
        Object value = payload.get(key);
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof Integer) return (Integer) value;
        return null;
    }
    
    public MessagePacket add(String key, Object value) {
        this.payload.put(key, value);
        return this;                  
    }
    
    public static MessagePacket response(String action, String token) {
        MessagePacket p = new MessagePacket();
        p.setType("RESPONSE");
        p.setAction(action);
        p.setToken(token);
        return p;
    }
    
    public static MessagePacket event(String action) {
        MessagePacket p = new MessagePacket();
        p.setType("EVENT");
        p.setAction(action);
        return p;
    }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    
}

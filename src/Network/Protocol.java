/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Network;

/**
 *
 * @author sfmdo
 */
public class Protocol {
    // Auth
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    
    // Amigos
    public static final String FRIEND_REQUEST = "FRIEND_REQ";
    public static final String FRIEND_ACCEPT = "FRIEND_ACCEPT";
    public static final String FRIEND_DECLINE = "FRIEND_DECLINE";
    public static final String FRIEND_MSG = "FRIEND_MSG";
    public static final String FRIEND_HISTORY = "FRIEND_HISTORY";
    
    // Grupos
    public static final String GROUP_CREATE = "GROUP_CREATE";
    public static final String GROUP_INVITE = "GROUP_INVITE";
    public static final String GROUP_INVITE_ACCEPT = "GROUP_ACCEPT";
    public static final String GROUP_INVITE_DECLINE = "GROUP_DECLINE";
    public static final String GROUP_MSG = "GROUP_MSG";
    public static final String GROUP_LEAVE = "GROUP_LEAVE";
    public static final String GROUP_HISTORY = "GROUP_HISTORY";
    
    // Especiales
    public static final String GLOBAL_MSG = "GLOBAL_MSG"; // 1 a 1 volátil
    public static final String GLOBAL_FETCH_HISTORY = "GLOBAL_FETCH_HISTORY";
    public static final String FETCH_NOTIFICATIONS = "NOTIF_REQ";
    public static final String NOTIFICATION = "NOTIFICATION";
    public static final String FETCH_USERS = "FETCH_USERS";
    
    //Actualizaciones
    public static final String GLOBAL_CHAT_UPDATE = "GLOBAL_CHAT_UPDATE";
    public static final String FRIEND_CHAT_UPDATE = "FRIEND_CHAT_UPDATE";
    public static final String GROUP_CHAT_UPDATE = "GROUP_CHAT_UPDATE";
}

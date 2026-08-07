package com.neopick.port.messaging;

public interface WebSocketSessionManager {

    void registerSession(String userId, String sessionId);

    void removeSession(String sessionId);

    void sendToUser(String userId, Object message);

    boolean isUserOnline(String userId);
}

package org.kimwanyi.sacco.security.session;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private final Map<Long, UserSession> sessions = new HashMap<>();

    public void createSession(UserSession session){
        sessions.put(session.getUserId(), session);
    }

    public UserSession getSession(Long userId){
        return sessions.get(userId);
    }

    public void removeSession(Long userId){
        sessions.remove(userId);
    }
}

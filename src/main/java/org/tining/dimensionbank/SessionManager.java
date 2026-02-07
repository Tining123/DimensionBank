package org.tining.dimensionbank;

import java.util.HashMap;
import java.util.Map;

public final class SessionManager {

    private final Map<String, MenuSession> sessions = new HashMap<String, MenuSession>();

    public MenuSession get(String playerName) {
        MenuSession s = sessions.get(playerName);
        if (s == null) {
            s = new MenuSession();
            sessions.put(playerName, s);
        }
        return s;
    }

    public void clear(String playerName) {
        sessions.remove(playerName);
    }
}

package com.neopick.adapter.web.websocket;

import java.security.Principal;

/**
 * Simple Principal implementation for STOMP WebSocket authentication.
 * Carries the authenticated user's ID through the WebSocket session.
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StompPrincipal that)) {
            return false;
        }
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "StompPrincipal{name='" + name + "'}";
    }
}

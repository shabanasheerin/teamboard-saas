package com.teamboard.realtime;

import java.security.Principal;

/** Who is connected on a WebSocket. name = user id, so messages can be sent to one person. */
public record StompUser(String name, Long orgId) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}

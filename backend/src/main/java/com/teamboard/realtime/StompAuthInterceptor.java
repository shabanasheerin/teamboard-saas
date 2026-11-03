package com.teamboard.realtime;

import com.teamboard.security.AuthUser;
import com.teamboard.security.AuthUserService;
import com.teamboard.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Security for WebSockets:
 * - CONNECT must carry a valid access token
 * - a person may only subscribe to their own notifications and to their own organization's task topic
 * - clients are not allowed to publish anything
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final AuthUserService authUserService;

    public StompAuthInterceptor(JwtService jwtService, AuthUserService authUserService) {
        this.jwtService = jwtService;
        this.authUserService = authUserService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            AuthUser user = authenticate(accessor.getFirstNativeHeader("Authorization"));
            accessor.setUser(new StompUser(String.valueOf(user.id()), user.orgId()));

        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();
            if (!(principal instanceof StompUser user) || destination == null) {
                throw new MessagingException("Not authenticated");
            }
            boolean allowed = destination.equals("/topic/org/" + user.orgId() + "/tasks")
                    || destination.startsWith("/user/queue/");
            if (!allowed) {
                throw new MessagingException("Subscription not allowed");
            }

        } else if (StompCommand.SEND.equals(command)) {
            throw new MessagingException("Sending messages is not allowed");
        }
        return message;
    }

    private AuthUser authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Missing token");
        }
        try {
            String email = jwtService.parse(header.substring(7)).getSubject();
            AuthUser user = authUserService.findByEmail(email);
            if (user == null || !user.active() || user.mustChangePassword()) {
                throw new MessagingException("Account not allowed");
            }
            return user;
        } catch (JwtException | IllegalArgumentException e) {
            throw new MessagingException("Invalid token");
        }
    }
}

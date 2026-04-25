package com.a3solutions.fsm.realtime;

import com.a3solutions.fsm.auth.CustomUserDetailsService;
import com.a3solutions.fsm.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author samuelkawuma
 * @package com.a3solutions.fsm.realtime
 * @project A3 Field Service Management Backend
 * @date 4/14/26
 */

/**
 * Validates JWT access tokens carried in the STOMP CONNECT frame so dashboard
 * subscriptions use the same auth model as the REST API.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public WebSocketAuthChannelInterceptor(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = resolveAuthorizationHeader(accessor);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException("Missing WebSocket bearer token.");
            }

            String token = authHeader.substring(7);
            String email;

            try {
                email = jwtService.extractUsername(token);
            } catch (Exception ex) {
                throw new AccessDeniedException("Invalid WebSocket token.");
            }

            if (!jwtService.isAccessToken(token)) {
                throw new AccessDeniedException("WebSocket requires an access token.");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(token, userDetails)) {
                throw new AccessDeniedException("Expired or invalid WebSocket token.");
            }

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            ));
        }

        if ((StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand()))
                && accessor.getUser() == null) {
            throw new AccessDeniedException("WebSocket authentication is required.");
        }

        return message;
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header != null && !header.isBlank()) {
            return header;
        }

        header = accessor.getFirstNativeHeader("authorization");
        if (header != null && !header.isBlank()) {
            return header;
        }

        return null;
    }
}

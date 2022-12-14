package com.vaadin.flow.spring.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CustomWebSocket implements WebSocketConfigurer {

    public static final String WEBSOCKET_URL = "/customWebSocket";

    public static final String WEBSOCKET_RESPONSE_TEXT = "This is a response from Web Socket!";

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler() {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) {
            }

            @Override
            public void handleMessage(WebSocketSession session,
                    WebSocketMessage<?> message) throws Exception {
                session.sendMessage(new TextMessage(WEBSOCKET_RESPONSE_TEXT));
            }

            @Override
            public void handleTransportError(WebSocketSession session,
                    Throwable exception) {

            }

            @Override
            public void afterConnectionClosed(WebSocketSession session,
                    CloseStatus closeStatus) {

            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, WEBSOCKET_URL);
    }

}

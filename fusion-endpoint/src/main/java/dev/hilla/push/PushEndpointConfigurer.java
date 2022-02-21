package dev.hilla.push;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class PushEndpointConfigurer implements WebSocketConfigurer {
    private PushEndpointHandler pushEndpointHandler;

    public PushEndpointConfigurer(PushEndpointHandler pushEndpointHandler) {
        this.pushEndpointHandler = pushEndpointHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(pushEndpointHandler, "/hilla");
    }

}
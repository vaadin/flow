package com.vaadin.base.devserver.viteproxy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.ViteHandler;

public class ViteWebsocketConnection implements Listener {

    private Consumer<String> onMessage;

    public ViteWebsocketConnection(int port, String subProtocol,
            Consumer<String> onMessage) throws Exception {
        this.onMessage = onMessage;
        String wsHost = ViteHandler.DEV_SERVER_HOST.replace("http://", "ws://");
        URI uri = URI.create(wsHost + ":" + port + "/VAADIN/");
        WebSocket clientWebSocket = HttpClient.newHttpClient()
                .newWebSocketBuilder().subprotocols(subProtocol)
                .buildAsync(uri, this).get();
        getLogger().debug("Connecting to " + uri + " using the "
                + clientWebSocket.getSubprotocol() + " protocol");
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        getLogger().debug("Connected using the " + webSocket.getSubprotocol()
                + " protocol");
        Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data,
            boolean last) {
        // Message from Vite
        String msg = data.toString();
        getLogger().debug("Message from Vite: " + msg);
        onMessage.accept(msg);
        return Listener.super.onText(webSocket, data, last);
    }

    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}

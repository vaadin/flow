package dev.hilla.push;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import dev.hilla.EndpointInvoker;
import dev.hilla.push.messages.fromclient.AbstractServerMessage;
import dev.hilla.push.messages.fromclient.PushCloseMessage;
import dev.hilla.push.messages.fromclient.PushConnectMessage;
import dev.hilla.push.messages.toclient.AbstractClientMessage;
import dev.hilla.push.messages.toclient.ClientMessageComplete;
import dev.hilla.push.messages.toclient.ClientMessageError;
import dev.hilla.push.messages.toclient.ClientMessageUpdate;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@Component
public class PushEndpointHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private EndpointInvoker endpointInvoker;
    private Map<String, Disposable> closeHandlers = new ConcurrentHashMap<>();

    public PushEndpointHandler(ObjectMapper objectMapper,
            EndpointInvoker endpointInvoker) {
        this.objectMapper = objectMapper;
        this.endpointInvoker = endpointInvoker;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {
        super.afterConnectionEstablished(session);
        ConcurrentWebSocketSessionDecorator threadSafeSession = new ConcurrentWebSocketSessionDecorator(
                session, 60000, 100000);
        session.getAttributes().put(
                ConcurrentWebSocketSessionDecorator.class.getName(),
                threadSafeSession);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session,
            TextMessage textMessage) throws Exception {

        ConcurrentWebSocketSessionDecorator threadSafeSession = (ConcurrentWebSocketSessionDecorator) session
                .getAttributes()
                .get(ConcurrentWebSocketSessionDecorator.class.getName());
        String text = textMessage.getPayload();
        AbstractServerMessage message = objectMapper.readValue(text,
                AbstractServerMessage.class);
        if (message instanceof PushConnectMessage) {
            handleConnect((PushConnectMessage) message, threadSafeSession);
        } else if (message instanceof PushCloseMessage) {
            handleClose((PushCloseMessage) message, threadSafeSession);
        } else {
            throw new IllegalArgumentException(
                    "Unknown message type: " + message.getClass().getName());
        }
    }

    private void handleConnect(PushConnectMessage message,
            WebSocketSession threadSafeSession) throws Exception {
        if (endpointInvoker.getReturnType(message.getEndpointName(),
                message.getMethodName()) != Flux.class) {
            throw new IllegalArgumentException("Method "
                    + message.getEndpointName() + "/" + message.getMethodName()
                    + " is not a Flux method");
        }

        try {
            // FIXME roles through security context
            Flux<?> result = (Flux<?>) endpointInvoker.invoke(
                    message.getEndpointName(), message.getMethodName(),
                    message.getParams(), threadSafeSession.getPrincipal(),
                    role -> false);
            Disposable closeHandler = result.subscribe(item -> {
                try {
                    send(threadSafeSession,
                            new ClientMessageUpdate(message.getId(), item));
                } catch (IOException e) {
                    if (!isBrokenPipe(e)) {
                        throw new RuntimeException(e);
                    }
                    // TODO Disconnected
                }
            }, error -> {
                closeHandlers.remove(message.getId());
                try {
                    send(threadSafeSession,
                            new ClientMessageError(message.getId()));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    // TODO Disconnected
                }

                if (!isBrokenPipe(error)) {
                    error.printStackTrace();
                }
            }, () -> {
                closeHandlers.remove(message.getId());
                // when done
                try {
                    send(threadSafeSession,
                            new ClientMessageComplete(message.getId()));
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    // TODO Disconnected
                }
            });
            closeHandlers.put(message.getId(), closeHandler);
        } catch (Exception e) {
            // TODO SHould catch a lot of different things here
            throw e;
        }

    }

    private void send(WebSocketSession threadSafeSession,
            AbstractClientMessage message) throws IOException {
        threadSafeSession.sendMessage(
                new TextMessage(objectMapper.writeValueAsString(message)));
    }

    private void handleClose(PushCloseMessage message,
            ConcurrentWebSocketSessionDecorator threadSafeSession) {
        Disposable closeHandler = closeHandlers.remove(message.getId());
        if (closeHandler == null) {
            getLogger().warn("Trying to close an unknown flux");
            return;
        }
        closeHandler.dispose();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

    private boolean isBrokenPipe(Throwable e) {
        return e instanceof IOException && e.getMessage().equals("Broken pipe");
    }

}
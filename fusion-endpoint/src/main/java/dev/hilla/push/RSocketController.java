package dev.hilla.push;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.dependency.NpmPackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

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
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.publisher.Sinks.Many;

@Controller
@NpmPackage(value = "rsocket-websocket-client", version = "0.0.27")
@NpmPackage(value = "@types/rsocket-websocket-client", version = "0.0.4")
public class RSocketController {

    private final ObjectMapper objectMapper;
    private final EndpointInvoker endpointInvoker;
    private Map<String, Disposable> closeHandlers = new ConcurrentHashMap<>();

    public RSocketController(ObjectMapper objectMapper,
            EndpointInvoker endpointInvoker) {
        this.objectMapper = objectMapper;
        this.endpointInvoker = endpointInvoker;
    }

    @MessageMapping("rs")
    private Flux<AbstractClientMessage> requestResponse(
            Flux<AbstractServerMessage> request) {
        Many<AbstractClientMessage> response = Sinks.many().multicast()
                .directBestEffort();
        getLogger().info("Received request-response request: {}", request);
        Disposable disposable = request.subscribe(clientMessage -> {
            handleClientMessage(clientMessage, response);
        });
        request.doOnError(error -> {
            System.out.println(error);
        });
        request.doOnComplete(() -> {
            System.out.println("Complete");
        });
        request.doOnCancel(() -> {
            System.out.println("Cancel");
        });
        return response.asFlux();
    }

    private void handleClientMessage(AbstractServerMessage message,
            Many<AbstractClientMessage> response) {
        if (message instanceof PushConnectMessage) {
            try {
                handleConnect((PushConnectMessage) message, response);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else if (message instanceof PushCloseMessage) {
            handleClose((PushCloseMessage) message, response);
        } else {
            throw new IllegalArgumentException(
                    "Unknown message type: " + message.getClass().getName());
        }

    }

    private void handleClose(PushCloseMessage message,
            Many<AbstractClientMessage> response) {
    }

    private void handleConnect(PushConnectMessage message,
            Many<AbstractClientMessage> response) throws Exception {
        if (endpointInvoker.getReturnType(message.getEndpointName(),
                message.getMethodName()) != Flux.class) {
            IllegalArgumentException e = new IllegalArgumentException("Method "
                    + message.getEndpointName() + "/" + message.getMethodName()
                    + " is not a Flux method");
            response.tryEmitError(e);
            return;
        }

        // FIXME principal / roles through security context
        Flux<?> result = (Flux<?>) endpointInvoker.invoke(
                message.getEndpointName(), message.getMethodName(),
                message.getParams(), null, role -> false);
        Disposable closeHandler = result.subscribe(item -> {
            response.emitNext(new ClientMessageUpdate(message.getId(), item),
                    (signalType, emitResult) -> {
                        if (emitResult == EmitResult.FAIL_NON_SERIALIZED) {
                            // Two sends happened at the same time, just retry
                            return true;
                        }
                        System.err.println(
                                "failed to send update message: " + emitResult);
                        return false;
                        // TODO Disconnected?
                    });
        }, error -> {
            error.printStackTrace();
            closeHandlers.remove(message.getId());
            response.emitNext(new ClientMessageError(message.getId()),
                    (signalType, emitResult) -> {
                        if (emitResult == EmitResult.FAIL_NON_SERIALIZED) {
                            // Two sends happened at the same time, just retry
                            return true;
                        }
                        System.err.println(
                                "failed to send error message: " + emitResult);
                        return false;
                        // TODO Disconnected?
                    });
        }, () -> {
            closeHandlers.remove(message.getId());
            // when done
            response.emitNext(new ClientMessageComplete(message.getId()),
                    (signalType, emitResult) -> {
                        if (emitResult == EmitResult.FAIL_NON_SERIALIZED) {
                            // Two sends happened at the same time, just retry
                            return true;
                        }
                        System.err.println("failed to send complete message: "
                                + emitResult);
                        return false;
                        // TODO Disconnected?
                    });
        });
        closeHandlers.put(message.getId(), closeHandler);

    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}

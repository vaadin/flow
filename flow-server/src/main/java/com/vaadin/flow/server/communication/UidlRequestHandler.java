/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.server.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.SessionExpiredHandler;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.server.communication.ServerRpcHandler.ResynchronizationRequiredException;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonException;
import elemental.json.JsonObject;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Processes a UIDL request from the client.
 *
 * Uses {@link ServerRpcHandler} to execute client-to-server RPC invocations and
 * {@link UidlWriter} to write state changes and client RPC calls back to the
 * client.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRequestHandler extends SynchronizedRequestHandler
        implements SessionExpiredHandler {

    private AtomicReference<ServerRpcHandler> rpcHandler = new AtomicReference<>();

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return HandlerHelper.isRequestType(request, RequestType.UIDL);
    }

    /**
     * Creates the ServerRpcHandler to use.
     *
     * @return the ServerRpcHandler to use
     */
    protected ServerRpcHandler createRpcHandler() {
        return new ServerRpcHandler();
    }

    @Override
    public boolean synchronizedHandleRequest(VaadinSession session,
            VaadinRequest request, VaadinResponse response) throws IOException {
        String requestBody = SynchronizedRequestHandler
                .getRequestBody(request.getReader());
        Optional<ResponseWriter> responseWriter = synchronizedHandleRequest(
                session, request, response, requestBody);
        if (responseWriter.isPresent()) {
            responseWriter.get().writeResponse();
        }
        return responseWriter.isPresent();
    }

    @Override
    public boolean isReadAndWriteOutsideSessionLock() {
        return true;
    }

    @Override
    public Optional<ResponseWriter> synchronizedHandleRequest(
            VaadinSession session, VaadinRequest request,
            VaadinResponse response, String requestBody)
            throws IOException, UnsupportedOperationException {
        UI uI = session.getService().findUI(request);
        if (uI == null) {
            // This should not happen but it will if the UI has been closed. We
            // really don't want to see it in the server logs though
            return Optional.of(() -> commitJsonResponse(response,
                    VaadinService.createUINotFoundJSON(false)));
        }

        StringWriter stringWriter = new StringWriter();

        try {
            getRpcHandler(session).handleRpc(uI, requestBody, request);
            writeUidl(uI, stringWriter, false);
        } catch (JsonException e) {
            getLogger().error("Error writing JSON to response", e);
            // Refresh on client side
            return Optional.of(() -> writeRefresh(response));
        } catch (InvalidUIDLSecurityKeyException e) {
            getLogger().warn("Invalid security key received from {}",
                    request.getRemoteHost());
            // Refresh on client side
            return Optional.of(() -> writeRefresh(response));
        } catch (ResynchronizationRequiredException e) { // NOSONAR
            // Resync on the client side
            writeUidl(uI, stringWriter, true);
        } finally {
            stringWriter.close();
        }

        return Optional.of(
                () -> commitJsonResponse(response, stringWriter.toString()));
    }

    private void writeRefresh(VaadinResponse response) throws IOException {
        String json = VaadinService.createCriticalNotificationJSON(null, null,
                null, null);
        commitJsonResponse(response, json);
    }

    private static void writeUidl(UI ui, Writer writer, boolean resync)
            throws IOException {
        JsonObject uidl = new UidlWriter().createUidl(ui, false, resync);

        // some dirt to prevent cross site scripting
        String responseString = "for(;;);[" + uidl.toJson() + "]";
        writer.write(responseString);
    }

    private static final Logger getLogger() {
        return LoggerFactory.getLogger(UidlRequestHandler.class.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.vaadin.server.SessionExpiredHandler#handleSessionExpired(com.vaadin
     * .server.VaadinRequest, com.vaadin.server.VaadinResponse)
     */
    @Override
    public boolean handleSessionExpired(VaadinRequest request,
            VaadinResponse response) throws IOException {
        if (!HandlerHelper.isRequestType(request, RequestType.UIDL)) {
            return false;
        }
        VaadinService service = request.getService();
        service.writeUncachedStringResponse(response,
                JsonConstants.JSON_CONTENT_TYPE,
                VaadinService.createSessionExpiredJSON(false));

        return true;
    }

    private ServerRpcHandler getRpcHandler(VaadinSession session) {
        ServerRpcHandler handler = rpcHandler.get();
        if (handler == null) {
            rpcHandler.compareAndSet(null, createRpcHandler());
            handler = rpcHandler.get();
        }
        return handler;
    }

    /**
     * Commit the JSON response. We can't write immediately to the output stream
     * as we want to write only a critical notification if something goes wrong
     * during the response handling.
     *
     * @param response
     *            The response to write to
     * @param json
     *            The JSON to write
     * @throws IOException
     *             If there was an exception while writing to the output
     */
    public static void commitJsonResponse(VaadinResponse response, String json)
            throws IOException {
        response.setContentType(JsonConstants.JSON_CONTENT_TYPE);

        // Ensure that the browser does not cache UIDL responses.
        // iOS 6 Safari requires this (#9732)
        response.setHeader("Cache-Control", "no-cache");

        byte[] b = json.getBytes(UTF_8);
        response.setContentLength(b.length);

        OutputStream outputStream = response.getOutputStream();
        outputStream.write(b);
        // NOTE GateIn requires the buffers to be flushed to work
        outputStream.flush();
    }
}

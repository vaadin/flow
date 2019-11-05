/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.server.communication;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServletHelper;
import com.vaadin.flow.server.ServletHelper.RequestType;
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

/**
 * Processes a UIDL request from the client.
 *
 * Uses {@link ServerRpcHandler} to execute client-to-server RPC invocations and
 * {@link UidlWriter} to write state changes and client RPC calls back to the
 * client.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRequestHandler extends SynchronizedRequestHandler
        implements SessionExpiredHandler {

    private ServerRpcHandler rpcHandler;

    @Override
    protected boolean canHandleRequest(VaadinRequest request) {
        return ServletHelper.isRequestType(request, RequestType.UIDL);
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
        UI uI = session.getService().findUI(request);
        if (uI == null) {
            // This should not happen but it will if the UI has been closed. We
            // really don't want to see it in the server logs though
            commitJsonResponse(response, VaadinService.createUINotFoundJSON());
            return true;
        }

        StringWriter stringWriter = new StringWriter();

        try {
            getRpcHandler(session).handleRpc(uI, request.getReader(), request);
            writeUidl(uI, stringWriter, false);
        } catch (JsonException e) {
            getLogger().error("Error writing JSON to response", e);
            // Refresh on client side
            writeRefresh(response);
            return true;
        } catch (InvalidUIDLSecurityKeyException e) {
            getLogger().warn("Invalid security key received from {}",
                    request.getRemoteHost());
            // Refresh on client side
            writeRefresh(response);
            return true;
        } catch (ResynchronizationRequiredException e) { // NOSONAR
            // Resync on the client side
            writeUidl(uI, stringWriter, true);
        } finally {
            stringWriter.close();
        }

        commitJsonResponse(response, stringWriter.toString());
        return true;
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
        if (!ServletHelper.isRequestType(request, RequestType.UIDL)) {
            return false;
        }
        VaadinService service = request.getService();
        service.writeUncachedStringResponse(response,
                JsonConstants.JSON_CONTENT_TYPE,
                VaadinService.createSessionExpiredJSON());

        return true;
    }

    private ServerRpcHandler getRpcHandler(VaadinSession session) {
        session.checkHasLock();
        if (rpcHandler == null) {
            rpcHandler = createRpcHandler();
        }
        return rpcHandler;
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

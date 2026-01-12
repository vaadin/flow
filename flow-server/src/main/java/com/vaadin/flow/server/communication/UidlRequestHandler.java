/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonDecodingException;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.SessionExpiredHandler;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.ClientResentPayloadException;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.server.communication.ServerRpcHandler.ResynchronizationRequiredException;
import com.vaadin.flow.server.dau.DAUUtils;
import com.vaadin.flow.server.dau.DauEnforcementException;
import com.vaadin.flow.shared.JsonConstants;

import static com.vaadin.flow.shared.ApplicationConstants.RPC_INVOCATIONS;
import static com.vaadin.flow.shared.ApplicationConstants.SERVER_SYNC_ID;
import static com.vaadin.flow.shared.JsonConstants.RPC_NAVIGATION_LOCATION;
import static com.vaadin.flow.shared.JsonConstants.UIDL_KEY_EXECUTE;
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

    public static final Pattern HASH_PATTERN = Pattern
            .compile("window.location.hash ?= ?'(.*?)'");
    public static final Pattern URL_PATTERN = Pattern.compile("^(.*)#(.+)$");
    public static final String PUSH_STATE_HASH = "setTimeout(() => history.pushState(null, null, location.pathname + location.search + '#%s'));";
    public static final String PUSH_STATE_LOCATION = "setTimeout(() => history.pushState(null, null, '%s'));";

    private static final String SYNC_ID = '"' + SERVER_SYNC_ID + '"';
    private static final String RPC = RPC_INVOCATIONS;
    private static final String LOCATION = RPC_NAVIGATION_LOCATION;
    private static final String CHANGES = "changes";
    private static final String EXECUTE = UIDL_KEY_EXECUTE;

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
            getRpcHandler().handleRpc(uI, requestBody, request);
            writeUidl(uI, stringWriter, false);
        } catch (ClientResentPayloadException e) {
            stringWriter.write(uI.getInternals().getLastRequestResponse());
        } catch (JsonDecodingException e) {
            getLogger().error("Error writing JSON to response", e);
            // Refresh on client side
            return Optional.of(() -> writeRefresh(response));
        } catch (InvalidUIDLSecurityKeyException e) {
            getLogger().warn("Invalid security key received from {}",
                    request.getRemoteHost());
            // Refresh on client side
            return Optional.of(() -> writeRefresh(response));
        } catch (DauEnforcementException e) {
            getLogger().warn(
                    "Daily Active User limit reached. Blocking new user request");
            response.setHeader(DAUUtils.STATUS_CODE_KEY, String
                    .valueOf(HttpStatusCode.SERVICE_UNAVAILABLE.getCode()));
            String json = DAUUtils.jsonEnforcementResponse(request, e);
            return Optional.of(() -> commitJsonResponse(response, json));
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

    void writeUidl(UI ui, Writer writer, boolean resync) throws IOException {
        ObjectNode uidl = createUidl(ui, resync);

        removeOffendingMprHashFragment(uidl);

        // some dirt to prevent cross site scripting
        String responseString = "for(;;);[" + uidl + "]";
        ui.getInternals().setLastRequestResponse(responseString);
        writer.write(responseString);
    }

    ObjectNode createUidl(UI ui, boolean resync) {
        return new UidlWriter().createUidl(ui, false, resync);
    }

    private static Logger getLogger() {
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

    private ServerRpcHandler getRpcHandler() {
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

    private void removeOffendingMprHashFragment(ObjectNode uidl) {
        if (!uidl.has(EXECUTE)) {
            return;
        }

        ArrayNode exec = (ArrayNode) uidl.get(EXECUTE);
        String location = null;
        int idx = -1;
        for (int i = 0; i < exec.size(); i++) {
            ArrayNode arr = (ArrayNode) exec.get(i);
            for (int j = 0; j < arr.size(); j++) {
                if (!arr.get(j).getNodeType().equals(JsonNodeType.STRING)) {
                    continue;
                }
                String script = arr.get(j).asString();
                if (script.contains("history.pushState")) {
                    idx = i;
                    continue;
                }
                if (!script.startsWith(SYNC_ID)) {
                    continue;
                }

                ObjectNode json = JacksonUtils.readTree("{" + script + "}");
                location = removeHashInV7Uidl(json);
                if (location != null) {
                    script = json.toPrettyString();
                    // remove curly brackets
                    script = script.substring(1, script.length() - 1);
                    arr.set(j, script);
                }
            }
        }

        if (location != null) {
            ArrayNode arr = JacksonUtils.createArrayNode();
            arr.add("");
            arr.add(String
                    .format(location.startsWith("http") ? PUSH_STATE_LOCATION
                            : PUSH_STATE_HASH, location));
            if (idx >= 0) {
                exec.set(idx, arr);
            } else {
                exec.add(arr);
            }

        }
    }

    private String removeHashInV7Uidl(ObjectNode json) {
        String removed = null;
        ArrayNode changes = (ArrayNode) json.get(CHANGES);
        for (int i = 0; i < changes.size(); i++) {
            String hash = removeHashInChange((ArrayNode) changes.get(i));
            if (hash != null) {
                removed = hash;
            }
        }
        ArrayNode rpcs = (ArrayNode) json.get(RPC);
        for (int i = 0; i < rpcs.size(); i++) {
            String hash = removeHashInRpc((ArrayNode) rpcs.get(i));
            if (removed == null && hash != null) {
                removed = hash;
            }
        }
        return removed;
    }

    private String removeHashInChange(ArrayNode change) {
        if (change.size() < 3
                || !change.get(2).getNodeType().equals(JsonNodeType.ARRAY)) {
            return null;
        }
        ArrayNode value = (ArrayNode) change.get(2);
        if (value.size() < 2
                || !value.get(1).getNodeType().equals(JsonNodeType.OBJECT)) {
            return null;
        }
        ObjectNode location = (ObjectNode) value.get(1);
        if (!location.has(LOCATION)) {
            return null;
        }
        String url = location.get(LOCATION).asString();
        Matcher match = URL_PATTERN.matcher(url);
        if (match.find()) {
            location.put(LOCATION, match.group(1));
            return url;
        }
        return null;
    }

    private String removeHashInRpc(ArrayNode rpc) {
        if (rpc.size() != 4
                || !rpc.get(1).getNodeType().equals(JsonNodeType.STRING)
                || !rpc.get(2).getNodeType().equals(JsonNodeType.STRING)
                || !rpc.get(3).getNodeType().equals(JsonNodeType.ARRAY)
                || !"com.vaadin.shared.extension.javascriptmanager.ExecuteJavaScriptRpc"
                        .equals(rpc.get(1).asString())
                || !"executeJavaScript".equals(rpc.get(2).asString())) {
            return null;
        }
        ArrayNode scripts = (ArrayNode) rpc.get(3);
        for (int j = 0; j < scripts.size(); j++) {
            String exec = scripts.get(j).asString();
            Matcher match = HASH_PATTERN.matcher(exec);
            if (match.find()) {
                // replace JS with a noop
                scripts.set(j, ";");
                return match.group(1);
            }
        }
        return null;
    }
}

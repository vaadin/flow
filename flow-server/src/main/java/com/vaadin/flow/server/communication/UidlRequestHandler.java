/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.JavaScriptBootstrapUI;
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

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonException;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.impl.JsonUtil;

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
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRequestHandler extends SynchronizedRequestHandler
        implements SessionExpiredHandler {


    private ServerRpcHandler rpcHandler;

    public static final Pattern HASH_PATTERN = Pattern.compile("window.location.hash ?= ?'(.*?)'");
    public static final Pattern URL_PATTERN = Pattern.compile("^(.*)#(.+)$");
    public static final String PUSH_STATE_HASH =
            "setTimeout(() => history.pushState(null, null, location.pathname + location.search + '#%s'));";
    public static final String PUSH_STATE_LOCATION =
            "setTimeout(() => history.pushState(null, null, '%s'));";

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
        UI uI = session.getService().findUI(request);
        if (uI == null) {
            // This should not happen but it will if the UI has been closed. We
            // really don't want to see it in the server logs though
            commitJsonResponse(response,
                    VaadinService.createUINotFoundJSON(false));
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

    void writeUidl(UI ui, Writer writer, boolean resync)
            throws IOException {
        JsonObject uidl = createUidl(ui, resync);

        if (ui instanceof JavaScriptBootstrapUI) {
            removeOffendingMprHashFragment(uidl);
        }

        // some dirt to prevent cross site scripting
        String responseString = "for(;;);[" + uidl.toJson() + "]";
        writer.write(responseString);
    }

    JsonObject createUidl(UI ui, boolean resync) {
        return new UidlWriter().createUidl(ui, false, resync);
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

    private void removeOffendingMprHashFragment(JsonObject uidl) {
        if (!uidl.hasKey(EXECUTE)) {
            return;
        }

        JsonArray exec = uidl.getArray(EXECUTE);
        String location = null;
        int idx = -1;
        for (int i = 0; i < exec.length(); i++) {
            JsonArray arr = exec.get(i);
            for (int j = 0; j < arr.length(); j++) {
                if (!arr.get(j).getType().equals(JsonType.STRING)) {
                    continue;
                }
                String script = arr.getString(j);
                if (script.contains("history.pushState")) {
                    idx = i;
                    continue;
                }
                if (!script.startsWith(SYNC_ID)) {
                    continue;
                }

                JsonObject json = JsonUtil.parse("{" + script + "}");
                location = removeHashInV7Uidl(json);
                if (location != null) {
                    script = JsonUtil.stringify(json);
                    // remove curly brackets
                    script = script.substring(1, script.length() - 1);
                    arr.set(j, script);
                }
            }
        }

        if (location != null) {
            idx = idx >= 0 ? idx : exec.length();
            JsonArray arr = Json.createArray();
            arr.set(0, "");
            arr.set(1,
                    String.format(
                            location.startsWith("http") ? PUSH_STATE_LOCATION
                                    : PUSH_STATE_HASH,
                            location));
            exec.set(idx, arr);
        }
    }

    private String removeHashInV7Uidl(JsonObject json) {
        String removed = null;
        JsonArray changes = json.getArray(CHANGES);
        for (int i = 0; i < changes.length(); i++) {
            String hash = removeHashInChange(changes.getArray(i));
            if (hash != null) {
                removed = hash;
            }
        }
        JsonArray rpcs = json.getArray(RPC);
        for (int i = 0; i < rpcs.length(); i++) {
            String hash = removeHashInRpc(rpcs.getArray(i));
            if (removed == null && hash != null) {
                removed = hash;
            }
        }
        return removed;
    }

    private String removeHashInChange(JsonArray change) {
        if (change.length() < 3
                || !change.get(2).getType().equals(JsonType.ARRAY)) {
            return null;
        }
        JsonArray value = change.getArray(2);
        if (value.length() < 2
                || !value.get(1).getType().equals(JsonType.OBJECT)) {
            return null;
        }
        JsonObject location = value.getObject(1);
        if (!location.hasKey(LOCATION)) {
            return null;
        }
        String url = location.getString(LOCATION);
        Matcher match = URL_PATTERN.matcher(url);
        if (match.find()) {
            location.put(LOCATION, match.group(1));
        }
        return url;
    }

    private String removeHashInRpc(JsonArray rpc) {
        if (rpc.length() != 4 || !rpc.get(1).getType().equals(JsonType.STRING)
                || !rpc.get(2).getType().equals(JsonType.STRING)
                || !rpc.get(3).getType().equals(JsonType.ARRAY)
                || !"com.vaadin.shared.extension.javascriptmanager.ExecuteJavaScriptRpc"
                        .equals(rpc.getString(1))
                || !"executeJavaScript".equals(rpc.getString(2))) {
            return null;
        }
        JsonArray scripts = rpc.getArray(3);
        for (int j = 0; j < scripts.length(); j++) {
            String exec = scripts.getString(j);
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


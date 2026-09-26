/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.History.HistoryJs;
import com.vaadin.flow.internal.ConstantPool;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.JsonDecodingException;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.RequestBodyTooLargeException;
import com.vaadin.flow.server.SessionExpiredHandler;
import com.vaadin.flow.server.SynchronizedRequestHandler;
import com.vaadin.flow.server.SystemMessages;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.ServerRpcHandler.ClientResentPayloadException;
import com.vaadin.flow.server.communication.ServerRpcHandler.InvalidUIDLSecurityKeyException;
import com.vaadin.flow.server.communication.ServerRpcHandler.MessageIdSyncException;
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
    /**
     * The JavaScript that pushes a corrected hash onto the location the browser
     * is at, for a v7 UIDL that named no location to go with it. The hash is
     * the parameter of the call rather than part of the JavaScript.
     */
    private static final String PUSH_STATE_HASH = "setTimeout(() => history.pushState(null, '', location.pathname + location.search + '#' + $0));";

    /**
     * The JavaScript that pushes a corrected location, which the v7 UIDL gave
     * in full. The location is the parameter of the call rather than part of
     * the JavaScript.
     */
    private static final String PUSH_STATE_LOCATION = "setTimeout(() => history.pushState(null, '', $0));";

    private static final String SYNC_ID = '"' + SERVER_SYNC_ID + '"';
    private static final String RPC = RPC_INVOCATIONS;
    private static final String LOCATION = RPC_NAVIGATION_LOCATION;
    private static final String CHANGES = "changes";

    private static final String CONSTANTS = "constants";
    private static final String EXECUTE = UIDL_KEY_EXECUTE;

    /**
     * What names the push state that the router scheduled among the constants
     * of a response, which is the invocation the fix-up corrects.
     * <p>
     * An invocation names what it runs rather than carrying it, and a constant
     * is named by a hash of its value, so the name of a given call is the same
     * in every response and is known here without reading one. Only the push
     * state of the non-React router is this: a replace state, a React
     * navigation and an application invocation that runs the same browser
     * function are all named differently.
     */
    private static final String ROUTER_PUSH_STATE = nameOfFunction(
            functionId(HistoryJs.class, "pushState", 2));

    private static final String CORRECTED_LOCATION_FUNCTION = functionId(
            MprPushStateJs.class, "pushLocation", 1);

    private static final String CORRECTED_HASH_FUNCTION = functionId(
            MprPushStateJs.class, "pushHash", 1);

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
        String requestBody;
        try {
            requestBody = SynchronizedRequestHandler.getRequestBody(
                    request.getReader(),
                    SynchronizedRequestHandler.getMaxRequestBodySize(request));
        } catch (RequestBodyTooLargeException e) {
            response.sendError(
                    HttpStatusCode.REQUEST_ENTITY_TOO_LARGE.getCode(),
                    e.getMessage());
            return true;
        }
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
            String lastResponse = uI.getInternals().getLastRequestResponse();
            if (lastResponse != null) {
                stringWriter.write(lastResponse);
            } else {
                // Nothing recorded to send again, so describe the current
                // state instead.
                writeUidl(uI, stringWriter, false);
            }
        } catch (JsonDecodingException e) {
            getLogger().error("Error writing JSON to response", e);
            // Refresh on client side
            return Optional.of(() -> writeRefresh(response));
        } catch (InvalidUIDLSecurityKeyException e) {
            getLogger().warn("Invalid security key received from {}",
                    request.getRemoteHost());
            // Refresh on client side
            return Optional.of(() -> writeRefresh(response));
        } catch (MessageIdSyncException e) {
            getLogger().warn(
                    "Message ID sync error. Expected: {}, received: {}",
                    e.getExpectedId(), e.getReceivedId());
            SystemMessages systemMessages = session.getService()
                    .getSystemMessages(HandlerHelper.findLocale(null, request),
                            request);
            return Optional.of(() -> writeSyncError(systemMessages, response));
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

    private void writeSyncError(SystemMessages systemMessages,
            VaadinResponse response) throws IOException {
        String json = VaadinService.createCriticalNotificationJSON(
                systemMessages.getSyncErrorCaption(),
                systemMessages.getSyncErrorMessage(), null,
                systemMessages.getSyncErrorURL());
        commitJsonResponse(response, json);
    }

    void writeUidl(UI ui, Writer writer, boolean resync) throws IOException {
        ObjectNode uidl = createUidl(ui, resync);

        removeOffendingMprHashFragment(ui, uidl);

        String responseString = uidl.toString();
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

    private void removeOffendingMprHashFragment(UI ui, ObjectNode uidl) {
        if (!uidl.has(EXECUTE)) {
            return;
        }

        ArrayNode exec = (ArrayNode) uidl.get(EXECUTE);
        String location = null;
        int idx = -1;
        for (int i = 0; i < exec.size(); i++) {
            ArrayNode arr = (ArrayNode) exec.get(i);
            if (runsRouterPushState(arr)) {
                idx = i;
            }
            // Everything but the last element is a parameter, and the v7 UIDL
            // this reaches into is one of them. The last one names what the
            // invocation runs rather than being it.
            for (int j = 0; j < arr.size() - 1; j++) {
                if (!arr.get(j).getNodeType().equals(JsonNodeType.STRING)) {
                    continue;
                }
                String script = arr.get(j).asString();
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
            ArrayNode corrected = correctedPushState(ui, uidl, location);
            if (idx >= 0) {
                exec.set(idx, corrected);
            } else {
                exec.add(corrected);
            }

        }
    }

    /**
     * Whether the given invocation is the push state that the router scheduled
     * for the location being navigated to, which is the one the fix-up replaces
     * with a push state of the corrected location.
     *
     * @see #ROUTER_PUSH_STATE
     */
    private static boolean runsRouterPushState(ArrayNode invocation) {
        if (invocation.isEmpty()) {
            return false;
        }
        JsonNode name = invocation.get(invocation.size() - 1);
        return name.getNodeType().equals(JsonNodeType.STRING)
                && ROUTER_PUSH_STATE.equals(name.asString());
    }

    /**
     * The invocation that pushes the corrected location, as a call of the
     * JavaScript that {@link MprPushStateJs} declares.
     * <p>
     * The location is a parameter of the call, so whatever it holds reaches the
     * browser rather than becoming part of what the browser runs, and the two
     * declared functions serve every location a session navigates to rather
     * than each of them adding a constant the session keeps for good. The
     * browser runs a function the bundle already has, which is what a content
     * security policy without <code>unsafe-eval</code> allows.
     */
    private static ArrayNode correctedPushState(UI ui, ObjectNode uidl,
            String location) {
        ArrayNode invocation = JacksonUtils.createArrayNode();
        invocation.add(location);
        // The client applies the function to the parameter that follows the
        // arguments of the call, and this call has nothing to run on: the
        // declared JavaScript addresses the browser's history rather than an
        // element, the same way page level JavaScript does.
        invocation.addNull();
        invocation.add(asConstant(ui, uidl,
                UidlWriter.functionConstant(location.startsWith("http")
                        ? CORRECTED_LOCATION_FUNCTION
                        : CORRECTED_HASH_FUNCTION)));
        return invocation;
    }

    /**
     * The identifier of the function that the named method of the given
     * JavaScript definition runs, which is what an invocation of it names.
     * <p>
     * A call is built to ask it, since the identifier belongs to the
     * declaration rather than to the arguments: they only say which of the
     * methods of that name is meant.
     * <p>
     * Package private for the tests of the fix-up, which assert on what it
     * sends and recognizes.
     */
    static String functionId(Class<?> definitionType, String methodName,
            int parameterCount) {
        return new JsCall(definitionType, methodName,
                Collections.nCopies(parameterCount, null)).getFunctionId();
    }

    /**
     * What names the constant of the given function, which is what an
     * invocation that runs it carries.
     */
    private static String nameOfFunction(String functionId) {
        return new ConstantPoolKey(UidlWriter.functionConstant(functionId))
                .getId();
    }

    /**
     * Registers the given value with the constant pool of the given UI, puts it
     * among the constants of the given response when the client does not have
     * it yet, and answers with what names it - which is what an invocation
     * carries instead of the value.
     */
    private static String asConstant(UI ui, ObjectNode uidl, JsonNode value) {
        ConstantPool constantPool = ui.getInternals().getConstantPool();
        String name = constantPool.getConstantId(new ConstantPoolKey(value));
        if (constantPool.hasNewConstants()) {
            ObjectNode constants = uidl.has(CONSTANTS)
                    ? (ObjectNode) uidl.get(CONSTANTS)
                    : uidl.putObject(CONSTANTS);
            constantPool.dumpConstants().properties()
                    .forEach(constant -> constants.set(constant.getKey(),
                            constant.getValue()));
        }
        return name;
    }

    /**
     * The push state that the MPR fix-up sends, as a JavaScript definition, so
     * that the build collects it into the bundle and the corrected location is
     * a parameter of the call rather than part of the JavaScript.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     */
    @JsDefinition
    public interface MprPushStateJs extends Serializable {

        /**
         * Pushes the given location, which the v7 UIDL gave in full.
         *
         * @param location
         *            the location to push
         */
        @JsExpression(PUSH_STATE_LOCATION)
        void pushLocation(String location);

        /**
         * Pushes the given hash onto the location the browser is at, for a v7
         * UIDL that named no location to go with it.
         *
         * @param hash
         *            the hash to push, without the leading <code>#</code>
         */
        @JsExpression(PUSH_STATE_HASH)
        void pushHash(String hash);
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

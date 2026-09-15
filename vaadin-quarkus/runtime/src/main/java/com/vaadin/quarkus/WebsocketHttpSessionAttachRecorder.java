/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.quarkus;

import java.security.Principal;
import java.util.Optional;
import java.util.concurrent.Executor;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.quarkus.websockets.client.runtime.WebSocketPrincipal;
import io.undertow.httpcore.HttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.handlers.ServletPathMatch;
import io.undertow.servlet.handlers.ServletPathMatches;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.servlet.spec.HttpSessionImpl;
import io.undertow.servlet.spec.ServletContextImpl;
import io.undertow.vertx.VertxHttpExchange;
import io.undertow.websockets.ServerWebSocketContainer;
import io.undertow.websockets.WebSocketDeploymentInfo;
import io.undertow.websockets.vertx.VertxWebSocketHandler;
import io.undertow.websockets.vertx.VertxWebSocketHttpExchange;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import jakarta.servlet.http.HttpSession;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.shared.ApplicationConstants;

@Recorder
public class WebsocketHttpSessionAttachRecorder {

    /**
     * Gets an adapted version of VertxWebSocketHandler that attaches upgrade
     * request HTTP session to the Undertow WebSocket HttpExchange.
     *
     * @param info
     *            websocket deployment info
     * @param container
     *            websocket container
     * @param deploymentManager
     *            deployment manager
     * @return an adapted version of VertxWebSocketHandler
     */
    public Handler<RoutingContext> createWebSocketHandler(
            RuntimeValue<WebSocketDeploymentInfo> info,
            RuntimeValue<ServerWebSocketContainer> container,
            DeploymentManager deploymentManager) {

        Deployment deployment = deploymentManager.getDeployment();

        UpgradeRequestSessionAttachmentHandler handler = new UpgradeRequestSessionAttachmentHandler(
                deployment);

        return new VertxWebSocketHandler(container.getValue(),
                info.getValue()) {

            @Override
            public void handle(RoutingContext event) {
                // Only handle Vaadin PUSH requests
                if (HandlerHelper.RequestType.PUSH.getIdentifier()
                        .equals(event.request().getParam(
                                ApplicationConstants.REQUEST_TYPE_PARAMETER))) {
                    super.handle(event);
                } else {
                    event.next();
                }
            }

            @Override
            protected VertxWebSocketHttpExchange createHttpExchange(
                    RoutingContext event) {

                VertxHttpExchange delegate = new VertxHttpExchange(
                        event.request(), null, executor, null, null);
                return handler.extractSession(event, delegate)
                        .<VertxWebSocketHttpExchange> map(
                                sess -> new QuarkusVertxWebSocketHttpExchange(
                                        executor, event, sess))
                        .orElseGet(() -> super.createHttpExchange(event));
            }
        };
    }

    // Extract of logics from Undertow ServletInitialHandler to apply
    // SessionAttachmentHandler and get HTTP session for the current request
    private static class UpgradeRequestSessionAttachmentHandler {

        private final Deployment deployment;
        private final SessionAttachmentHandler sessionAttachmentHandler;

        private UpgradeRequestSessionAttachmentHandler(Deployment deployment) {
            this.deployment = deployment;
            sessionAttachmentHandler = new SessionAttachmentHandler(
                    exchange -> {
                    }, deployment.getSessionManager(),
                    deployment.getServletContext().getSessionConfig());
        }

        private void handleRequest(HttpServerExchange exchange)
                throws Exception {
            ServletContextImpl servletContext = deployment.getServletContext();
            final ServletPathMatch pathMatch = new ServletPathMatches(
                    deployment)
                    .getServletHandlerByPath(exchange.getRelativePath());
            final HttpServletResponseImpl response = new HttpServletResponseImpl(
                    exchange, servletContext);
            final HttpServletRequestImpl request = new HttpServletRequestImpl(
                    exchange, servletContext);
            final ServletRequestContext servletRequestContext = new ServletRequestContext(
                    servletContext.getDeployment(), request, response,
                    pathMatch);
            exchange.putAttachment(ServletRequestContext.ATTACHMENT_KEY,
                    servletRequestContext);
            sessionAttachmentHandler.handleRequest(exchange);
        }

        private Optional<HttpSession> extractSession(RoutingContext event,
                HttpExchange httpExchange) {

            HttpServerExchange serverExchange = new HttpServerExchange(
                    httpExchange, -1);
            serverExchange.setRelativePath(event.request().path());
            serverExchange.setDispatchExecutor(Runnable::run);

            try {
                handleRequest(serverExchange);
            } catch (Exception e) {
                // ignore and proceed with original behaviour
                return Optional.empty();
            }
            SessionManager sessionManager = deployment.getSessionManager();
            SessionConfig sessionConfig = deployment.getServletContext()
                    .getSessionConfig();
            ServletContextImpl servletContext = deployment.getServletContext();
            return Optional
                    .ofNullable(sessionManager.getSession(serverExchange,
                            sessionConfig))
                    .map(sess -> HttpSessionImpl.forSession(sess,
                            servletContext, false));

        }
    }

    // Copy of WebsocketServerRecorder$QuarkusVertxWebSocketHttpExchange
    // modified to return HTTP session
    private static class QuarkusVertxWebSocketHttpExchange
            extends VertxWebSocketHttpExchange {

        private final RoutingContext routingContext;
        private final HttpSession session;

        public QuarkusVertxWebSocketHttpExchange(Executor executor,
                RoutingContext routingContext, HttpSession session) {
            super(executor, routingContext);
            this.routingContext = routingContext;
            this.session = session;
        }

        @Override
        public Object getSession() {
            return session;
        }

        @Override
        public Principal getUserPrincipal() {
            QuarkusHttpUser user = (QuarkusHttpUser) routingContext.user();
            if (user != null) {
                return new WebSocketPrincipal(user.getSecurityIdentity());
            }
            return null;
        }
    }
}

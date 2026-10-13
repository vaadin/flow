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
package com.vaadin.flow.server.streams;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class DownloadHandlerTest {

    @Test
    void allowDisabled_lambda_disabledUpdateModeIsAlways() {
        DownloadHandler delegate = event -> {
        };

        DownloadHandler wrapped = delegate.allowDisabled();

        assertEquals(DisabledUpdateMode.ALWAYS,
                wrapped.getDisabledUpdateMode());
    }

    @Test
    void allowDisabled_lambda_forwardsHandleDownloadRequest() throws Exception {
        AtomicReference<DownloadEvent> received = new AtomicReference<>();
        DownloadHandler delegate = received::set;

        DownloadEvent event = new DownloadEvent(mock(VaadinRequest.class),
                mock(VaadinResponse.class), mock(VaadinSession.class),
                mock(Element.class));

        delegate.allowDisabled().handleDownloadRequest(event);

        assertSame(event, received.get(),
                "Wrapped handler must forward the event to the delegate");
    }

    @Test
    void allowDisabled_forwardsUrlPostfixAllowInertAndRangeRequests() {
        DownloadHandler delegate = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }

            @Override
            public String getUrlPostfix() {
                return "icon.svg";
            }

            @Override
            public boolean isAllowInert() {
                return true;
            }

            @Override
            public boolean isRangeRequestsEnabled() {
                return true;
            }
        };

        DownloadHandler wrapped = delegate.allowDisabled();

        assertEquals("icon.svg", wrapped.getUrlPostfix());
        assertTrue(wrapped.isAllowInert());
        assertTrue(wrapped.isRangeRequestsEnabled());
    }

    @Test
    void isRangeRequestsEnabled_onlyForFilesAndOptedInHandlers() {
        assertFalse(((DownloadHandler) event -> {
        }).isRangeRequestsEnabled());
        assertTrue(DownloadHandler.forFile(new File("video.mp4"))
                .isRangeRequestsEnabled());

        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        assertFalse(handler.isRangeRequestsEnabled());
        assertTrue(handler.enableRangeRequests().allowDisabled()
                .isRangeRequestsEnabled());
    }

    @Test
    void allowDisabled_abstractDownloadHandlerWrapped_modeIsAlways_inlinePreserved() {
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        handler.inline();

        DownloadHandler wrapped = handler.allowDisabled();

        assertEquals(DisabledUpdateMode.ALWAYS,
                wrapped.getDisabledUpdateMode());
        // The wrap does not interfere with the original handler's inline state
        assertTrue(handler.isInline());
    }

    @Test
    void allowDisabled_isIdempotent_returnsSameInstance() {
        DownloadHandler alwaysAllowed = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }

            @Override
            public DisabledUpdateMode getDisabledUpdateMode() {
                return DisabledUpdateMode.ALWAYS;
            }
        };

        assertSame(alwaysAllowed, alwaysAllowed.allowDisabled(),
                "allowDisabled() on a handler that is already ALWAYS must be a no-op");
    }

    @Test
    void allowDisabled_doubleWrap_returnsFirstWrapper() {
        DownloadHandler delegate = event -> {
        };
        DownloadHandler wrappedOnce = delegate.allowDisabled();
        DownloadHandler wrappedTwice = wrappedOnce.allowDisabled();

        assertSame(wrappedOnce, wrappedTwice,
                "Wrapping a handler that is already ALWAYS must be a no-op");
    }

    @Test
    void handleRequest_rangeNotAnswered_warnsOncePerHandlerClassInDevelopmentMode()
            throws Throwable {
        VaadinService service = createService(false);

        List<String> warnings = captureWarnings(() -> {
            request(new NamedHandler(false), service, "bytes=0-");
            request(new NamedHandler(false), service, "bytes=4096-");
        });

        assertEquals(List.of("The download handler "
                + NamedHandler.class.getName() + " of 'lake.webm'"), warnings);
    }

    @Test
    void handleRequest_lambdaThroughAllowDisabled_warningNamesDeclaringClass()
            throws Throwable {
        VaadinService service = createService(false);

        List<String> warnings = captureWarnings(() -> request(
                Player.createHandler().allowDisabled(), service, "bytes=0-"));

        assertEquals(List.of("A download handler declared as a lambda in "
                + Player.class.getName()), warnings);
    }

    @Test
    void handleRequest_rangeAnsweredNotRequestedOrProductionMode_doesNotWarn()
            throws Throwable {
        VaadinService developmentMode = createService(false);

        List<String> warnings = captureWarnings(() -> {
            request(new NamedHandler(true), developmentMode, "bytes=0-");
            request(new NamedHandler(false), developmentMode, null);
            request(new NamedHandler(false), createService(true), "bytes=0-");
        });

        assertEquals(List.of(), warnings);
    }

    private static VaadinService createService(boolean productionMode) {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(productionMode);
        MockVaadinContext context = new MockVaadinContext();
        VaadinService service = mock(VaadinService.class);
        when(service.getDeploymentConfiguration()).thenReturn(configuration);
        when(service.getContext()).thenReturn(context);
        return service;
    }

    private static void request(DownloadHandler handler, VaadinService service,
            String range) throws Exception {
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getHeader("Range")).thenReturn(range);
        when(request.getService()).thenReturn(service);
        handler.handleRequest(request, mock(VaadinResponse.class),
                mock(VaadinSession.class), mock(Element.class));
    }

    /**
     * Runs the action and returns how each warning that it logged names the
     * handler.
     */
    private static List<String> captureWarnings(Executable action)
            throws Throwable {
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class)) {
            loggerFactory
                    .when(() -> LoggerFactory
                            .getLogger(UnansweredRangeRequests.class))
                    .thenReturn(logger);
            action.execute();
        }
        ArgumentCaptor<Object> handlers = ArgumentCaptor.forClass(Object.class);
        verify(logger, atLeast(0)).warn(anyString(), handlers.capture());
        verifyNoMoreInteractions(logger);
        return handlers.getAllValues().stream().map(String::valueOf).toList();
    }

    private static class NamedHandler implements DownloadHandler {
        private final boolean rangeRequestsEnabled;

        private NamedHandler(boolean rangeRequestsEnabled) {
            this.rangeRequestsEnabled = rangeRequestsEnabled;
        }

        @Override
        public void handleDownloadRequest(DownloadEvent event) {
            // the content does not matter, only how it is requested
        }

        @Override
        public String getUrlPostfix() {
            return "lake.webm";
        }

        @Override
        public boolean isRangeRequestsEnabled() {
            return rangeRequestsEnabled;
        }
    }

    private static class Player {
        private static DownloadHandler createHandler() {
            return event -> {
                // the content does not matter, only how it is requested
            };
        }
    }
}

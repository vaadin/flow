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

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

public class DownloadHandlerTest {

    @Test
    public void allowDisabled_lambda_disabledUpdateModeIsAlways() {
        DownloadHandler delegate = event -> {
        };

        DownloadHandler wrapped = delegate.allowDisabled();

        Assert.assertEquals(DisabledUpdateMode.ALWAYS,
                wrapped.getDisabledUpdateMode());
    }

    @Test
    public void allowDisabled_lambda_forwardsHandleDownloadRequest()
            throws Exception {
        AtomicReference<DownloadEvent> received = new AtomicReference<>();
        DownloadHandler delegate = received::set;

        DownloadEvent event = new DownloadEvent(
                Mockito.mock(VaadinRequest.class),
                Mockito.mock(VaadinResponse.class),
                Mockito.mock(VaadinSession.class), Mockito.mock(Element.class));

        delegate.allowDisabled().handleDownloadRequest(event);

        Assert.assertSame(
                "Wrapped handler must forward the event to the delegate", event,
                received.get());
    }

    @Test
    public void allowDisabled_forwardsUrlPostfixAndAllowInert() {
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
        };

        DownloadHandler wrapped = delegate.allowDisabled();

        Assert.assertEquals("icon.svg", wrapped.getUrlPostfix());
        Assert.assertTrue(wrapped.isAllowInert());
    }

    @Test
    public void allowDisabled_abstractDownloadHandlerWrapped_modeIsAlways_inlinePreserved() {
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        handler.inline();

        DownloadHandler wrapped = handler.allowDisabled();

        Assert.assertEquals(DisabledUpdateMode.ALWAYS,
                wrapped.getDisabledUpdateMode());
        // The wrap does not interfere with the original handler's inline state
        Assert.assertTrue(handler.isInline());
    }

    @Test
    public void allowDisabled_isIdempotent_returnsSameInstance() {
        DownloadHandler alwaysAllowed = new DownloadHandler() {
            @Override
            public void handleDownloadRequest(DownloadEvent event) {
            }

            @Override
            public DisabledUpdateMode getDisabledUpdateMode() {
                return DisabledUpdateMode.ALWAYS;
            }
        };

        Assert.assertSame(
                "allowDisabled() on a handler that is already ALWAYS must be a no-op",
                alwaysAllowed, alwaysAllowed.allowDisabled());
    }

    @Test
    public void allowDisabled_doubleWrap_returnsFirstWrapper() {
        DownloadHandler delegate = event -> {
        };
        DownloadHandler wrappedOnce = delegate.allowDisabled();
        DownloadHandler wrappedTwice = wrappedOnce.allowDisabled();

        Assert.assertSame(
                "Wrapping a handler that is already ALWAYS must be a no-op",
                wrappedOnce, wrappedTwice);
    }
}

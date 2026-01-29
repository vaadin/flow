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
package com.vaadin.flow.component.html;

import java.io.IOException;

import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.ValueSignal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for Anchor constructors that accept Signal<String> and related
 * bindText(Signal) semantics as documented in Anchor Javadoc.
 */
public class AnchorBindTextTest extends SignalsUnitTest {

    @Test
    public void constructor_href_signal_lifecycleAndUpdates() {
        // Detached: binding inactive
        var signal = new ValueSignal<>("one");
        Anchor anchor = new Anchor("/path", signal);
        // no propagation while detached
        assertEquals("", anchor.getText());

        // Update before attach is ignored
        signal.value("two");
        assertEquals("", anchor.getText());

        // Attach -> latest value applied
        UI.getCurrent().add(anchor);
        assertEquals("two", anchor.getText());

        // Updates propagate while attached
        signal.value("three");
        assertEquals("three", anchor.getText());

        // Detach -> updates ignored
        anchor.removeFromParent();
        signal.value("four");
        assertEquals("three", anchor.getText());

        // Re-attach -> latest value applied
        UI.getCurrent().add(anchor);
        assertEquals("four", anchor.getText());
    }

    @Test
    public void bindText_unbindWithNull_keepsCurrentAndStopsUpdates() {
        UI.getCurrent(); // ensure UI exists
        var signal = new ValueSignal<>("A");
        Anchor anchor = new Anchor("/a", signal);
        UI.getCurrent().add(anchor);
        assertEquals("A", anchor.getText());

        // Unbind using null
        anchor.bindText(null);
        assertEquals("A", anchor.getText());

        // Further updates no longer propagate
        signal.value("B");
        assertEquals("A", anchor.getText());

        // Manual setText works after unbind
        anchor.setText("manual");
        assertEquals("manual", anchor.getText());
    }

    @Test
    public void setText_whileBindingActive_throwsBindingActiveException() {
        var signal = new ValueSignal<>("x");
        Anchor anchor = new Anchor("/x", signal);
        UI.getCurrent().add(anchor);
        assertEquals("x", anchor.getText());
        assertThrows(BindingActiveException.class, () -> anchor.setText("y"));
    }

    @Test
    public void bindText_againWhileActive_throwsBindingActiveException() {
        var signal = new ValueSignal<>("first");
        Anchor anchor = new Anchor("/x", signal);
        UI.getCurrent().add(anchor);
        assertEquals("first", anchor.getText());
        assertThrows(BindingActiveException.class,
                () -> anchor.bindText(new ValueSignal<>("second")));
    }

    @Test
    public void constructor_downloadHandler_signal_setsDownloadAttributeAccordingToHandlerType() {
        // AbstractDownloadHandler default: attachment (download attribute true)
        var attachmentHandler = new AttachmentHandler();
        Anchor a1 = new Anchor(attachmentHandler, new ValueSignal<>("d1"));
        UI.getCurrent().add(a1);
        assertTrue("download attribute should be set for attachment handler",
                a1.isDownload());
        assertEquals("d1", a1.getText());

        // AbstractDownloadHandler inline: no download attribute
        var inlineHandler = new InlineHandler();
        Anchor a2 = new Anchor(inlineHandler, new ValueSignal<>("d2"));
        UI.getCurrent().add(a2);
        assertFalse("download attribute should NOT be set for inline handler",
                a2.isDownload());
        assertEquals("d2", a2.getText());

        // Custom non-abstract DownloadHandler: treated as DOWNLOAD per Javadoc
        DownloadHandler custom = new CustomHandler();
        Anchor a3 = new Anchor(custom, new ValueSignal<>("d3"));
        UI.getCurrent().add(a3);
        assertTrue("download attribute should be set for custom handler",
                a3.isDownload());
        assertEquals("d3", a3.getText());
    }

    private static class AttachmentHandler
            extends AbstractDownloadHandler<AttachmentHandler> {
        @Override
        public void handleDownloadRequest(DownloadEvent event)
                throws IOException {
            // no-op for unit test
        }
    }

    private static class InlineHandler
            extends AbstractDownloadHandler<InlineHandler> {
        InlineHandler() {
            inline();
        }

        @Override
        public void handleDownloadRequest(DownloadEvent event)
                throws IOException {
            // no-op for unit test
        }
    }

    private static class CustomHandler implements DownloadHandler {
        @Override
        public void handleDownloadRequest(DownloadEvent event)
                throws IOException {
            // no-op for unit test
        }
    }
}

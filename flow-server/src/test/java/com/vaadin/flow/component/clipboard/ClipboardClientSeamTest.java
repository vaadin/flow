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
package com.vaadin.flow.component.clipboard;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.trigger.internal.PromiseAction;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@link ClipboardClient} seam — both the public
 * {@link ClipboardClientFactory} extension point that external test drivers
 * register through {@link Lookup}, and the
 * {@link com.vaadin.flow.component.internal.UIInternals#setClipboardClient(ClipboardClient)}
 * direct-install seam used by flow-server's own tests. The wire-protocol
 * assertions that pin production behavior unchanged live in
 * {@code ClipboardTest}.
 */
class ClipboardClientSeamTest {

    private MockUI ui;

    @BeforeEach
    void setUp() {
        ui = new MockUI();
    }

    @Tag("test-button")
    private static final class TestButton extends Component
            implements ClickNotifier<TestButton> {
    }

    @Tag("test-field")
    private static final class TestField
            extends AbstractField<TestField, String> {
        TestField() {
            super("");
        }

        @Override
        protected void setPresentationValue(String newPresentationValue) {
            // not exercised in these tests
        }
    }

    private TestButton attachedButton() {
        TestButton button = new TestButton();
        ui.getElement().appendChild(button.getElement());
        return button;
    }

    @Test
    void lookupFactory_resolvedOnFirstUse_clientReceivesWriteCalls() {
        FakeClient fake = new FakeClient();
        stubFactory(unused -> fake);

        Clipboard.onClick(attachedButton()).writeText("Hello");

        assertEquals(1, fake.writes.size(),
                "factory-produced client should receive writeText() calls");
        assertSame(fake, ui.getInternals().getClipboardClient(),
                "the factory-produced client should be cached on the UI");
    }

    @Test
    void noFactory_fallsBackToBrowserClipboardClient() {
        // Explicitly clear any factory stubbed by another test (the mock
        // Lookup is shared across MockUIs that reuse the same session).
        stubFactory(null);

        Clipboard.onClick(attachedButton()).writeText("Hello");

        assertInstanceOf(BrowserClipboardClient.class,
                ui.getInternals().getClipboardClient(),
                "without a factory the default browser client should be used");
    }

    @Test
    void setClipboardClient_routesWriteThroughInstalledClient() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setClipboardClient(fake);

        Clipboard.onClick(attachedButton()).writeText("Hello");

        assertEquals(1, fake.writes.size(),
                "writeText() should route through the installed client");
    }

    @Test
    void setClipboardClient_closesPreviousClient() {
        FakeClient first = new FakeClient();
        FakeClient second = new FakeClient();
        ui.getInternals().setClipboardClient(first);
        ui.getInternals().setClipboardClient(second);

        assertTrue(first.closed,
                "previous client should be closed when setClipboardClient replaces it");
    }

    @Test
    void write_carriesContentAndCallbackPresenceToClient() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setClipboardClient(fake);

        TestField field = new TestField();
        field.setValue("live-value");
        ui.getElement().appendChild(field.getElement());
        Clipboard.onClick(attachedButton()).writeText(field, copied -> {
        }, err -> {
        });

        assertEquals(1, fake.writes.size());
        FakeClient.WriteCall call = fake.writes.get(0);
        assertEquals("live-value", call.write.text(),
                "the descriptor should report the bound field's live value");
        assertTrue(call.hasSuccess, "observed write should carry onSuccess");
        assertTrue(call.hasError, "observed write should carry onError");
    }

    @Test
    void read_routesThroughClientWithKind() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setClipboardClient(fake);

        Clipboard.onClick(attachedButton()).readText(text -> {
        }, err -> {
        });

        assertEquals(1, fake.reads.size(),
                "readText() should route through the installed client");
        assertEquals(ClipboardReadKind.READ_TEXT, fake.reads.get(0),
                "the read kind should be carried to the client");
    }

    @Test
    void onPaste_routesThroughInstalledClient() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setClipboardClient(fake);

        Clipboard.onPaste(attachedButton(), event -> {
        });

        assertEquals(1, fake.pasteRegistrations,
                "onPaste() should route through the installed client");
    }

    @Test
    void onFilePaste_routesThroughInstalledClient() {
        FakeClient fake = new FakeClient();
        ui.getInternals().setClipboardClient(fake);

        Clipboard.onFilePaste(attachedButton(),
                UploadHandler.inMemory((m, b) -> {
                }));

        assertEquals(1, fake.filePasteRegistrations,
                "onFilePaste() should route through the installed client");
    }

    private void stubFactory(@Nullable ClipboardClientFactory factory) {
        VaadinService service = VaadinService.getCurrent();
        Lookup lookup = service.getContext().getAttribute(Lookup.class);
        Mockito.when(lookup.lookup(ClipboardClientFactory.class))
                .thenReturn(factory);
    }

    /**
     * Minimal in-test fake recording the calls routed through the seam.
     */
    private static final class FakeClient implements ClipboardClient {

        record WriteCall(ClipboardWrite write, boolean hasSuccess,
                boolean hasError) {
        }

        final List<WriteCall> writes = new ArrayList<>();
        final List<ClipboardReadKind> reads = new ArrayList<>();
        int pasteRegistrations;
        int filePasteRegistrations;
        boolean closed;

        @Override
        public WriteHandle registerWrite(Component trigger,
                ClipboardWrite write,
                @Nullable SerializableConsumer<@Nullable String> onSuccess,
                @Nullable SerializableConsumer<PromiseAction.Error> onError) {
            writes.add(
                    new WriteCall(write, onSuccess != null, onError != null));
            return new FakeWriteHandle(trigger.getElement(), write,
                    onSuccess != null, onError != null);
        }

        @Override
        public ReadHandle registerRead(Component trigger,
                ClipboardReadKind kind,
                SerializableConsumer<@Nullable ClipboardPayload> onSuccess,
                SerializableConsumer<PromiseAction.Error> onError) {
            reads.add(kind);
            return new FakeReadHandle(trigger.getElement(), kind);
        }

        @Override
        public Registration registerPaste(Component target,
                PasteOptions options,
                SerializableConsumer<PasteEvent> listener) {
            pasteRegistrations++;
            return () -> {
            };
        }

        @Override
        public Registration registerFilePaste(Component target,
                UploadHandler uploadHandler) {
            filePasteRegistrations++;
            return () -> {
            };
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private record FakeWriteHandle(Element trigger, ClipboardWrite write,
            boolean hasSuccessCallback,
            boolean hasErrorCallback) implements ClipboardClient.WriteHandle {
        @Override
        public void remove() {
        }
    }

    private record FakeReadHandle(Element trigger,
            ClipboardReadKind kind) implements ClipboardClient.ReadHandle {
        @Override
        public void remove() {
        }
    }
}

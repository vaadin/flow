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

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IFrameTest extends ComponentTest {

    // Actual test methods mostly in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
        addOptionalStringProperty("srcdoc");
        addOptionalStringProperty("name");
        addOptionalStringProperty("allow");

        addProperty("importance", IFrame.ImportanceType.class,
                IFrame.ImportanceType.AUTO, IFrame.ImportanceType.HIGH, true,
                true);

        addProperty("sandbox", IFrame.SandboxType[].class, null,
                new IFrame.SandboxType[] { IFrame.SandboxType.ALLOW_POPUPS,
                        IFrame.SandboxType.ALLOW_MODALS },
                true, true);
    }

    @Test
    void reload() throws Exception {
        Element element = Mockito.mock(Element.class);
        IFrame iframe = new IFrame();
        Field f = Component.class.getDeclaredField("element");

        f.setAccessible(true);
        f.set(iframe, element);

        iframe.reload();

        Mockito.verify(element).executeJs("this.src = this.src");
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    void setSrc_downloadHandler_disabledUpdateModeIsAlways() {
        Element element = Mockito.mock(Element.class);
        class TestIFrame extends IFrame {
            @Override
            public Element getElement() {
                return element;
            }
        }
        // Plain lambda DownloadHandler, not an AbstractDownloadHandler subclass
        DownloadHandler lambda = event -> {
        };

        new TestIFrame().setSrc(lambda);

        ArgumentCaptor<DownloadHandler> captor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        Mockito.verify(element).setAttribute(Mockito.eq("src"),
                captor.capture());
        assertEquals(DisabledUpdateMode.ALWAYS,
                captor.getValue().getDisabledUpdateMode());
    }

    @Test
    void downloadHandler_isSetToInline() {
        Element element = Mockito.mock(Element.class);
        class TestIFrame extends IFrame {
            public TestIFrame(DownloadHandler downloadHandler) {
                super(downloadHandler);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }
        // dummy handler
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        assertFalse(handler.isInline());
        new TestIFrame(handler);
        assertTrue(handler.isInline());
    }

    @Test
    void setSrc_unsafeScheme_throws() {
        IFrame iframe = new IFrame();
        assertThrows(IllegalArgumentException.class,
                () -> iframe.setSrc("javascript:alert(1)"));
    }

    @Test
    void setUnsafeSrc_unsafeScheme_setsSrcWithoutValidation() {
        IFrame iframe = new IFrame();
        iframe.setUnsafeSrc("javascript:alert(1)");
        assertEquals("javascript:alert(1)", iframe.getSrc());
    }

    @Test
    void constructor_unsafeSrc_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new IFrame("javascript:alert(1)"));
    }

    @Test
    void setSrc_detached_unsafeInApplicationConfiguration_throwsOnAttachAndClearsSrc() {
        IFrame iframe = new IFrame();
        // Safe according to the framework default, but not according to the
        // configuration of the application that the iframe ends up in
        iframe.setSrc("http://example.com");

        UI attachTo = createUI("https");
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> attachTo.add(iframe));

        assertTrue(exception.getMessage().contains("http://example.com"),
                "The message should contain the rejected URL");
        assertEquals("", iframe.getSrc(),
                "The rejected src should be cleared so that it isn't sent to the client");
    }

    @Test
    void setSrc_detached_safeInApplicationConfiguration_attachSucceeds() {
        IFrame iframe = new IFrame();
        iframe.setSrc("https://example.com");

        createUI("https").add(iframe);

        assertEquals("https://example.com", iframe.getSrc());
    }

    @Test
    void setSrc_attached_usesConfigurationOfOwnUi() {
        UI attachTo = createUI("https");
        IFrame iframe = new IFrame();
        attachTo.add(iframe);

        assertThrows(IllegalArgumentException.class,
                () -> iframe.setSrc("http://example.com"));
        assertEquals("", iframe.getSrc());
    }

    @Test
    void setUnsafeSrc_afterSetSrc_cancelsValidationOnAttach() {
        IFrame iframe = new IFrame();
        iframe.setSrc("http://example.com");
        iframe.setUnsafeSrc("javascript:alert(1)");

        createUI("https").add(iframe);

        assertEquals("javascript:alert(1)", iframe.getSrc());
    }

    /**
     * Creates a UI that belongs to an application configured to only allow the
     * given URL schemes, without making the service available through
     * {@link VaadinService#getCurrent()}.
     */
    private UI createUI(String safeUrlSchemes) {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setApplicationOrSystemProperty(
                InitParameters.URL_SAFE_SCHEMES, safeUrlSchemes);

        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.getService()).thenReturn(service);

        UI attachTo = new UI();
        attachTo.getInternals().setSession(session);
        return attachTo;
    }
}

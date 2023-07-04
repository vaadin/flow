package com.vaadin.base.devserver.themeeditor;

import com.vaadin.base.devserver.MockVaadinContext;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.testutil.TestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class AbstractThemeEditorTest {

    protected final String FRONTEND_FOLDER = "themeeditor/META-INF/frontend";
    protected final String FRONTEND_NO_THEME_FOLDER = "themeeditor-empty/META-INF/frontend";
    protected final String SELECTOR_WITH_PART = "vaadin-text-field::part(label)";

    protected final int TESTVIEW_CREATE_AND_ATTACH = 22;
    protected final int TEXTFIELD_CREATE = 24;
    protected final int TEXTFIELD_ATTACH = 48;
    protected final int TEXTFIELD_CALL = 44;
    protected final int PINFIELD_CREATE = 18;
    protected final int PINFIELD_ATTACH = 48;
    protected final int PINFIELD2_CREATE = 46;
    protected final int PINFIELD2_ATTACH = 48;
    protected final int INLINEADD_CREATE = 48;
    protected final int INLINEADD_ATTACH = 48;

    protected class TestThemeModifier extends ThemeModifier {

        public TestThemeModifier() {
            super(mockContext);
        }

        @Override
        protected File getFrontendFolder() {
            return TestUtils.getTestFolder(FRONTEND_FOLDER);
        }
    }

    protected class TestJavaSourceModifier extends JavaSourceModifier {

        private VaadinSession session = new MockVaadinSession(null);

        public TestJavaSourceModifier() {
            super(mockContext);
        }

        @Override
        public VaadinSession getSession() {
            return session;
        }
    }

    protected class TestThemeEditorMessageHandler
            extends ThemeEditorMessageHandler {

        private ThemeModifier testThemeModifier = new TestThemeModifier();

        private JavaSourceModifier testSourceModifier = new TestJavaSourceModifier();

        public TestThemeEditorMessageHandler() {
            super(mockContext);
        }

        @Override
        public ThemeModifier getThemeModifier() {
            return testThemeModifier;
        }

        @Override
        public JavaSourceModifier getSourceModifier() {
            return testSourceModifier;
        }
    }

    protected VaadinContext mockContext = new MockVaadinContext();

    // mocking HasOverlayClassName from flow-compontents
    protected static class SpanWithOverlay extends Span {
        public SpanWithOverlay(String text) {
            super(text);
        }

        public void setOverlayClassName(String overlayClassName) {
            // NOP
        }
    }

    protected class MockVaadinSession extends VaadinSession {

        protected static Span pickedComponent = new Span("test");

        protected static Span pickedComponent2 = new Span("test");

        protected static SpanWithOverlay pickedComponent3 = new SpanWithOverlay(
                "test");

        public MockVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public Future<Void> access(Command command) {
            command.execute();
            return CompletableFuture.runAsync(() -> {
            });
        }

        @Override
        public Element findElement(int uiId, int nodeId)
                throws IllegalArgumentException {
            return switch (nodeId) {
            case 2 -> pickedComponent3.getElement();
            case 1 -> pickedComponent2.getElement();
            default -> pickedComponent.getElement();
            };
        }
    }

    @Before
    public void prepare() {
        Lookup lookup = Mockito.mock(Lookup.class);
        mockContext.setAttribute(Lookup.class, lookup);

        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);
        Mockito.when(service.getContext()).thenReturn(mockContext);

        ApplicationConfiguration configuration = Mockito
                .mock(ApplicationConfiguration.class);
        ApplicationConfigurationFactory factory = Mockito
                .mock(ApplicationConfigurationFactory.class);

        Mockito.when(lookup.lookup(ApplicationConfigurationFactory.class))
                .thenReturn(factory);
        Mockito.when(factory.create(Mockito.any())).thenReturn(configuration);
        Mockito.when(configuration.isProductionMode()).thenReturn(false);
        // used for source file manipulation
        Mockito.when(configuration.getJavaSourceFolder())
                .thenReturn(new File("target/test-classes/java"));
        Mockito.when(configuration.getJavaResourceFolder())
                .thenReturn(new File("src/test/resources"));
    }

    protected void copy(String from, String to) {
        try {
            File javaFolder = TestUtils
                    .getTestFolder("java/org/vaadin/example");
            File testViewClean = new File(javaFolder, from);
            File testView = new File(javaFolder, to);
            FileReader reader = new FileReader(testViewClean);
            FileWriter writer = new FileWriter(testView);
            IOUtils.copy(reader, writer);
            IOUtils.closeQuietly(writer, reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void prepareComponentTracker(int nodeId, int createLine,
            int attachLine) {
        try {
            Field createLocationField = ComponentTracker.class
                    .getDeclaredField("createLocation");
            createLocationField.setAccessible(true);
            Map<Component, ComponentTracker.Location> createMap = (Map<Component, ComponentTracker.Location>) createLocationField
                    .get(null);

            ComponentTracker.Location createLocation = new ComponentTracker.Location(
                    "org.vaadin.example.TestView", "TestView.java", "TestView",
                    createLine);

            createMap.put(switch (nodeId) {
            case 1 -> MockVaadinSession.pickedComponent2;
            case 2 -> MockVaadinSession.pickedComponent3;
            default -> MockVaadinSession.pickedComponent;
            }, createLocation);

            Field attachLocationField = ComponentTracker.class
                    .getDeclaredField("attachLocation");
            attachLocationField.setAccessible(true);
            Map<Component, ComponentTracker.Location> attachMap = (Map<Component, ComponentTracker.Location>) attachLocationField
                    .get(null);

            ComponentTracker.Location attachLocation = new ComponentTracker.Location(
                    "org.vaadin.example.TestView", "TestView.java", "TestView",
                    attachLine);

            attachMap.put(switch (nodeId) {
            case 1 -> MockVaadinSession.pickedComponent2;
            case 2 -> MockVaadinSession.pickedComponent3;
            default -> MockVaadinSession.pickedComponent;
            }, attachLocation);
        } catch (Exception ex) {

        }
    }

}

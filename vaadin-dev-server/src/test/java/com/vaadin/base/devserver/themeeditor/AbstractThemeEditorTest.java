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

    protected String FRONTEND_FOLDER = "themeeditor/META-INF/frontend";

    protected String FRONTEND_NO_THEME_FOLDER = "themeeditor-empty/META-INF/frontend";

    protected String SELECTOR_WITH_PART = "vaadin-text-field::part(label)";

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
        protected ThemeModifier getThemeModifier() {
            return testThemeModifier;
        }

        @Override
        public JavaSourceModifier getSourceModifier() {
            return testSourceModifier;
        }
    }

    protected VaadinContext mockContext = new MockVaadinContext();

    protected class MockVaadinSession extends VaadinSession {

        protected static Span pickedComponent = new Span("test");

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
            return pickedComponent.getElement();
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

        FeatureFlags.get(mockContext)
                .setEnabled(FeatureFlags.THEME_EDITOR.getId(), true);
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

    protected void prepareComponentTracker(int line) {
        try {
            Field createLocationField = ComponentTracker.class
                    .getDeclaredField("createLocation");
            createLocationField.setAccessible(true);
            Map<Component, ComponentTracker.Location> createMap = (Map<Component, ComponentTracker.Location>) createLocationField
                    .get(null);

            ComponentTracker.Location location = new ComponentTracker.Location(
                    "org.vaadin.example.TestView", "TestView.java", "TestView",
                    line);

            createMap.put(MockVaadinSession.pickedComponent, location);
        } catch (Exception ex) {

        }
    }

}

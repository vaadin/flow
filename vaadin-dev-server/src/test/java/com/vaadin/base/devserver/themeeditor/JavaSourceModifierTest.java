package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class JavaSourceModifierTest {

    private VaadinContext mockContext = new MockVaadinContext();

    private class TestJavaSourceModifier extends JavaSourceModifier {

        private VaadinSession session = new MockVaadinSession(null);

        public TestJavaSourceModifier() {
            super(mockContext);
        }

        @Override
        public VaadinSession getSession() {
            return session;
        }
    }

    private class MockVaadinSession extends VaadinSession {

        private static Span pickedComponent = new Span("test");

        public MockVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public Future<Void> access(Command command) {
            command.execute();
            return new CompletableFuture<>();
        }

        @Override
        public Element findElement(int uiId, int nodeId)
                throws IllegalArgumentException {
            return pickedComponent.getElement();
        }
    }

    @Before
    public void prepare() {
        copy("TestView_clean.java", "TestView.java");
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

    @After
    public void cleanup() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        File testView = new File(javaFolder, "TestView.java");
        if (testView.exists()) {
            testView.delete();
        }
    }

    private void copy(String from, String to) {
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

    @Test
    public void classNameAdd_javaUpdated_clean() {
        classNameAdd_javaUpdated(22, 23, 24);
    }

    public void classNameAdd_javaUpdated(int declarationLine, int expectedLine1,
            int expectedLine2) {
        prepareComponentTracker(declarationLine);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "beautiful"));

        CompilationUnit cu = getCompilationUnit();
        Node n1 = cu.accept(new LineNumberVisitor(), expectedLine1);
        Node n2 = cu.accept(new LineNumberVisitor(), expectedLine2);

        Assert.assertTrue(n1 instanceof ExpressionStmt);
        Assert.assertTrue(n2 instanceof ExpressionStmt);

        ExpressionStmt expr1 = modifier.createMethodCallExprStmt("textField",
                "addClassName", "bold");
        ExpressionStmt expr2 = modifier.createMethodCallExprStmt("textField",
                "addClassName", "beautiful");

        Assert.assertEquals(expr1, n1);
        Assert.assertEquals(expr2, n2);
    }

    @Test
    public void sameClassNameAdd_javaUpdated_singlePresent() {
        prepareComponentTracker(22);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "bold"));

        CompilationUnit cu = getCompilationUnit();
        Node n1 = cu.accept(new LineNumberVisitor(), 23);
        Node n2 = cu.accept(new LineNumberVisitor(), 24);

        Assert.assertTrue(n1 instanceof ExpressionStmt);
        Assert.assertTrue(n2 instanceof ExpressionStmt);

        ExpressionStmt expr1 = modifier.createMethodCallExprStmt("textField",
                "addClassName", "bold");

        Assert.assertEquals(expr1, n1);
        Assert.assertNotEquals(expr1, n2);
    }

    @Test
    public void classNameRemove_javaUpdated() {
        classNameRemove_javaUpdated(22, 42);
    }

    public void classNameRemove_javaUpdated(int declarationLine,
            int methodCallLine) {
        prepareComponentTracker(declarationLine);
        JavaSourceModifier modifier = new TestJavaSourceModifier();

        // check if statement exists
        Node n1 = getCompilationUnit().accept(new LineNumberVisitor(),
                methodCallLine);
        ExpressionStmt expr1 = modifier.createMethodCallExprStmt("textField",
                "addClassName", "ugly");
        Assert.assertEquals(((ExpressionStmt) n1).getExpression(),
                expr1.getExpression());

        modifier.removeClassNames(0, 0, Arrays.asList("ugly"));

        n1 = getCompilationUnit().accept(new LineNumberVisitor(),
                methodCallLine);
        Assert.assertNull(n1);
    }

    @Test
    public void nonExistingClassNameRemove_noException() {
        prepareComponentTracker(22);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.removeClassNames(0, 0, Arrays.asList("very-ugly"));
    }

    @Test(expected = ModifierException.class)
    public void declarationAsClassProperty_exceptionIsThrown() {
        prepareComponentTracker(18);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "beautiful"));
    }

    @Test(expected = ModifierException.class)
    public void declarationAsInlineArgument_exceptionIsThrown() {
        prepareComponentTracker(44);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "beautiful"));
    }

    @Test
    public void messedFileModified_structurePreserved() {
        copy("TestView_messed.java", "TestView.java");
        classNameRemove_javaUpdated(25, 47);
        classNameAdd_javaUpdated(25, 27, 28);
        try {
            File javaFolder = TestUtils
                    .getTestFolder("java/org/vaadin/example");
            Reader fileReader1 = new FileReader(
                    new File(javaFolder, "TestView.java"));
            Reader fileReader2 = new FileReader(
                    new File(javaFolder, "TestView_messedExpected.java"));
            Assert.assertTrue(IOUtils.contentEquals(fileReader1, fileReader2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareComponentTracker(int line) {
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

    private CompilationUnit getCompilationUnit() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        SourceRoot root = new SourceRoot(javaFolder.toPath());
        return root.parse("", "TestView.java");
    }

}

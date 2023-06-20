package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.SourceRoot;
import com.vaadin.base.devserver.themeeditor.utils.LineNumberVisitor;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.testutil.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.Collectors;

public class JavaSourceModifierTest extends AbstractThemeEditorTest {

    @Before
    public void prepare() {
        super.prepare();
        copy("TestView_clean.java", "TestView.java");
    }

    @After
    public void cleanup() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        File testView = new File(javaFolder, "TestView.java");
        if (testView.exists()) {
            testView.delete();
        }
    }

    @Test
    public void classNameAdded_javaUpdated_componentLocatorRefreshed() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        prepareComponentTracker(1, PINFIELD2_CREATE, PINFIELD2_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();

        modifier.setLocalClassName(0, 0, "bold-field");

        CompilationUnit cu = getCompilationUnit();
        Node n1 = cu.accept(new LineNumberVisitor(), TEXTFIELD_CREATE + 2);
        Assert.assertTrue(n1 instanceof ExpressionStmt);

        Statement expr1 = modifier.createAddClassNameStatement(
                new SimpleName("textField"), "bold-field", false);
        Assert.assertEquals(expr1, n1);

        modifier.setLocalClassName(0, 1, "even-bolder-field");

        cu = getCompilationUnit();
        n1 = cu.accept(new LineNumberVisitor(), PINFIELD2_CREATE + 4);
        Assert.assertTrue(n1 instanceof ExpressionStmt);

        expr1 = modifier.createAddClassNameStatement(
                new SimpleName("pinField2"), "even-bolder-field", false);
        Assert.assertEquals(expr1, n1);
    }

    @Test(expected = ThemeEditorException.class)
    public void nonExistingClassNameRemove_exceptionIsThrown() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.removeLocalClassName(0, 0);
    }

    @Test(expected = ThemeEditorException.class)
    public void declarationAsClassProperty_exceptionIsThrown() {
        prepareComponentTracker(0, PINFIELD_CREATE, PINFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setLocalClassName(0, 0, "bold-field");
    }

    @Test(expected = ThemeEditorException.class)
    public void declarationAsInlineArgument_exceptionIsThrown() {
        prepareComponentTracker(0, INLINEADD_CREATE, INLINEADD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setLocalClassName(0, 0, "bold-field");
    }

    @Test
    public void messedFileModified_structurePreserved() {
        copy("TestView_messed.java", "TestView.java");
        prepareComponentTracker(0, 25, 49);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setLocalClassName(0, 0, "bold-field");
        compareTestView("TestView_messedExpected.java");
    }

    @Test
    public void componentPicked_componentAccessible() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        Assert.assertTrue(modifier.isAccessible(0, 0));
    }

    @Test
    public void componentPicked_componentAccessible_constructor() {
        prepareComponentTracker(0, TESTVIEW_CREATE_AND_ATTACH,
                TESTVIEW_CREATE_AND_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        Assert.assertTrue(modifier.isAccessible(0, 0));
    }

    @Test
    public void componentPicked_componentNotAccessible() {
        prepareComponentTracker(0, INLINEADD_CREATE, INLINEADD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        Assert.assertFalse(modifier.isAccessible(0, 0));
    }

    @Test
    public void localClassName_set_get_remove_replace_suggest() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        testLocalClassName(0, null);
    }

    @Test
    public void localClassName_constructor_set_get_remove_replace_suggest() {
        prepareComponentTracker(0, TESTVIEW_CREATE_AND_ATTACH,
                TESTVIEW_CREATE_AND_ATTACH);
        testLocalClassName(0, null);
    }

    @Test
    public void localClassName_overlay_set_get_remove_replace_suggest() {
        prepareComponentTracker(2, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        testLocalClassName(2, "textField.");
    }

    @Test
    public void localClassName_overlay_constructor_set_get_remove_replace_suggest() {
        prepareComponentTracker(2, TESTVIEW_CREATE_AND_ATTACH,
                TESTVIEW_CREATE_AND_ATTACH);
        testLocalClassName(2, "");
    }

    private void testLocalClassName(int nodeId, String overlayScope) {
        JavaSourceModifier modifier = new TestJavaSourceModifier();

        // local classname does not exist
        String localClassName = modifier.getLocalClassName(0, nodeId);
        Assert.assertNull(localClassName);

        // set local classname
        modifier.setLocalClassName(0, nodeId, "test-name");
        localClassName = modifier.getLocalClassName(0, nodeId);
        Assert.assertNotNull(localClassName);
        Assert.assertEquals("test-name", localClassName);

        // check overlay
        if (overlayScope != null) {
            assertContainsLine(
                    overlayScope + "setOverlayClassName(\"test-name\");");
        }

        // remove local classname
        modifier.removeLocalClassName(0, nodeId);
        localClassName = modifier.getLocalClassName(0, nodeId);
        Assert.assertNull(localClassName);

        // check if file structure is not changed
        compareTestView("TestView_clean.java");

        // suggest new local classname
        String suggestedClassName = modifier.getSuggestedClassName(0, nodeId);
        Assert.assertNotNull(suggestedClassName);
        // suggested classname is derived from tag; span is used in mocks
        // because TextField is not available
        Assert.assertEquals("test-view-span-1", suggestedClassName);

        // set suggested classname
        modifier.setLocalClassName(0, nodeId, suggestedClassName);
        localClassName = modifier.getLocalClassName(0, nodeId);
        Assert.assertNotNull(localClassName);
        Assert.assertEquals(suggestedClassName, localClassName);

        // check overlay
        if (overlayScope != null) {
            assertContainsLine(overlayScope + "setOverlayClassName(\""
                    + suggestedClassName + "\");");
        }

        // suggest new classname
        suggestedClassName = modifier.getSuggestedClassName(0, nodeId);
        Assert.assertNotNull(suggestedClassName);
        Assert.assertEquals("test-view-span-2", suggestedClassName);

        // update suggested classname
        modifier.setLocalClassName(0, nodeId, suggestedClassName);
        localClassName = modifier.getLocalClassName(0, nodeId);
        Assert.assertNotNull(localClassName);
        Assert.assertEquals(suggestedClassName, localClassName);

        // check overlay
        if (overlayScope != null) {
            assertContainsLine(overlayScope + "setOverlayClassName(\""
                    + suggestedClassName + "\");");
        }

        // remove local classname
        modifier.removeLocalClassName(0, nodeId);
        localClassName = modifier.getLocalClassName(0, nodeId);
        Assert.assertNull(localClassName);

        // check if file structure is not changed
        compareTestView("TestView_clean.java");
    }

    @Test
    public void componentTagName() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        String tagName = modifier.getTag(0, 0);
        Assert.assertEquals("span", tagName); // Span used in tests
    }

    private CompilationUnit getCompilationUnit() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        SourceRoot root = new SourceRoot(javaFolder.toPath());
        return LexicalPreservingPrinter.setup(root.parse("", "TestView.java"));
    }

    private void compareTestView(String expectedFile) {
        try {
            File javaFolder = TestUtils
                    .getTestFolder("java/org/vaadin/example");
            String expected = readFile(new File(javaFolder, expectedFile));
            String current = readFile(new File(javaFolder, "TestView.java"));
            Assert.assertEquals(expected, current);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertContainsLine(String line) {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        File file = new File(javaFolder, "TestView.java");
        try (Reader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader(fileReader)) {
            Assert.assertTrue(br.lines().anyMatch(l -> l.trim().equals(line)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readFile(File file) throws IOException {
        try (Reader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader(fileReader)) {
            return br.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

}

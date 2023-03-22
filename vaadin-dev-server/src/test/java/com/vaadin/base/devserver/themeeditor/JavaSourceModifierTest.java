package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
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

        Statement expr1 = modifier.createAddClassNameStatement("textField",
                "bold-field");
        Assert.assertEquals(expr1, n1);

        modifier.setLocalClassName(0, 1, "even-bolder-field");

        cu = getCompilationUnit();
        n1 = cu.accept(new LineNumberVisitor(), PINFIELD2_CREATE + 4);
        Assert.assertTrue(n1 instanceof ExpressionStmt);

        expr1 = modifier.createAddClassNameStatement("pinField2",
                "even-bolder-field");
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
        try {
            File javaFolder = TestUtils
                    .getTestFolder("java/org/vaadin/example");
            Reader fileReader1 = new FileReader(
                    new File(javaFolder, "TestView.java"));
            Reader fileReader2 = new FileReader(
                    new File(javaFolder, "TestView_messedExpected.java"));
            BufferedReader br1 = new BufferedReader(fileReader1);
            BufferedReader br2 = new BufferedReader(fileReader2);

            Assert.assertEquals(br1.lines().collect(Collectors.toList()),
                    br2.lines().collect(Collectors.toList()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void componentPicked_componentAccessible() {
        prepareComponentTracker(0, TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
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
        JavaSourceModifier modifier = new TestJavaSourceModifier();

        // local classname does not exist
        String localClassName = modifier.getLocalClassName(0, 0);
        Assert.assertNull(localClassName);

        // set local classname
        modifier.setLocalClassName(0, 0, "test-name");
        localClassName = modifier.getLocalClassName(0, 0);
        Assert.assertNotNull(localClassName);
        Assert.assertEquals("test-name", localClassName);

        // remove local classname
        modifier.removeLocalClassName(0, 0);
        localClassName = modifier.getLocalClassName(0, 0);
        Assert.assertNull(localClassName);

        // suggest new local classname
        String suggestedClassName = modifier.getSuggestedClassName(0, 0);
        Assert.assertNotNull(suggestedClassName);
        // suggested classname is derived from tag; span is used in mocks
        // because TextField is not available
        Assert.assertEquals("span-1", suggestedClassName);

        // set suggested classname
        modifier.setLocalClassName(0, 0, suggestedClassName);
        localClassName = modifier.getLocalClassName(0, 0);
        Assert.assertNotNull(localClassName);
        Assert.assertEquals(suggestedClassName, localClassName);

        // suggest new classname
        suggestedClassName = modifier.getSuggestedClassName(0, 0);
        Assert.assertNotNull(suggestedClassName);
        Assert.assertEquals("span-2", suggestedClassName);

        // update suggested classname
        modifier.setLocalClassName(0, 0, suggestedClassName);
        localClassName = modifier.getLocalClassName(0, 0);
        Assert.assertNotNull(localClassName);
        Assert.assertEquals(suggestedClassName, localClassName);
    }

    private CompilationUnit getCompilationUnit() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        SourceRoot root = new SourceRoot(javaFolder.toPath());
        return LexicalPreservingPrinter.setup(root.parse("", "TestView.java"));
    }

}

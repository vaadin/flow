package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;
import com.vaadin.base.devserver.themeeditor.utils.LineNumberVisitor;
import com.vaadin.base.devserver.themeeditor.utils.ThemeEditorException;
import com.vaadin.flow.testutil.TestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

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

    @Test(expected = ThemeEditorException.class)
    public void declarationAsClassProperty_exceptionIsThrown() {
        prepareComponentTracker(18);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "beautiful"));
    }

    @Test(expected = ThemeEditorException.class)
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

    @Test
    public void componentPicked_componentAccessible() {
        prepareComponentTracker(22);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        JavaSourceModifier.ComponentMetadata metadata = modifier.getMetadata(0,
                0);
        Assert.assertTrue(metadata.isAccessible());
    }

    @Test
    public void componentPicked_componentNotAccessible() {
        prepareComponentTracker(44);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        JavaSourceModifier.ComponentMetadata metadata = modifier.getMetadata(0,
                0);
        Assert.assertFalse(metadata.isAccessible());
    }

    private CompilationUnit getCompilationUnit() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        SourceRoot root = new SourceRoot(javaFolder.toPath());
        return root.parse("", "TestView.java");
    }

}

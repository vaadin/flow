package com.vaadin.base.devserver.themeeditor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
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
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.vaadin.base.devserver.themeeditor.JavaSourceModifier.UNIQUE_CLASSNAME_PREFIX;

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
        classNameAdd_javaUpdated(TEXTFIELD_CREATE, TEXTFIELD_ATTACH,
                TEXTFIELD_CREATE + 1, TEXTFIELD_CREATE + 2, "textField",
                "beautiful");
    }

    @Test
    public void classNameAdd2_javaUpdated_clean() {
        classNameAdd_javaUpdated(PINFIELD2_CREATE, PINFIELD2_ATTACH,
                PINFIELD2_CREATE + 1, PINFIELD2_CREATE + 2, "pinField2",
                "beautiful");
    }

    public void classNameAdd_javaUpdated(int createLine, int attachLine,
            int expectedLine1, int expectedLine2, String variableName,
            String className) {
        prepareComponentTracker(createLine, attachLine);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", className));

        CompilationUnit cu = getCompilationUnit();
        Node n1 = cu.accept(new LineNumberVisitor(), expectedLine1);
        Node n2 = cu.accept(new LineNumberVisitor(), expectedLine2);

        Assert.assertTrue(n1 instanceof ExpressionStmt);
        Assert.assertTrue(n2 instanceof ExpressionStmt);

        ExpressionStmt expr1 = createMethodCallExprStmt(variableName,
                "addClassName", className);
        ExpressionStmt expr2 = createMethodCallExprStmt(variableName,
                "addClassName", "bold");

        Assert.assertEquals(expr1, n1);
        Assert.assertEquals(expr2, n2);
    }

    @Test
    public void sameClassNameAdd_javaUpdated_singlePresent() {
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold"));
        modifier.setClassNames(0, 0, Arrays.asList("bold"));

        CompilationUnit cu = getCompilationUnit();
        Node n1 = cu.accept(new LineNumberVisitor(), TEXTFIELD_CREATE + 1);
        Node n2 = cu.accept(new LineNumberVisitor(), TEXTFIELD_CREATE + 2);

        Assert.assertTrue(n1 instanceof ExpressionStmt);
        Assert.assertTrue(n2 instanceof ExpressionStmt);

        ExpressionStmt expr1 = createMethodCallExprStmt("textField",
                "addClassName", "bold");

        Assert.assertEquals(expr1, n1);
        Assert.assertNotEquals(expr1, n2);
    }

    @Test
    public void classNameRemove_javaUpdated() {
        classNameRemove_javaUpdated(TEXTFIELD_CREATE, TEXTFIELD_ATTACH,
                TEXTFIELD_CALL);
    }

    public void classNameRemove_javaUpdated(int createLine, int attachLine,
            int methodCallLine) {
        prepareComponentTracker(createLine, attachLine);
        JavaSourceModifier modifier = new TestJavaSourceModifier();

        // check if statement exists
        Node n1 = getCompilationUnit().accept(new LineNumberVisitor(),
                methodCallLine);
        ExpressionStmt expr1 = createMethodCallExprStmt("textField",
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
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.removeClassNames(0, 0, Arrays.asList("very-ugly"));
    }

    @Test(expected = ThemeEditorException.class)
    public void declarationAsClassProperty_exceptionIsThrown() {
        prepareComponentTracker(PINFIELD_CREATE, PINFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "beautiful"));
    }

    @Test(expected = ThemeEditorException.class)
    public void declarationAsInlineArgument_exceptionIsThrown() {
        prepareComponentTracker(INLINEADD_CREATE, INLINEADD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        modifier.setClassNames(0, 0, Arrays.asList("bold", "beautiful"));
    }

    @Test
    public void messedFileModified_structurePreserved() {
        copy("TestView_messed.java", "TestView.java");
        classNameRemove_javaUpdated(25, 49, 47);
        classNameAdd_javaUpdated(25, 49, 27, 28, "textField", "beautiful");
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
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        Assert.assertTrue(modifier.isAccessible(0, 0));
    }

    @Test
    public void componentPicked_componentNotAccessible() {
        prepareComponentTracker(INLINEADD_CREATE, INLINEADD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        Assert.assertFalse(modifier.isAccessible(0, 0));
    }

    @Test
    public void uniqueClassNameNotExists_javaUpdated() {
        prepareComponentTracker(TEXTFIELD_CREATE, TEXTFIELD_ATTACH);
        JavaSourceModifier modifier = new TestJavaSourceModifier();
        String uniqueClassName = modifier.getUniqueClassName(0, 0, true);
        Assert.assertNotNull(uniqueClassName);
        Assert.assertTrue(uniqueClassName.startsWith(UNIQUE_CLASSNAME_PREFIX));

        CompilationUnit cu = getCompilationUnit();
        Node n1 = cu.accept(new LineNumberVisitor(), TEXTFIELD_CREATE + 1);

        Assert.assertTrue(n1 instanceof ExpressionStmt);

        ExpressionStmt expr1 = createMethodCallExprStmt("textField",
                "addClassName", uniqueClassName);

        Assert.assertEquals(expr1, n1);
    }

    @Test
    public void uniqueClassNameExists_valueRetrieved() {
        String expectedClassName = "te-123456789";
        classNameAdd_javaUpdated(TEXTFIELD_CREATE, TEXTFIELD_ATTACH,
                TEXTFIELD_CREATE + 1, TEXTFIELD_CREATE + 2, "textField",
                expectedClassName);

        JavaSourceModifier modifier = new TestJavaSourceModifier();
        String uniqueClassName = modifier.getUniqueClassName(0, 0, false);
        Assert.assertEquals(expectedClassName, uniqueClassName);
    }

    private CompilationUnit getCompilationUnit() {
        File javaFolder = TestUtils.getTestFolder("java/org/vaadin/example");
        SourceRoot root = new SourceRoot(javaFolder.toPath());
        return root.parse("", "TestView.java");
    }

    public ExpressionStmt createMethodCallExprStmt(String variableName,
            String methodName, String argument) {
        return new ExpressionStmt(new MethodCallExpr(methodName)
                .setScope(new NameExpr(variableName)).addArgument(
                        new StringLiteralExpr().setEscapedValue(argument)));
    }

}

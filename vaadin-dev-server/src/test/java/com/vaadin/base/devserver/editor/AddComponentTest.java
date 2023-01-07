package com.vaadin.base.devserver.editor;

import java.io.IOException;

import org.junit.Test;

public class AddComponentTest extends AbstractClassBasedTest {

    @Test
    public void addBeforeFirst() throws IOException {
        setupTestClass("DemoFile");
        String source = "name = new TextField(\"Your name\")";
        String attachSource = "add(name, sayHello, sayHello2";
        String expected = "add(new com.vaadin.flow.component.button.Button(\"Hello\"), name, sayHello, sayHello2";

        testAddBeforeComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addAfterFirst() throws IOException {
        setupTestClass("DemoFile");
        String source = "name = new TextField(\"Your name\")";
        String attachSource = "add(name, sayHello, sayHello2";
        String expected = "add(name, new com.vaadin.flow.component.button.Button(\"Hello\"), sayHello, sayHello2";

        testAddAfterComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addAfterLast() throws IOException {
        setupTestClass("DemoFile");
        String source = "Button sayHello4 = new Button()";
        String attachSource = "add(name, sayHello, sayHello2, sayHello3, sayHello4);";
        String expected = "add(name, sayHello, sayHello2, sayHello3, sayHello4, new com.vaadin.flow.component.button.Button(\"Hello\"));";
        testAddAfterComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addBeforeLast() throws IOException {
        setupTestClass("DemoFile");
        String source = "Button sayHello4 = new Button()";
        String attachSource = "add(name, sayHello, sayHello2, sayHello3, sayHello4);";
        String expected = "add(name, sayHello, sayHello2, sayHello3, new com.vaadin.flow.component.button.Button(\"Hello\"), sayHello4);";
        testAddBeforeComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addAfterInlineComponent() throws IOException {
        setupTestClass("DemoFile");
        String source = "new Button(\"Say hello6\")";
        String attachSource = "add(sayHello5, new Button(\"Say hello6\"));";
        String expected = "add(sayHello5, new Button(\"Say hello6\"), new com.vaadin.flow.component.button.Button(\"Hello\"));";
        testAddAfterComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addBeforeInlineComponent() throws IOException {
        setupTestClass("DemoFile");
        String source = "new Button(\"Say hello6\")";
        String attachSource = "add(sayHello5, new Button(\"Say hello6\"));";
        String expected = "add(sayHello5, new com.vaadin.flow.component.button.Button(\"Hello\"), new Button(\"Say hello6\"));";
        testAddBeforeComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addToEmptyView() throws Exception {
        setupTestClass("EmptyView");
        // Create is the class declaration when there is no constructor
        int create = getLineNumber(testFile, "public class EmptyView");
        int attach = create;
        editor.addComponentInside(testFile, create, attach,
                ComponentType.BUTTON, "Hello world");

        assertTestFileContains(
                "Button helloWorld = new Button(\"Hello world\");");
        assertTestFileContains("add(helloWorld)");
    }

    @Test
    public void addToEmptyViewWithConstructor() throws Exception {
        setupTestClass("EmptyViewWithConstructor");
        // Create is the constructor
        int create = getLineNumber(testFile, "public EmptyView() {");
        int attach = create;
        editor.addComponentInside(testFile, create, attach,
                ComponentType.BUTTON, "Hello world");

        assertTestFileContains(
                "Button helloWorld = new Button(\"Hello world\");");
        assertTestFileContains("add(helloWorld)");
    }

    @Test
    public void addTwiceToEmptyView() throws Exception {
        setupTestClass("EmptyView");
        // Create is the class declaration when there is no constructor
        int create = getLineNumber(testFile, "public class EmptyView");
        int attach = create;
        editor.addComponentInside(testFile, create, attach,
                ComponentType.BUTTON, "Hello world");
        int helloWorldCreate = getLineNumber(testFile, "new Button");
        int helloWorldAttach = getLineNumber(testFile, "add(helloWorld)");

        editor.addComponentAfter(testFile, helloWorldCreate, helloWorldAttach,
                ComponentType.TEXTFIELD, "Goodbye world");

        System.out.println(getTestFileContents());
        assertTestFileContains(
                "Button helloWorld = new Button(\"Hello world\");");
        assertTestFileContains(" add(helloWorld, new TextField(\"Goodbye world\"))");
    }

    private void testAddAfterComponent(String source, String attachSource,
            String expected, ComponentType componentType,
            String... constructorArguments) throws IOException {
        String constructorString = "new " + componentType.getClassName() + "(";
        assertTestFileNotContains(constructorString);
        assertTestFileNotContains(expected);

        int instantiationLineNumber = getLineNumber(testFile, source);
        int attachLineNumber = getLineNumber(testFile, attachSource);
        editor.addComponentAfter(testFile, instantiationLineNumber,
                attachLineNumber, componentType, constructorArguments);
        assertTestFileContains(constructorString);
        assertTestFileContains(expected);
    }

    private void testAddBeforeComponent(String source, String attachSource,
            String expected, ComponentType componentType,
            String... constructorArguments) throws IOException {
        String constructorString = "new " + componentType.getClassName() + "(";
        assertTestFileNotContains(constructorString);
        assertTestFileNotContains(expected);

        int instantiationLineNumber = getLineNumber(testFile, source);
        int attachLineNumber = getLineNumber(testFile, attachSource);
        editor.addComponentBefore(testFile, instantiationLineNumber,
                attachLineNumber, componentType, constructorArguments);
        assertTestFileContains(constructorString);
        assertTestFileContains(expected);
    }

}

package com.vaadin.base.devserver.editor;

import java.io.IOException;

import org.junit.Test;

public class AddComponentTest extends AbstractDemoFileTest {

    @Test
    public void addBeforeFirst() throws IOException {
        String source = "name = new TextField(\"Your name\")";
        String attachSource = "add(name, sayHello, sayHello2";
        String expected = "add(new com.vaadin.flow.component.button.Button(\"Hello\"), name, sayHello, sayHello2";

        testAddBeforeComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addAfterFirst() throws IOException {
        String source = "name = new TextField(\"Your name\")";
        String attachSource = "add(name, sayHello, sayHello2";
        String expected = "add(name, new com.vaadin.flow.component.button.Button(\"Hello\"), sayHello, sayHello2";

        testAddAfterComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addAfterLast() throws IOException {
        String source = "Button sayHello4 = new Button()";
        String attachSource = "add(name, sayHello, sayHello2, sayHello3, sayHello4);";
        String expected = "add(name, sayHello, sayHello2, sayHello3, sayHello4, new com.vaadin.flow.component.button.Button(\"Hello\"));";
        testAddAfterComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addBeforeLast() throws IOException {
        String source = "Button sayHello4 = new Button()";
        String attachSource = "add(name, sayHello, sayHello2, sayHello3, sayHello4);";
        String expected = "add(name, sayHello, sayHello2, sayHello3, new com.vaadin.flow.component.button.Button(\"Hello\"), sayHello4);";
        testAddBeforeComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addAfterInlineComponent() throws IOException {
        String source = "new Button(\"Say hello6\")";
        String attachSource = "add(sayHello5, new Button(\"Say hello6\"));";
        String expected = "add(sayHello5, new Button(\"Say hello6\"), new com.vaadin.flow.component.button.Button(\"Hello\"));";
        testAddAfterComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
    }

    @Test
    public void addBeforeInlineComponent() throws IOException {
        String source = "new Button(\"Say hello6\")";
        String attachSource = "add(sayHello5, new Button(\"Say hello6\"));";
        String expected = "add(sayHello5, new com.vaadin.flow.component.button.Button(\"Hello\"), new Button(\"Say hello6\"));";
        testAddBeforeComponent(source, attachSource, expected,
                ComponentType.BUTTON, "Hello");
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

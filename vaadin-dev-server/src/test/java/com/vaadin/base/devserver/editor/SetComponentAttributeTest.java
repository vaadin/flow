package com.vaadin.base.devserver.editor;

import java.io.IOException;

import org.junit.Test;

public class SetComponentAttributeTest extends AbstractClassBasedTest {

    @Test
    public void edit1() throws IOException {
        setupTestClass("DemoFile");
        String source = "name = new TextField(\"Your name\")";
        String target = "name = new TextField(\"New label\")";

        testComponentAttribute(source, ComponentType.TEXTFIELD, "setLabel",
                "New label", target);
    }

    @Test
    public void edit2() throws IOException {
        setupTestClass("DemoFile");
        String source = "sayHello = new Button(\"Say hello1\");";
        String target = "sayHello = new Button(\"New text\")";

        testComponentAttribute(source, ComponentType.BUTTON, "setText",
                "New text", target);
    }

    @Test
    public void edit3() throws IOException {
        setupTestClass("DemoFile");
        String source = "sayHello3 = new Button(\"Say hello3\");";
        String target = "sayHello3 = new Button(\"New text\");";

        testComponentAttribute(source, ComponentType.BUTTON, "setText",
                "New text", target);
    }

    @Test
    public void edit4() throws IOException {
        setupTestClass("DemoFile");
        String source = "Button sayHello4 = new Button();";
        String target = "sayHello4.setText(\"New text\");";

        testComponentAttribute(source, ComponentType.BUTTON, "setText",
                "New text", target);
    }

    @Test
    public void edit5() throws IOException {
        setupTestClass("DemoFile");
        String source = "Button sayHello5 = new Button();";
        String target = "sayHello5.setText(\"New text\");";

        testComponentAttribute(source, ComponentType.BUTTON, "setText",
                "New text", target);
    }

    @Test
    public void edit6() throws IOException {
        setupTestClass("DemoFile");
        String source = "add(sayHello5, new Button(\"Say hello6\"))";
        String target = "add(sayHello5, new Button(\"New text\"))";

        testComponentAttribute(source, ComponentType.BUTTON, "setText",
                "New text", target);
    }

    @Test
    public void remove1() throws IOException {
        setupTestClass("DemoFile");
        String source = "Button sayHello4 = new Button();";

        int instantiationLineNumber = getLineNumber(testFile, source);
        int attachLineNumber = getLineNumber(testFile, "add(name, sayHello");
        editor.removeComponentAttribute(testFile, instantiationLineNumber,
                attachLineNumber, null, "setText", "Say hello4");
        assertTestFileNotContains("sayHello4.setText(\"Say hello4\");");
    }

    protected void testComponentAttribute(String source,
            ComponentType componentType, String method, String param,
            String target) throws IOException {
        assertTestFileNotContains(target);

        int instantiationLineNumber = getLineNumber(testFile, source);
        int attachLineNumber = getLineNumber(testFile, "add(name, sayHello");
        editor.setComponentAttribute(testFile, instantiationLineNumber,
                attachLineNumber, componentType, method, param);
        assertTestFileContains(target);
    }

}

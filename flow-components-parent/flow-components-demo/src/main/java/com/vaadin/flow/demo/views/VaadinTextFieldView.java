package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.text.field.VaadinTextField;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinTextField} demo.
 */
@ComponentDemo(name = "Vaadin Text Field", href = "vaadin-text-field")
public class VaadinTextFieldView extends DemoView {
    @Override
    void initView() {
        VaadinTextField textField = new VaadinTextField();
        textField.setLabel("Text field label");
        add(textField);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("VaadinTextField textField = new VaadinTextField();\n"
                + "textField.setLabel(\"Text field label\");\n"
                + "layoutContainer.add(textField);");
    }
}

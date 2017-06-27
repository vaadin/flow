package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.text.field.VaadinTextFieldElement;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinTextFieldElement} demo.
 */
@ComponentDemo(name = "Vaadin Text Field", href = "vaadin-text-field-element")
public class VaadinTextFieldElementView extends DemoView {
    @Override
    void initView() {
        VaadinTextFieldElement textField = new VaadinTextFieldElement();
        textField.setLabel("Text field label");
        add(textField);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode(
                "VaadinTextFieldElement textField = new VaadinTextFieldElement();\n"
                        + "textField.setLabel(\"Text field label\");\n"
                        + "layoutContainer.add(textField);");
    }
}

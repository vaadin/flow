package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.checkbox.VaadinCheckboxElement;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinCheckboxElement} demo.
 */
@ComponentDemo(name = "Vaadin Checkbox", href = "vaadin-checkbox-element")
public class VaadinCheckboxElementView extends DemoView {
    @Override
    void initView() {
        VaadinCheckboxElement textField = new VaadinCheckboxElement();
        add(textField);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode(
                "VaadinCheckboxElement textField = new VaadinCheckboxElement();\n"
                        + "layoutContainer.add(textField);");
    }
}

package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.checkbox.VaadinCheckbox;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinCheckboxElement} demo.
 */
@ComponentDemo(name = "Vaadin Checkbox", href = "vaadin-checkbox")
public class VaadinCheckboxView extends DemoView {
    @Override
    void initView() {
        VaadinCheckbox textField = new VaadinCheckbox();
        add(textField);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("VaadinCheckbox textField = new VaadinCheckbox();\n"
                + "layoutContainer.add(textField);");
    }
}

package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.button.VaadinButton;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinButton} demo.
 */
@ComponentDemo(name = "Vaadin Button", href = "vaadin-button")
public class VaadinButtonView extends DemoView {
    @Override
    void initView() {
        VaadinButton button = new VaadinButton();
        button.getElement().setText("Vaadin button");
        add(button);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("VaadinButton button = new VaadinButton();\n"
                + "button.getElement().setText(\"Vaadin button\");\n"
                + "layoutContainer.add(button);");
    }
}

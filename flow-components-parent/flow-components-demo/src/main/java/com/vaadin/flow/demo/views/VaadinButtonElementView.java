package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.button.VaadinButtonElement;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinButtonElement} demo.
 */
@ComponentDemo(name = "Vaadin Button", href = "vaadin-button-element")
public class VaadinButtonElementView extends DemoView {
    @Override
    void initView() {
        VaadinButtonElement button = new VaadinButtonElement();
        button.getElement().setText("Vaadin button");
        add(button);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode(
                "VaadinButtonElement button = new VaadinButtonElement();\n"
                        + "button.getElement().setText(\"Vaadin button\");\n"
                        + "layoutContainer.add(button);");
    }
}

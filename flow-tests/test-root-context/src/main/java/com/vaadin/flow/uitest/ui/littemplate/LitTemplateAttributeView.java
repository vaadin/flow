package com.vaadin.flow.uitest.ui.littemplate;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.littemplate.LitTemplateAttributeView", layout = ViewTestLayout.class)
@Tag("attribute-lit-template")
@JsModule("./lit-templates/AttributeLitTemplate.js")
public class LitTemplateAttributeView extends LitTemplate
        implements HasComponents {

    @Id("div")
    private Div injectedDiv;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setId("template");
        Div div = new Div();
        div.setText(injectedDiv.getTitle().get() + " "
                + injectedDiv.getElement().getProperty("foo") + " "
                + injectedDiv.getElement().getProperty("baz"));
        div.setId("info");
        add(div);
    }
}

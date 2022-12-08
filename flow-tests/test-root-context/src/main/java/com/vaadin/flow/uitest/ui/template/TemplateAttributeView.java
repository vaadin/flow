package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplateAttributeView", layout = ViewTestLayout.class)
@Tag("attribute-template")
@JsModule("./AttributeTemplate.js")
public class TemplateAttributeView extends PolymerTemplate<TemplateModel>
        implements HasComponents {

    @Id("div")
    private Div injectedDiv;

    @Id("disabled")
    private Div disabledDiv;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        setId("template");
        Div div = new Div();
        div.setText(injectedDiv.getTitle().get() + " "
                + injectedDiv.getElement().getProperty("foo") + " "
                + injectedDiv.getElement().getProperty("baz"));
        div.setId("info");
        add(div);

        div = new Div();
        div.setId("disabledInfo");
        div.setText("Enabled: " + disabledDiv.isEnabled());
        add(div);
    }
}

package org.vaadin.example.addon;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;

@Tag(AddonLitComponent.TAG)
@JsModule("./AddonLitComponent.ts")
public class AddonLitComponent extends LitTemplate {

    public static final String TAG = "addon-lit-component";

    @Id("label")
    private Span label;

    public void setLabel(String value) {
        label.setText(value);
    }
}

package com.vaadin.viteapp.views.template;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(TemplateView.ROUTE)
public class TemplateView extends Div {

    public static final String ROUTE = "template";

    public TemplateView() {
        LitComponent litComponent = new LitComponent();
        add(litComponent);

        PolymerComponent polymerComponent = new PolymerComponent();
        add(polymerComponent);

        Input setLabelInput = new Input();
        add(setLabelInput);

        NativeButton setLabelButton = new NativeButton("Set labels");
        setLabelButton.addClickListener(e -> {
            String newLabel = setLabelInput.getValue();
            litComponent.setLabel(newLabel);
            polymerComponent.setLabel(newLabel);
        });
        add(setLabelButton);
    }
}

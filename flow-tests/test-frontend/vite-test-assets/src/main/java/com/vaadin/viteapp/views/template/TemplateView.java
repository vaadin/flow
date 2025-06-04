package com.vaadin.viteapp.views.template;

import org.vaadin.example.addon.AddonLitComponent;

import com.vaadin.flow.component.Component;
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

        AddonLitComponent addonLitComponent = new AddonLitComponent();
        add(addonLitComponent);

        Input setLabelInput = new Input();
        add(setLabelInput);

        NativeButton setLabelButton = new NativeButton("Set labels");
        setLabelButton.addClickListener(e -> {
            String newLabel = setLabelInput.getValue();
            litComponent.setLabel(newLabel);
            polymerComponent.setLabel(newLabel);
            addonLitComponent.setLabel(newLabel);
        });
        add(setLabelButton);

        // Add component by reflection to excercise fallback chunk
        try {
            Class<?> clazz = Class.forName(
                    "com.vaadin.viteapp.views.template.ReflectivelyReferencedComponent");
            add((Component) clazz.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

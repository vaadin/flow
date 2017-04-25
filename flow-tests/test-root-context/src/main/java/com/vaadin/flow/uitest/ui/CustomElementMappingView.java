package com.vaadin.flow.uitest.ui;

import java.util.Arrays;
import java.util.List;

import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Title;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.uitest.ui.template.DomRepeatPolymerTemplate;
import com.vaadin.flow.uitest.ui.template.EventHandlerPolymerTemplate;
import com.vaadin.flow.uitest.ui.template.OneWayPolymerBindingTemplate;
import com.vaadin.flow.uitest.ui.template.RouterLinksTemplate;
import com.vaadin.flow.uitest.ui.template.SubPropertyModelTemplate;
import com.vaadin.flow.uitest.ui.template.TwoWayPolymerBindingTemplate;
import com.vaadin.server.CustomElementRegistry;

@Title("Registered custom elements view")
public class CustomElementMappingView extends AbstractDivView {

    List<Class<? extends PolymerTemplate>> customElements = Arrays.asList(
            DomRepeatPolymerTemplate.class, EventHandlerPolymerTemplate.class,
            OneWayPolymerBindingTemplate.class, RouterLinksTemplate.class,
            SubPropertyModelTemplate.class, TwoWayPolymerBindingTemplate.class);

    @Override
    protected void onShow() {
        removeAll();

        customElements.forEach(clazz -> addKeyIfRegistered(clazz));
    }

    private void addKeyIfRegistered(Class<? extends PolymerTemplate> clazz) {
        String tagName = clazz.getAnnotation(Tag.class).value();
        CustomElementRegistry registry = CustomElementRegistry.getInstance();
        if (registry.isRegisteredCustomElement(tagName)
                && registry.getRegisteredCustomElement(tagName).equals(clazz)) {
            addKey(tagName);
        }
    }

    private void addKey(String key) {
        Div titleView = new Div();
        titleView.setText(key);
        titleView.getElement().setAttribute("custom", true);
        add(titleView);
    }

}

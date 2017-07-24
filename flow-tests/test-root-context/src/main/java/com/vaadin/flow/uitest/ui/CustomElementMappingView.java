package com.vaadin.flow.uitest.ui;

import java.util.Arrays;
import java.util.List;

import com.vaadin.annotations.Tag;
import com.vaadin.annotations.Title;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.uitest.ui.template.DomRepeatView;
import com.vaadin.flow.uitest.ui.template.EventHandlerView;
import com.vaadin.flow.uitest.ui.template.OneWayPolymerBindingView;
import com.vaadin.flow.uitest.ui.template.RouterLinksTemplate;
import com.vaadin.flow.uitest.ui.template.SubPropertyModelTemplate;
import com.vaadin.flow.uitest.ui.template.TwoWayPolymerBindingView;
import com.vaadin.server.startup.CustomElementRegistry;

@Title("Registered custom elements view")
public class CustomElementMappingView extends AbstractDivView {

    List<Class<? extends PolymerTemplate>> customElements = Arrays.asList(
            DomRepeatView.class, EventHandlerView.class,
            OneWayPolymerBindingView.class, RouterLinksTemplate.class,
            SubPropertyModelTemplate.class, TwoWayPolymerBindingView.class);

    @Override
    protected void onShow() {
        removeAll();

        customElements.forEach(this::addKeyIfRegistered);
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

package com.vaadin.flow.uitest.ui;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.DomRepeatView;
import com.vaadin.flow.uitest.ui.template.EventHandlerView;
import com.vaadin.flow.uitest.ui.template.OneWayPolymerBindingView;
import com.vaadin.flow.uitest.ui.template.SubPropertyModelTemplate;
import com.vaadin.flow.uitest.ui.template.TwoWayPolymerBindingView;
import com.vaadin.router.PageTitle;
import com.vaadin.router.Route;
import com.vaadin.server.startup.CustomElementRegistry;
import com.vaadin.ui.Tag;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

@Route(value = "com.vaadin.flow.uitest.ui.CustomElementMappingView", layout = ViewTestLayout.class)
@PageTitle("Registered custom elements view")
public class CustomElementMappingView extends AbstractDivView {

    List<Class<? extends PolymerTemplate<?>>> customElements = Arrays.asList(
            DomRepeatView.class, EventHandlerView.class,
            OneWayPolymerBindingView.class, SubPropertyModelTemplate.class,
            TwoWayPolymerBindingView.class);

    @Override
    protected void onShow() {
        removeAll();

        customElements.forEach(this::addKeyIfRegistered);
    }

    private void addKeyIfRegistered(Class<? extends PolymerTemplate<?>> clazz) {
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

/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.servlet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

public class ViewTestLayout extends Div
        implements RouterLayout, AfterNavigationObserver {

    private Element element = ElementFactory.createDiv();
    private Element viewContainer = ElementFactory.createDiv();
    private Element viewSelect = ElementFactory.createSelect();

    @Route(value = "", layout = ViewTestLayout.class)
    public static class BaseNavigationTarget extends Div {
        public BaseNavigationTarget() {
            setText(this.getClass().getSimpleName());
            setId("name-div");
        }
    }

    public ViewTestLayout() {

        element.setAttribute("id", "main-layout");

        List<Class<? extends Component>> classes = new ArrayList<>(
                ViewTestServlet.getViewLocator().getAllViewClasses());
        classes.removeIf(e -> !e.getName().endsWith("View"));

        Comparator<Class<? extends Component>> comparator = Comparator
                .comparing(Class::getName);
        Collections.sort(classes, comparator);

        String lastPackage = "";
        viewSelect.appendChild(ElementFactory.createOption());
        Element optionGroup = null;
        for (Class<? extends Component> c : classes) {
            if (!c.getPackage().getName().equals(lastPackage)) {
                lastPackage = c.getPackage().getName();
                optionGroup = new Element("optgroup");
                optionGroup.setAttribute("label",
                        c.getPackage().getName().replaceAll("^.*\\.", ""));
                viewSelect.appendChild(optionGroup);
            }
            Element option = ElementFactory.createOption(c.getSimpleName())
                    .setAttribute("value", c.getName());
            option.setAttribute("id", c.getSimpleName());

            optionGroup.appendChild(option);
        }

        viewSelect.addEventListener("change", e -> {
            UI ui = UI.getCurrent();
            ui.navigate(viewSelect.getProperty("value"));
        }).synchronizeProperty("value");

        element.appendChild(viewSelect, ElementFactory.createHr(),
                viewContainer);

        getElement().appendChild(element);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        // Defer value setting until all option elements have been attached
        UI.getCurrent().getPage().executeJs(
                "setTimeout(function() {$0.value = $1}, 0)", viewSelect,
                event.getLocation().getPath());
    }

}

/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

public class ViewTestLayout extends Div
        implements RouterLayout, BeforeEnterObserver {

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

        viewSelect.synchronizeProperty("value", "change");
        viewSelect.addEventListener("change", e -> {
            UI ui = UI.getCurrent();
            ui.navigate(viewSelect.getProperty("value"));
        });

        element.appendChild(viewSelect, ElementFactory.createHr(),
                viewContainer);

        getElement().appendChild(element);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Defer value setting until all option elements have been attached
        UI.getCurrent().getPage().executeJs(
                "setTimeout(function() {$0.value = $1}, 0)", viewSelect,
                event.getLocation().getPath());
    }
}

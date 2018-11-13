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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.AttachListenerView", layout = ViewTestLayout.class)
public class AttachListenerView extends AbstractDivView {

    public AttachListenerView() {
        Div group = new Div();

        Input firstAsHost = createRadioButton(group, "firstAsHost", "host",
                "Add to first");
        Input middleAsHost = createRadioButton(group, "middleAsHost", "host",
                "Add to middle");
        Input lastAsHost = createRadioButton(group, "lastAsHost", "host",
                "Add to last");
        add(group);

        group = new Div();
        Input firstAsChild = createRadioButton(group, "firstAsChild", "child",
                "First as child");
        Input middleAsChild = createRadioButton(group, "middleAsChild", "child",
                "Middle as child");
        Input lastAsChild = createRadioButton(group, "lastAsChild", "child",
                "Last as child");
        add(group);

        group = new Div();
        Input attachListenerToFirst = createRadioButton(group,
                "attachListenerToFirst", "listener",
                "Attach listener to first");
        Input attachListenerToMiddle = createRadioButton(group,
                "attachListenerToMiddle", "listener",
                "Attach listener to middle");
        Input attachListenerToLast = createRadioButton(group,
                "attachListenerToLast", "listener", "Attach listener to last");
        add(group);

        NativeButton submit = new NativeButton("Submit", click -> {
            Span first = new Span("First");
            Span middle = new Span("Middle");
            Span last = new Span("Last");

            Span host;
            if (isChecked(firstAsHost)) {
                host = first;
            } else if (isChecked(middleAsHost)) {
                host = middle;
            } else {
                host = last;
            }

            Span child;
            if (isChecked(firstAsChild)) {
                child = first;
            } else if (isChecked(middleAsChild)) {
                child = middle;
            } else {
                child = last;
            }

            Span listener;
            if (isChecked(attachListenerToFirst)) {
                listener = first;
            } else if (isChecked(attachListenerToMiddle)) {
                listener = middle;
            } else {
                listener = last;
            }

            listener.addAttachListener(event -> {
                if (event.isInitialAttach()) {
                    host.add(child);
                }
            });

            Div result = new Div(first, middle, last);
            result.setId("result");
            add(result);
        });

        submit.setId("submit");
        add(submit);
    }

    private Input createRadioButton(HasComponents parent, String id,
            String group, String text) {

        Input input = new Input();
        input.getElement().setAttribute("type", "radio")
                .setAttribute("name", group).setAttribute("value", text)
                .addSynchronizedProperty("checked")
                .addSynchronizedPropertyEvent("change");
        input.setId(id);

        Label label = new Label(text);
        label.setFor(id);

        parent.add(input, label);

        return input;
    }

    private boolean isChecked(Input input) {
        return input.getElement().getProperty("checked") != null;
    }
}

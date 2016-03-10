/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.servlet;

import java.util.Arrays;
import java.util.Comparator;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.HasSubView;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.uitest.ui.BasicElementView;
import com.vaadin.hummingbird.uitest.ui.SynchronizedPropertyView;
import com.vaadin.ui.UI;

public class ViewTestLayout implements HasSubView {

    private Element element = new Element("div");
    private Element viewContainer = new Element("div");

    public ViewTestLayout() {
        Element viewSelect = new Element("select");
        @SuppressWarnings("unchecked")
        Class<? extends View>[] classes = new Class[] {
                SynchronizedPropertyView.class, BasicElementView.class };
        Arrays.sort(classes, new Comparator<Class>() {
            @Override
            public int compare(Class o1, Class o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        String lastPackage = "";
        viewSelect.appendChild(new Element("option"));
        Element optionGroup = null;
        for (Class<? extends View> c : classes) {
            if (!c.getPackage().getName().equals(lastPackage)) {
                lastPackage = c.getPackage().getName();
                optionGroup = new Element("optgroup");
                optionGroup.setAttribute("label",
                        c.getPackage().getName().replaceAll("^.*\\.", ""));
                viewSelect.appendChild(optionGroup);
            }
            Element option = new Element("option").setAttribute("value",
                    c.getName());
            option.setTextContent(c.getSimpleName());

            // TODO Set current view selected

            optionGroup.appendChild(option);
        }
        viewSelect.addEventListener("change", e -> {
            // FIXME Does not navigate
            UI.getCurrent().getPage().getHistory().pushState(null,
                    e.getEventData().getString("element.value"));
        } , "element.value");
        // UI.getCurrent().getPage().executeJavaScript("$0.addEventListener('change',
        // function() {});", viewSelect);

        element.appendChild(viewSelect, new Element("hr"), viewContainer);
        viewContainer.appendChild(new Element("div"));
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public void setSubView(View subView) {
        viewContainer.setChild(0, subView.getElement());

    }

}

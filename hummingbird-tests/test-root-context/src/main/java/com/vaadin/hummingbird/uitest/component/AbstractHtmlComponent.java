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
package com.vaadin.hummingbird.uitest.component;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.Component;

public class AbstractHtmlComponent implements Component {

    private Element element;

    public AbstractHtmlComponent(Element element) {
        this.element = element;
        element.setComponent(this);
    }

    @Override
    public Element getElement() {
        return element;
    }

    public void setId(String id) {
        getElement().setAttribute("id", id);
    }

    public String getId() {
        return getElement().getAttribute("id");
    }

    public void addClass(String className) {
        getElement().getClassList().add(className);
    }

    protected void setText(String text) {
        getElement().setTextContent(text);
    }

}

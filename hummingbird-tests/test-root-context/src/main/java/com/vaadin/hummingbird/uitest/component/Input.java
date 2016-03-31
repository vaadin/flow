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

import com.vaadin.hummingbird.dom.ElementFactory;

public class Input extends AbstractHtmlComponent {

    public Input() {
        super(ElementFactory.createInput());
        getElement().setSynchronizedProperties("value");
        getElement().setSynchronizedPropertiesEvents("change");

    }

    public void setPlaceholder(String placeholder) {
        getElement().setAttribute("placeholder", placeholder);
    }

    public String getPlaceholder() {
        return getElement().getAttribute("placeholder");
    }

    public String getValue() {
        return getElement().getProperty("value");
    }

    public void setValue(String string) {
        getElement().setProperty("value", "");
    }

}

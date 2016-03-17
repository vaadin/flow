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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.ui.Page;
import com.vaadin.ui.UI;

public abstract class TestView implements View {

    private Element element;

    public TestView() {
        element = new Element("div");
    }

    protected abstract void onShow();

    @Override
    public void onLocationChange(LocationChangeEvent event) {
        onShow();
    }

    @Override
    public Element getElement() {
        return element;
    }

    protected UI getUI() {
        return UI.getCurrent();
    }

    protected Page getPage() {
        return getUI().getPage();
    }

}

/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Route("com.vaadin.flow.uitest.ui.WebComponentsView")
public class WebComponentsView extends AbstractDivView {

    public WebComponentsView() {
        Element div = ElementFactory.createDiv("Web components v1");
        getElement().appendChild(div);
    }
}

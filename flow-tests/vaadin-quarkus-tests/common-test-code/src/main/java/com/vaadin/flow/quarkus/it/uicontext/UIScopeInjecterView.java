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
package com.vaadin.flow.quarkus.it.uicontext;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("injecter")
public class UIScopeInjecterView extends Div {

    @Inject
    UIScopedBean bean;

    @Inject
    UIScopedLabel label;

    @PostConstruct
    private void init() {
        Div div = new Div();
        div.setId(UIContextRootView.UI_SCOPED_BEAN_ID);
        div.setText(bean.getId());
        add(div);

        add(label);
    }
}

/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("browser-tab-scope")
public class BrowserTabScopeTarget extends Div {

    public BrowserTabScopeTarget(@Autowired BrowserTabScopedBean bean,
            @Autowired UIScopedBean uiScopedBean) {
        Span tabBean = new Span(bean.getUid());
        tabBean.setId("tab-bean");
        Span uiBean = new Span(uiScopedBean.getUid());
        uiBean.setId("ui-bean");
        add(tabBean, uiBean);
    }
}

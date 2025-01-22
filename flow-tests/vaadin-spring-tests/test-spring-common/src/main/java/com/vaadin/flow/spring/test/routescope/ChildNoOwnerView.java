/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.test.routescope;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route(value = "child-no-owner", layout = ParentNoOwnerView.class)
public class ChildNoOwnerView extends Div {

    private @Autowired BeanNoOwner bean;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            RouterLink link = new RouterLink("parent", ParentNoOwnerView.class);
            link.setId("to-parent");
            add(link);

            Div div = new Div();
            div.setId("child-info");
            div.getElement().getStyle().set("display", "block");
            div.setText(bean.getValue());
            add(div);
        }
    }
}

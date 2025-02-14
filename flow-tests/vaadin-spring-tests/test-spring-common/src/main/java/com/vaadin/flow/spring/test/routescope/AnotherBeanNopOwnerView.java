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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("another-no-owner")
public class AnotherBeanNopOwnerView extends Div {

    public AnotherBeanNopOwnerView(@Autowired DivNoOwner childDiv) {
        setId("another-no-owner");
        add(childDiv);

        RouterLink link = new RouterLink("no-owner-view",
                BeansWithNoOwnerView.class);
        link.getElement().getStyle().set("display", "block");
        link.setId("no-owner-view");
        add(link);
    }
}

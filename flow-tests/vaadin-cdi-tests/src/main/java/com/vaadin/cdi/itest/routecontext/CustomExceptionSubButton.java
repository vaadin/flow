/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.cdi.itest.routecontext;

import java.util.UUID;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.html.NativeButton;

@RouteScoped
@RouteScopeOwner(ErrorHandlerView.class)
@CdiComponent
public class CustomExceptionSubButton extends AbstractCountedView {

    public CustomExceptionSubButton() {
        NativeButton button = new NativeButton();
        button.setId("custom-exception-button");
        button.setText(UUID.randomUUID().toString());
        add(button);
    }
}

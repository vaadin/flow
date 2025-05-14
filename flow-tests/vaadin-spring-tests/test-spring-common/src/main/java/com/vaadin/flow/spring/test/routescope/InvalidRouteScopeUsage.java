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
import com.vaadin.flow.spring.annotation.RouteScopeOwner;

@Route(value = "invalid-layout", layout = Layout.class)
public class InvalidRouteScopeUsage extends Div {

    // Injection point is valid and there is a bean eligible for injection. But
    // the scope doesn't exist: so this should fail
    public InvalidRouteScopeUsage(
            @Autowired @RouteScopeOwner(ButtonInLayout.class) ButtonScopedBean bean) {
        setId("invalid-bean");
    }

}

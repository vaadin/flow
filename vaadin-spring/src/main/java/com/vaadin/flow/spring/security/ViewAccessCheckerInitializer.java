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
package com.vaadin.flow.spring.security;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.ViewAccessChecker;

/**
 * Helper for checking access to views.
 *
 * @deprecated ViewAccessChecker has been replaced by
 *             {@link com.vaadin.flow.server.auth.NavigationAccessControl}.
 */
@Deprecated(forRemoval = true, since = "24.3")
public class ViewAccessCheckerInitializer implements VaadinServiceInitListener {

    @Autowired
    private ViewAccessChecker viewAccessChecker;

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource()
                .addUIInitListener(uiInitEvent -> uiInitEvent.getUI()
                        .addBeforeEnterListener(viewAccessChecker));
    }

}

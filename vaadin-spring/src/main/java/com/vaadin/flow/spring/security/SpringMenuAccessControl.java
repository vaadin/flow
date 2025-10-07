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

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.DefaultMenuAccessControl;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.flow.server.menu.AvailableViewInfo;

/**
 * A Spring specific menu access control that falls back to Spring mechanisms
 * for view access checking, when the generic mechanisms do not work.
 * <p>
 * </p>
 * In Spring Boot application, a {@link SpringMenuAccessControl} is provided by
 * default, if Spring Security is available.
 */
public class SpringMenuAccessControl extends DefaultMenuAccessControl {

    @Override
    public boolean canAccessView(AvailableViewInfo viewInfo) {
        VaadinRequest request = VaadinRequest.getCurrent();
        return MenuAccessControl.canAccessView(viewInfo,
                SecurityUtil.getPrincipal(request),
                SecurityUtil.getRolesChecker(request));
    }
}

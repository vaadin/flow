/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 *
 * @since 24.5.1
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

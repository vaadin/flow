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
package com.vaadin.base.devserver;

import javax.servlet.annotation.HandlesTypes;
import java.util.Set;

import com.vaadin.base.devserver.startup.DevModeInitializer;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.VaadinInitializerException;

/**
 * Provides API to access to the {@link DevModeHandler} instance by a
 * {@link VaadinService}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeHandlerManagerImpl implements DevModeHandlerManager {

    @Override
    public Class<?>[] getHandlesTypes() {
        return DevModeInitializer.class.getAnnotation(HandlesTypes.class)
                .value();
    }

    @Override
    public void initDevModeHandler(Set<Class<?>> classes, VaadinContext context)
            throws VaadinInitializerException {
        DevModeInitializer.initDevModeHandler(classes, context);
    }

    @Override
    public DevModeHandler getDevModeHandler() {
        return DevModeHandlerImpl.getDevModeHandler();
    }

    @Override
    public boolean isDevModeAlreadyStarted(VaadinContext context) {
        return false;
    }
}

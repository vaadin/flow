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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerAccessor;
import com.vaadin.flow.server.VaadinService;

/**
 * Provides API to access to the {@link DevModeHandler} instance by a
 * {@link VaadinService}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class DevModeHandlerAccessorImpl implements DevModeHandlerAccessor {

    @Override
    public DevModeHandler getDevModeHandler(VaadinService service) {
        if (service.getDeploymentConfiguration().isProductionMode()) {
            getLogger().debug(
                    "DevModeHandlerAccessImpl::getDevModeHandler is called in production mode.");
            return null;
        }
        if (!service.getDeploymentConfiguration().enableDevServer()) {
            getLogger().debug(
                    "DevModeHandlerAccessImpl::getDevModeHandler is called when dev server is disabled.");
            return null;
        }
        return DevModeHandlerImpl.getDevModeHandler();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(DevModeHandler.class);
    }
}

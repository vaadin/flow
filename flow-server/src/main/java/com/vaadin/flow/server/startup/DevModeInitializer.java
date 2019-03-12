/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.DevModeHandler;

/**
 * Static class for starting the {@link DevModeHandler} and getting
 * instance information for a running instance.
 */
public final class DevModeInitializer implements Serializable {

    private static AtomicReference<DevModeHandler> devmodeHandler = new AtomicReference<>();

    /**
     * Start the dev mode handler if none has been started yet.
     *
     * @param configuration
     *         deployment configuration
     */
    public static void start(DeploymentConfiguration configuration) {
        devmodeHandler.compareAndSet(null,
                DevModeHandler.createInstance(configuration));
    }

    /**
     * Get the instantiated DevModeHandler.
     *
     * @return devModeHandler or {@code null} if not started
     */
    public static DevModeHandler getDevModeHandler() {
        return devmodeHandler.get();
    }
}

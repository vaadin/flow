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

package com.vaadin.flow.hotswap;

import java.util.Set;

import com.vaadin.flow.server.VaadinService;

/*
 * Event fired when hotswap has been completed.
 */
public class HotswapCompleteEvent {

    private final Set<Class<?>> classes;
    private final VaadinService vaadinService;
    private final boolean redefined;

    public HotswapCompleteEvent(VaadinService vaadinService,
            Set<Class<?>> classes, boolean redefined) {
        this.classes = classes;
        this.vaadinService = vaadinService;
        this.redefined = redefined;
    }

    /**
     * Gets the classes that were updated.
     *
     * @return the updated classes
     */
    public Set<Class<?>> getClasses() {
        return classes;
    }

    /**
     * Checks if the classes were redefined (as opposed to being new classes).
     *
     * @return {@literal true} if the classes have been redefined by hotswap
     */
    public boolean isRedefined() {
        return redefined;
    }

    /**
     * Gets the Vaadin service.
     *
     * @return the vaadin service
     */
    public VaadinService getService() {
        return vaadinService;
    }

}

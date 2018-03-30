/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.component;

import com.vaadin.flow.component.page.Push;

/**
 * Interface for configuring the initial push channel settings.
 *
 * Class implementing this interface must have
 * a zero-argument constructor.
 *
 * @since
 */
@FunctionalInterface
public interface PushConfigurator {

    /**
     * Configure initial settings on {@link PushConfiguration}
     *
     * @param configuration push channel configuration
     * @param pushSetting initial push settings
     */
    void configurePush(PushConfiguration configuration, Push pushSetting);

}

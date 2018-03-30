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
package com.vaadin.flow.server.communication;

import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.PushConfigurator;
import com.vaadin.flow.component.page.Push;

/**
 * Default implementation of {@link PushConfigurator}.
 *
 * Applies settings from {@link Push} annotation and
 * sets the push connection factory that creates
 * {@link AtmospherePushConnection} instances
 *
 * @since
 */
public class DefaultPushConfigurator implements PushConfigurator {

    @Override
    public void configurePush(PushConfiguration configuration, Push pushSetting) {
        configuration.applyConnectionFactoryIfPossible(AtmospherePushConnection::new);
        configuration.setPushMode(pushSetting.value());
        configuration.setTransport(pushSetting.transport());
    }
}

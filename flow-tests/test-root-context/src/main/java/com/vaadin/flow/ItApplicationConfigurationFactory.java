/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow;

import java.util.Map;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

@Component(service = ApplicationConfigurationFactory.class, property = org.osgi.framework.Constants.SERVICE_RANKING
        + ":Integer=" + Integer.MAX_VALUE)
public class ItApplicationConfigurationFactory
        extends DefaultApplicationConfigurationFactory {

    @Override
    protected ApplicationConfigurationImpl doCreate(VaadinContext context,
            FallbackChunk chunk, Map<String, String> properties) {
        properties.put(Constants.ALLOW_APPSHELL_ANNOTATIONS,
                Boolean.TRUE.toString());
        return super.doCreate(context, chunk, properties);
    }
}

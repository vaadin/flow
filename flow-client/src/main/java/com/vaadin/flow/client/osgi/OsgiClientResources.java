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
package com.vaadin.flow.client.osgi;

import java.io.InputStream;

import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.client.ClientResources;

/**
 * OSGi service to access to the client-side resources.
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
@Component(immediate = true, service = ClientResources.class)
public class OsgiClientResources implements ClientResources {

    @Override
    public InputStream getResource(String path) {
        return OsgiClientResources.class.getResourceAsStream(path);
    }
}

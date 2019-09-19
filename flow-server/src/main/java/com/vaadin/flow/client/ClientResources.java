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
package com.vaadin.flow.client;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Service which allows to get resources from the client side module.
 * <p>
 * {@link DefaultClientResources} is used if we are in plain Java environment
 * where resources may be gotten via the {@code Class::getResourceAsStream()}
 * (it doesn't always work out of the box since the resources are in the
 * different bundle).
 * <p>
 * In OSGi Environment a special service is registered which allows to get the
 * resources.
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
@FunctionalInterface
public interface ClientResources extends Serializable {

    /**
     * Get content of the resource in the client-side module.
     *
     * @param path
     *            the resource path
     * @return the content of the resource as InputStream or {@code null} if
     *         there is no resource with the {@code path}
     */
    InputStream getResource(String path);
}

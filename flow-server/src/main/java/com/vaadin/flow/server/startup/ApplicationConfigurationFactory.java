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
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.VaadinContext;

/**
 * A factory for {@link ApplicationConfiguration}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface ApplicationConfigurationFactory {

    /**
     * Creates a new instance of {@link ApplicationConfiguration} for the given
     * {@code context}.
     *
     * @param context
     *            the context to create a configuration for
     * @return the configuration created based on the {@code context}
     */
    ApplicationConfiguration create(VaadinContext context);
}

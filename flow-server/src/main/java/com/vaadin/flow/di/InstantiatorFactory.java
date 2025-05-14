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
package com.vaadin.flow.di;

import com.vaadin.flow.server.VaadinService;

/**
 * A factory for an {@link Instantiator}.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
public interface InstantiatorFactory {

    /**
     * Create an {@link Instantiator} using the provided {@code service}.
     *
     * @param service
     *            a {@code VaadinService} to create an {@code Instantiator} for
     * @return an instantiator for the service or null if this factory is not
     *         able to create an instantiator for the provided service
     */
    Instantiator createInstantitor(VaadinService service);
}

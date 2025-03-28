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

package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.frontend.FrontendUtils;

/**
 * Interface for providing client side routes.
 *
 * @deprecated Provider is deprecated, use
 *             {@link FrontendUtils#getClientRoutes()} instead.
 */
@Deprecated(forRemoval = true)
public interface ClientRoutesProvider extends Serializable {

    /**
     * Get a list of client side routes.
     *
     * @return a list of client side routes. Not null.
     */
    List<String> getClientRoutes();
}

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

package com.vaadin.flow.router;

import java.io.Serializable;

/**
 * Allows to resolve navigation target title dynamically at runtime.
 * <p>
 * NOTE: It is not legal for a class to both implement {@link HasDynamicTitle}
 * and have a {@link PageTitle} annotation.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
@FunctionalInterface
public interface HasDynamicTitle extends Serializable {

    /**
     * Gets the title of this navigation target.
     *
     * @return the title of this navigation target
     */
    String getPageTitle();
}

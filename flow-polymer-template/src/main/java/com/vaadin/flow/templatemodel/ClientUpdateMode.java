/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.templatemodel;

/**
 * A mode for whether a model property may be updated from the client.
 *
 * @see AllowClientUpdates
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public enum ClientUpdateMode {
    /**
     * Always allow updating the property.
     */
    ALLOW,
    /**
     * Never allow updating the property.
     */
    DENY,
    /**
     * Allow updating the property if there is a corresponding two-way binding
     * in the template. This is the default mode that is used if nothing else
     * has been defined for a property.
     */
    IF_TWO_WAY_BINDING;
}

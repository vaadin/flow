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
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Configures the initial page contents.
 *
 * @since 1.0
 * @deprecated Deprecated due to multiple issues on feature design, like this
 * won't work together with the {@link com.vaadin.flow.router.PreserveOnRefresh}
 * annotation. Will not be removed until <em>after the next long term support
 * version</em> (targeted Vaadin 23).
 * <p>
 * For Vaadin 14, use {@link BootstrapListener} instead, which provides API for
 * modifying the bootstrap page and access to the {@link UI}, which provides
 * further replacement API like {@link UI#getLoadingIndicatorConfiguration()}.
 * <p>
 * For Vaadin 15+, use {@code AppShellConfigurator} instead.
 */
@Deprecated
@FunctionalInterface
public interface PageConfigurator extends Serializable {

    /**
     * Configure the initial page settings when called.
     *
     * @param settings
     *            initial page settings
     */
    void configurePage(InitialPageSettings settings);
}

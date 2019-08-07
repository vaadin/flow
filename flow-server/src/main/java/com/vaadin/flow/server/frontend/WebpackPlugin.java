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
package com.vaadin.flow.server.frontend;

import java.io.Serializable;

/**
 * Interface for webpack plugin wrapper which can be inserted into webpack
 * configuration by {@link TaskUpdateWebpack}.
 */
public interface WebpackPlugin extends Serializable {
    /**
     * Get the import statement for this plugin. Note: the plugin dependency
     * must be declared in dev_dependencies of package.json.
     *
     * @return the webpack plugin import statement
     */
    String getImportStatement();

    /**
     * Get the plugin content which should be appended in the 'webpack
     * .generated.config'.
     *
     * @return the plugin declaration and configuration.
     */
    String getContent();
}

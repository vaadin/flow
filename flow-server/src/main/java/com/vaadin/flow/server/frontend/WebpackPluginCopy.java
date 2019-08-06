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

/**
 * Webpack copy plugin wrapper class for generating webpack config.
 */
public class WebpackPluginCopy implements WebpackPlugin {

    private final String source;
    private final String destination;

    WebpackPluginCopy(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String getImportStatement() {
        return "const CopyWebpackPlugin = require('copy-webpack-plugin');";
    }

    @Override
    public String getContent() {
        // @formatter:off
        return  "    new CopyWebpackPlugin([{\n" +
                "      from: `" + source +"`,\n" +
                "      to: `" + destination + "`\n" +
                "    }])";
        // @formatter:on
    }
}

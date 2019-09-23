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
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.WebBrowser;

/**
 * Browser instance targeted for server side resolving of resources.
 *
 * @since 1.0
 */
public abstract class FakeBrowser extends WebBrowser {

    private static FakeBrowser es6instance = new FakeBrowser() {
        @Override
        public boolean isEs6Supported() {
            return true;
        }
    };

    private static FakeBrowser es5instance = new FakeBrowser() {
        @Override
        public boolean isEs6Supported() {
            return false;
        }
    };


    /**
     * Gets the es6 singleton instance.
     *
     * @return the one and only instance of a ES6 browser
     */
    public static FakeBrowser getEs6() {
        return es6instance;
    }

    /**
     * Gets the es6 singleton instance.
     *
     * @return the one and only instance of a ES5 browser
     */
    public static FakeBrowser getEs5() {
        return es5instance;
    }
}

/*
 * Copyright 2000-2017 Vaadin Ltd.
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
 */
public class FakeEs6Browser extends WebBrowser {

    private static FakeEs6Browser instance = new FakeEs6Browser();

    private FakeEs6Browser() {
        // Singleton, use get()
    }

    @Override
    public boolean isEs6Supported() {
        return true;
    }

    /**
     * Gets the singleton instance.
     *
     * @return the one and only instance
     */
    public static FakeEs6Browser get() {
        return instance;
    }
}

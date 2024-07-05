/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.WebBrowser;

/**
 * Browser instance targeted for server side resolving of resources.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
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

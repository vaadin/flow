/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.router;

import com.vaadin.hummingbird.dom.Element;

/**
 * A navigation handler that shows an error to the user.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ErrorNavigationHandler implements NavigationHandler {

    private final int errorCode;

    /**
     * Creates a new handler that will show the given HTTP status code.
     *
     * @param errorCode
     *            the HTTP status code of the error
     */
    public ErrorNavigationHandler(int errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public void handle(NavigationEvent event) {
        RouterUI ui = event.getUI();

        if (ui == null) {
            throw new IllegalArgumentException(
                    "This method should be updated to support downloads.");
        }

        ui.showView(() -> Element.createText("Error: " + errorCode), null);
    }

}

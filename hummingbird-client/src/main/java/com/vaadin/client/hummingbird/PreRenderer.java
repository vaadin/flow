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
package com.vaadin.client.hummingbird;

import com.vaadin.shared.ApplicationConstants;

import elemental.client.Browser;
import elemental.dom.Element;

/**
 * Handles the transition from the pre-rendered version of the app to the live
 * version.
 *
 * @author Vaadin Ltd
 */
public class PreRenderer {

    /**
     * Transition from a pre-rendered version to the live version.
     */
    public static void transitionToLive() {
        Element body = Browser.getDocument().getBody();

        if (body.hasAttribute(ApplicationConstants.PRE_RENDER_ATTRIBUTE)) {
            body.removeAttribute(ApplicationConstants.PRE_RENDER_ATTRIBUTE);
            // Remove all pre-rendered children
            for (int i = 0; i < body.getChildElementCount(); i++) {
                Element child = (Element) body.getChildren().at(i);
                if (child.hasAttribute(
                        ApplicationConstants.PRE_RENDER_ATTRIBUTE)) {
                    body.removeChild(child);
                    i--;
                }
            }
        }

    }

}

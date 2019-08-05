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
package com.vaadin.flow.testnpmonlyfeatures.general;

import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;

public class TestingUiUnitListener implements UIInitListener {

    @Override
    public void uiInit(UIInitEvent event) {
        event.getUI().add(new ComponentAddedViaInitListenerView());
    }

}

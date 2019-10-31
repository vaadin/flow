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
package com.vaadin.flow.uitest.ui.frontend;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wrapper view for the {@link FrontendProtocolTemplate} component. This class
 * is used by the FrontendProtocolIT to test the "frontend://" protocol in
 * multiple scenarios.
 *
 * @since 1.0
 */
@Route(value = "com.vaadin.flow.uitest.ui.frontend.FrontendProtocolView", layout = ViewTestLayout.class)
@Tag("div")
public class FrontendProtocolView extends Component implements HasComponents {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        FrontendProtocolTemplate component = new FrontendProtocolTemplate();
        add(component);
    }

}

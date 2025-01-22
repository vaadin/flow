/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow;

import org.junit.Test;

import com.vaadin.flow.component.BlurNotifier;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.CompositionNotifier;
import com.vaadin.flow.component.FocusNotifier;
import com.vaadin.flow.component.InputNotifier;
import com.vaadin.flow.component.KeyNotifier;
import com.vaadin.flow.component.Tag;

public class NotifierTest {
    @Tag("div")
    public static class NotifierComponent extends Component
            implements BlurNotifier<NotifierComponent>,
            ClickNotifier<NotifierComponent>, CompositionNotifier,
            FocusNotifier<NotifierComponent>, InputNotifier, KeyNotifier {
    }

    @Test
    public void addNotifiers() {
        // Just testing that adding notifiers actually compiles and doesn't
        // throw. Test is on purpose outside com.vaadin.flow.component to
        // uncover visibility problems.
        NotifierComponent component = new NotifierComponent();
        component.addBlurListener(event -> ignore());
        component.addClickListener(event -> ignore());
        component.addCompositionStartListener(event -> ignore());
        component.addCompositionEndListener(event -> ignore());
        component.addCompositionUpdateListener(event -> ignore());
        component.addInputListener(event -> ignore());
        component.addKeyDownListener(event -> ignore());
        component.addKeyUpListener(event -> ignore());
        component.addKeyPressListener(event -> ignore());
    }

    private static void ignore() {
        // This method is here to avoid line breaks from {} lambas
    }
}

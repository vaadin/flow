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
package com.vaadin.flow.uitest.ui;

import java.util.Random;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;

public abstract class AbstractLiveReloadView extends Div {
    public static final String INSTANCE_IDENTIFIER = "instance-identifier";
    public static final String ATTACH_IDENTIFIER = "attach-identifier";

    private static final Random random = new Random();

    private Span attachIdLabel = new Span();

    public AbstractLiveReloadView() {
        getStyle().set("display", "flex");
        getStyle().setFlexDirection(Style.FlexDirection.COLUMN);
        getStyle().setAlignItems(Style.AlignItems.FLEX_START);

        Span instanceIdLabel = new Span(Integer.toString(random.nextInt()));
        instanceIdLabel.setId(INSTANCE_IDENTIFIER);
        add(instanceIdLabel);

        attachIdLabel.setId(ATTACH_IDENTIFIER);
        add(attachIdLabel);
        addAttachListener(e -> {
            attachIdLabel.setText(Integer.toString(random.nextInt()));
        });
    }

}

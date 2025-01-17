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
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("")
public class RootNavigationTarget extends Div {

    public RootNavigationTarget(@Autowired DataBean dataBean,
            @Autowired FooNavigationTarget section) {
        setId("main");
        NativeLabel label = new NativeLabel(dataBean.getMessage());
        label.setId("message");
        add(label);

        section.addAttachListener(event -> {
            section.setId("singleton-in-ui");
            section.setText("UI singleton");
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        NativeLabel label = new NativeLabel(
                String.valueOf(getUI().get().getUIId()));
        label.setId("ui-id");
        add(label);

        RouterLink link = new RouterLink("foo", FooNavigationTarget.class);
        link.setId("foo");
        add(link);
    }

}

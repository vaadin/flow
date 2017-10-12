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
package com.vaadin.flow.spring;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.router.Route;
import com.vaadin.ui.common.AttachEvent;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.html.Label;

@Route("")
public class RootNavigationTarget extends Div {

    public RootNavigationTarget(@Autowired DataBean dataBean,
            @Autowired FooNavigationTarget section) {
        setId("main");
        Label label = new Label(dataBean.getMessage());
        label.setId("message");
        add(label);

        section.setId("singleton");
        section.setText("singleton");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Label label = new Label(String.valueOf(getUI().get().getUIId()));
        label.setId("ui-id");
        add(label);
    }

}

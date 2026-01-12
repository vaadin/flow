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
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;

public class RootContextLayout extends Div implements RouterLayout {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        PushUtil.setupPush();
    }

    @Route(value = "", layout = RootContextLayout.class)
    public static class RootSubLayout extends DependencyLayout {
        public RootSubLayout() {
            getElement().appendChild(ElementFactory.createDiv("Root Layout")
                    .setAttribute("id", "root"));
        }

    }

    @Route(value = "sub-context", layout = RootContextLayout.class)
    public static class SubContextLayout extends DependencyLayout
            implements HasUrlParameter<String> {

        public SubContextLayout() {
            getElement().appendChild(ElementFactory
                    .createDiv("Sub Context Layout").setAttribute("id", "sub"));
        }

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            // ignored
        }
    }

}

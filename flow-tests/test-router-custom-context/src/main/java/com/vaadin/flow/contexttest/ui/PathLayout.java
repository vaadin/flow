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
package com.vaadin.flow.contexttest.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.WildcardParameter;

/**
 * UI to be a parent for routed layouts (see inner classes). Mapped to some context inside the application.
 *
 * @since 1.2
 */
@RoutePrefix("routed")
public class PathLayout extends Div implements RouterLayout {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        PushUtil.setupPush();
    }

    @Route(value = "", layout = PathLayout.class)
    public static class RootSubContextLayout extends DependencyLayout {

        public RootSubContextLayout() {
            getElement().appendChild(ElementFactory.createDiv("Routed Layout")
                    .setAttribute("id", "routed"));
        }
    }

    @Route(value = "sub-context", layout = PathLayout.class)
    public static class SubContextLayout extends DependencyLayout implements HasUrlParameter<String> {

        public SubContextLayout() {
            getElement().appendChild(ElementFactory.createDiv("Routed Sub Context Layout")
                    .setAttribute("id", "routed-sub"));
        }

        @Override
        public void setParameter(BeforeEvent event, @WildcardParameter String parameter) {
            //Nothing to do
        }
    }
}

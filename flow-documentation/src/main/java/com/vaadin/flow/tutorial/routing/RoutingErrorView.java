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
package com.vaadin.flow.tutorial.routing;

import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.View;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-error-view.asciidoc")
public class RoutingErrorView {
    public class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setErrorView(MyErrorView.class, MainLayout.class);
        }
    }

    public class MainLayout extends Div implements HasChildView {

        @Override
        public void setChildView(View childView) {

        }

    }

    public class MyErrorView extends Div implements View {
        public MyErrorView() {
            setText("404 - View not found");
        }
    }

    public class Foo {
        public class MyErrorView extends Div implements View {
            @Override
            // @formatter:off
            public void onLocationChange(LocationChangeEvent locationChangeEvent) {
                setText("The view ''" + locationChangeEvent.getLocation().getPath() + "' cannot be found");
            }
            // @formatter:on
        }

    }
}

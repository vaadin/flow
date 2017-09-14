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
package com.vaadin.flow.tutorial.routing;

import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.View;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-rerouting.asciidoc")
public class ReroutingView {

    public class MyView extends Div implements View {
        @Override
        public void onLocationChange(LocationChangeEvent locationChangeEvent) {
            // implementation omitted
            Object record = getItem();

            if (record == null) {
                locationChangeEvent.rerouteToErrorView();
                return;
            }
            // implementation omitted
        }

        private Object getItem() {
            // no-op implementation
            return null;
        }
    }

    public class NoItemsView extends Div implements View {
        public NoItemsView() {
            setText("No items found.");
        }
    }

    public class ItemsView extends Div implements View {
        @Override
        public void onLocationChange(LocationChangeEvent locationChangeEvent) {
            // implementation omitted
            Object record = getItem();

            if (record == null) {
                locationChangeEvent.rerouteTo(NoItemsView.class);
            }
        }

        private Object getItem() {
            // no-op implementation
            return null;
        }
    }
}

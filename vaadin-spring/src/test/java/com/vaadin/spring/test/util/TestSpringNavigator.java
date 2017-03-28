/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.test.util;

import com.vaadin.navigator.NavigationStateManager;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewDisplay;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.ui.UI;

// customized navigator to bypass most dependencies
public final class TestSpringNavigator extends SpringNavigator {
    @Override
    public void init(UI ui, ViewDisplay display) {
        init(ui, new NavigationStateManager() {
            private String state;

            @Override
            public void setState(String state) {
                this.state = state;
            }

            @Override
            public void setNavigator(Navigator navigator) {
            }

            @Override
            public String getState() {
                return state;
            }
        }, new ViewDisplay() {
            @Override
            public void showView(View view) {
            }
        });
    }
}
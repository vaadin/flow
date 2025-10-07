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

import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

@Route(value = "com.vaadin.flow.uitest.ui.PreserveOnRefreshNestedBeforeEnterView", layout = PreserveOnRefreshNestedBeforeEnterView.NestedLayout.class)
public class PreserveOnRefreshNestedBeforeEnterView
        extends PreserveOnRefreshNestedBeforeEnterCounter {

    @PreserveOnRefresh
    public static class RootLayout extends
            PreserveOnRefreshNestedBeforeEnterCounter implements RouterLayout {
    }

    @ParentLayout(RootLayout.class)
    public static class NestedLayout extends
            PreserveOnRefreshNestedBeforeEnterCounter implements RouterLayout {
    }
}

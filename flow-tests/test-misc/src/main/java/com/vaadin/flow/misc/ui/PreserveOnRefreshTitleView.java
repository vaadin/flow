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
package com.vaadin.flow.misc.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

@Route("preserve-on-refresh-title-view")
@PreserveOnRefresh
@PageTitle("Initial Title")
public class PreserveOnRefreshTitleView extends Div {

    public PreserveOnRefreshTitleView() {
        NativeButton updateTitle = new NativeButton("Update Title", e -> {
            getUI().ifPresent(ui -> ui.getPage().setTitle("Updated Title"));
        });
        updateTitle.setId("update-title");
        add(updateTitle);
    }
}

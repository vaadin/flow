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
package com.vaadin.flow.router.legacy;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.Tag;

/**
 * The default error view shown to the user. Corresponds to the 404 page.
 * <p>
 * The error view can changed using
 * {@link RouterConfiguration#setErrorView(Class)}.
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
@Tag("div")
public final class DefaultErrorView extends Component implements HasText, View {

    /**
     * Creates a default error view that displays a text message to the user.
     */
    public DefaultErrorView() {
        setText("404 - View not found");
    }

    @Override
    public void onLocationChange(LocationChangeEvent locationChangeEvent) {
        locationChangeEvent.setStatusCode(HttpServletResponse.SC_NOT_FOUND);
    }
}

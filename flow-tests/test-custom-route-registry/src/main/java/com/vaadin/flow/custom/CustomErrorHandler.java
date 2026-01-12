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
package com.vaadin.flow.custom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

/**
 * Customized error handler for IllegalAccessException.
 * <p>
 * This should not be available through the CustomRouteRegistry.
 */
@Tag(Tag.DIV)
public class CustomErrorHandler extends Component
        implements HasErrorParameter<IllegalAccessException> {

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<IllegalAccessException> parameter) {
        getElement().appendChild(
                new Span("This shouldn't be available").getElement());
        return 0;
    }
}

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
package com.vaadin.router;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * This is a basic default error view shown on routing exceptions.
 */
@Tag(Tag.DIV)
public class RouteNotFoundError extends Component
        implements HasErrorParameter<NotFoundException> {

    @Override
    public int setErrorParameter(BeforeNavigationEvent event,
            ErrorParameter<NotFoundException> parameter) {
        getElement().setText(
                "Could not navigate to '" + event.getLocation().getPath() + "'");
        return HttpServletResponse.SC_NOT_FOUND;
    }
}

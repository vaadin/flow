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
package com.vaadin.flow.uitest.ui.routing;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouteNotFoundError;

@ParentLayout(PushLayout.class)
public class PushRouteNotFoundView extends RouteNotFoundError {

    public static String PUSH_NON_EXISTENT_PATH = "push-no-route";

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<NotFoundException> parameter) {
        String path = event.getLocation().getPath();
        if (PUSH_NON_EXISTENT_PATH.equals(path)) {
            Element div = ElementFactory.createDiv("Push mode: "
                    + event.getUI().getPushConfiguration().getPushMode());
            div.setAttribute("id", "push-mode");
            getElement().appendChild(div);
            return HttpServletResponse.SC_NOT_FOUND;
        } else {
            return super.setErrorParameter(event, parameter);
        }
    }
}

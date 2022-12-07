/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.spring.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;

import com.vaadin.flow.component.UI;

/**
 * A strategy to handle redirects which is aware of UIDL requests.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRedirectStrategy extends DefaultRedirectStrategy {

    private final RequestUtil requestUtil;

    public UidlRedirectStrategy(RequestUtil requestUtil) {
        this.requestUtil = requestUtil;
    }

    @Override
    public void sendRedirect(HttpServletRequest request,
            HttpServletResponse response, String url) throws IOException {
        if (requestUtil.isFrameworkInternalRequest(request)) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.getPage().setLocation(url);
            } else {
                LoggerFactory.getLogger(UidlRedirectStrategy.class).warn(
                        "A redirect to {} was request during a Vaadin request, "
                                + "but it was not possible to get the UI instance to perform the action.",
                        url);
            }
        } else {
            super.sendRedirect(request, response, url);
        }
    }
}

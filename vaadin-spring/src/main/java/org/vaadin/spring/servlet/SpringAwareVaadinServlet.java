/*
 * Copyright 2015 The original authors
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
package org.vaadin.spring.servlet;

import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;

/**
 * Subclass of the standard {@link com.vaadin.server.VaadinServlet Vaadin servlet} that adds a {@link SpringAwareUIProvider} to
 * every new Vaadin session.
 * <p>
 * If you need a custom Vaadin servlet, you can either extend this servlet directly, or extend another subclass of {@link VaadinServlet}
 * and just add the UI provider.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Josh Long (josh@joshlong.com)
 */
public class SpringAwareVaadinServlet extends VaadinServlet {

    private static final long serialVersionUID = 5371983676318947478L;

    @Override
    protected void servletInitialized() throws ServletException {
        getService().addSessionInitListener(new SessionInitListener() {

            private static final long serialVersionUID = -6307820453486668084L;

            @Override
            public void sessionInit(SessionInitEvent sessionInitEvent) throws ServiceException {
                WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
                SpringAwareUIProvider uiProvider = new SpringAwareUIProvider(webApplicationContext);
                sessionInitEvent.getSession().addUIProvider(uiProvider);
            }
        });
    }

}

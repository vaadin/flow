/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.Controller;

import com.vaadin.flow.spring.VaadinServletConfiguration.RootExcludeHandler;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class VaadinServletConfigurationTest {

    private final MockServletContext servletContext = new MockServletContext();
    private final Controller forwardingController = new VaadinServletConfiguration()
            .vaadinForwardingController();

    @Test
    void vaadinServletRegistered_unmatchedRequestForwardedToVaadin()
            throws Exception {
        servletContext.registerNamedDispatcher("springServlet",
                new MockRequestDispatcher("/vaadinServlet"));

        HandlerExecutionChain chain = createRootMapping()
                .getHandler(createRequest());

        assertSame(forwardingController, chain.getHandler());
    }

    @Test
    void noVaadinServletRegistered_unmatchedRequestNotHandled()
            throws Exception {
        assertNull(createRootMapping().getHandler(createRequest()));
    }

    @Test
    void customForwardingController_noVaadinServletRegistered_requestStillHandled()
            throws Exception {
        Controller customController = (request, response) -> null;

        HandlerExecutionChain chain = createRootMapping(customController)
                .getHandler(createRequest());

        assertSame(customController, chain.getHandler());
    }

    private RootExcludeHandler createRootMapping() {
        return createRootMapping(forwardingController);
    }

    private RootExcludeHandler createRootMapping(Controller controller) {
        StaticWebApplicationContext context = new StaticWebApplicationContext();
        context.setServletContext(servletContext);
        context.refresh();
        RootExcludeHandler mapping = new RootExcludeHandler(List.of(),
                controller, null);
        mapping.setApplicationContext(context);
        return mapping;
    }

    private MockHttpServletRequest createRequest() {
        return new MockHttpServletRequest(servletContext, "GET",
                "/api/does-not-exist");
    }
}

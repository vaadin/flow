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

import java.util.Properties;

import org.springframework.web.context.WebApplicationContext;

import com.vaadin.server.DefaultDeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.VaadinServlet;
import com.vaadin.spring.server.SpringVaadinServletService;

// override methods to increase their visibility
public class TestSpringVaadinServletService extends SpringVaadinServletService {
    private WebApplicationContext applicationContext;

    public TestSpringVaadinServletService(VaadinServlet servlet,
            WebApplicationContext applicationContext) throws ServiceException {
        super(servlet,
                new DefaultDeploymentConfiguration(
                        TestSpringVaadinServletService.class, new Properties()),
                "");
        this.applicationContext = applicationContext;
        init();
    }

    @Override
    public WebApplicationContext getWebApplicationContext() {
        return applicationContext;
    }
}
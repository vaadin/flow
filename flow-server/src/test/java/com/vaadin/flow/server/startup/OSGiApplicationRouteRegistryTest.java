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
package com.vaadin.flow.server.startup;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.osgi.OSGiAccess;

@RunWith(EnableOSGiRunner.class)
public class OSGiApplicationRouteRegistryTest
        extends ApplicationRouteRegistryTest {

    @After
    public void cleanUp() {
        if (OSGiAccess.getInstance().getOsgiServletContext() != null) {
            ApplicationRouteRegistry.getInstance(new VaadinServletContext(
                    OSGiAccess.getInstance().getOsgiServletContext())).clean();
        }
    }

    @Override
    @Test
    public void assertApplicationRegistry() {
        Assert.assertEquals(
                ApplicationRouteRegistry.class.getName() + "$OSGiRouteRegistry",
                getTestedRegistry().getClass().getName());
    }
}

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
package com.vaadin.flow.spring;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinSession;

public class DeploymentConfigurationPropertiesTest {

    /**
     * Checks that we don't miss any newly added constant into {@link Constants}
     * class which should be exposed also as a config property for Spring.
     *
     */
    @Test
    public void deploymentConfigurationPropertiesAreKnown()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field field = SpringServlet.class.getDeclaredField("PROPERTY_NAMES");
        field.setAccessible(true);
        List<?> list = new ArrayList<>((List<?>) field.get(null));

        Set<Object> constants = new HashSet<Object>();
        for (Field constant : Constants.class.getDeclaredFields()) {
            Assert.assertTrue(
                    "Field " + constant.getName() + " is not a constant",
                    constant.getName().startsWith("$") || (Modifier
                            .isStatic(constant.getModifiers())
                            && Modifier.isPublic(constant.getModifiers())));
            if (constant.getName().startsWith("$")) {
                // thanks to java code coverage which adds non-existent
                // initially variables everywhere: we should skip this extra
                // field
                continue;
            }
            constants.add(constant.get(null));
        }

        Set<Object> constantsCopy = new HashSet<Object>(constants);

        constantsCopy.removeAll(list);

        // Check that the only parameter which is not in Constants is
        // UI_PARAMETER
        list.removeAll(constants);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(VaadinSession.UI_PARAMETER, list.get(0));

        // Check that we have added all other constants as parameters (except
        // those we know)
        Assert.assertEquals(39, constantsCopy.size());

        Assert.assertTrue(constantsCopy
                .contains(Constants.REQUIRED_ATMOSPHERE_RUNTIME_VERSION));
        Assert.assertTrue(constantsCopy.contains(Constants.VAADIN_PREFIX));
        Assert.assertTrue(constantsCopy.contains(Constants.META_INF));
        Assert.assertTrue(
                constantsCopy.contains(Constants.VAADIN_CONFIGURATION));
        Assert.assertTrue(
                constantsCopy.contains(Constants.COMPILED_WEB_COMPONENTS_PATH));
        Assert.assertTrue(
                constantsCopy.contains(Constants.STATISTICS_JSON_DEFAULT));
        Assert.assertTrue(
                constantsCopy.contains(Constants.VAADIN_SERVLET_RESOURCES));
        Assert.assertTrue(constantsCopy.contains(Constants.VAADIN_MAPPING));
        Assert.assertTrue(constantsCopy.contains(Constants.VAADIN_BUILD));
        Assert.assertTrue(
                constantsCopy.contains(Constants.VAADIN_BUILD_FILES_PATH));
        Assert.assertTrue(
                constantsCopy.contains(Constants.POLYFILLS_DEFAULT_VALUE));
        Assert.assertTrue(
                constantsCopy.contains(Constants.RESOURCES_FRONTEND_DEFAULT));
        Assert.assertTrue(constantsCopy.contains(Constants.PACKAGE_JSON));
        Assert.assertTrue(constantsCopy.contains(
                Constants.SERVLET_PARAMETER_WEB_COMPONENT_DISCONNECT));
        Assert.assertTrue(
                constantsCopy.contains(Constants.VAADIN_VERSIONS_JSON));
    }
}

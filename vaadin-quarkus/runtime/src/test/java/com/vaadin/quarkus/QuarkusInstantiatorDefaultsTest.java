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
package com.vaadin.quarkus;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.DefaultMenuAccessControl;
import com.vaadin.flow.server.auth.MenuAccessControl;
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;
import com.vaadin.quarkus.context.ServiceUnderTestContext;

@QuarkusTest
@TestProfile(QuarkusInstantiatorDefaultsTest.NoBeansTestProfile.class)
class QuarkusInstantiatorDefaultsTest {

    @Inject
    BeanManager beanManager;

    @Inject
    @VaadinServiceEnabled
    QuarkusInstantiatorFactory instantiatorFactory;

    Instantiator instantiator;

    ServiceUnderTestContext serviceUnderTestContext;

    @BeforeEach
    public void setUp() {
        serviceUnderTestContext = new ServiceUnderTestContext(beanManager);
        serviceUnderTestContext.activate();
        instantiator = instantiatorFactory
                .createInstantitor(VaadinService.getCurrent());
    }

    @AfterEach
    public void tearDown() {
        serviceUnderTestContext.tearDownAll();
    }

    @Test
    public void getMenuAccessControl_beanNotProvided_instanceReturned() {
        MenuAccessControl menuAccessControl = instantiator
                .getMenuAccessControl();
        Assertions.assertNotNull(menuAccessControl);
        Assertions.assertTrue(
                menuAccessControl instanceof DefaultMenuAccessControl);
    }

    public static class NoBeansTestProfile implements QuarkusTestProfile {
        @Override
        public String getConfigProfile() {
            return "empty";
        }

        @Override
        public Map<String, String> getConfigOverrides() {
            // Prevents discovering beans defined in QuarkusInstantiatorTest
            return Map.of("quarkus.arc.exclude-types",
                    QuarkusInstantiatorTest.TestMenuAccessControl.class
                            .getName());
        }
    }
}

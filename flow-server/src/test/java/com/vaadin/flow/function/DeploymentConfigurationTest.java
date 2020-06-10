/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.function;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class DeploymentConfigurationTest {

    public abstract static class TestDeploymentConfiguration
            implements DeploymentConfiguration {

    }

    private TestDeploymentConfiguration configuration = Mockito
            .spy(TestDeploymentConfiguration.class);

    @Test
    public void useCompiledFrontendResources_compatibilityMode_productionMode_returnTrue() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(configuration.isCompatibilityMode()).thenReturn(true);
        Assert.assertTrue(configuration.useCompiledFrontendResources());
    }

    @Test
    public void useCompiledFrontendResources_notCompatibilityMode_productionMode_returnFalse() {
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(configuration.isCompatibilityMode()).thenReturn(false);
        Assert.assertFalse(configuration.useCompiledFrontendResources());
    }
}

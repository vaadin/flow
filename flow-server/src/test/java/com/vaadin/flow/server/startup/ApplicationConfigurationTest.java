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
package com.vaadin.flow.server.startup;

import java.util.Enumeration;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinContext;

public class ApplicationConfigurationTest {

    @Test(expected = IllegalStateException.class)
    public void get_contextHasNoLookup_iseIsThrown() {
        VaadinContext context = Mockito.spy(VaadinContext.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(null);
        Mockito.doAnswer(
                invocation -> invocation.getArgument(1, Supplier.class).get())
                .when(context).getAttribute(Mockito.any(), Mockito.any());
        ApplicationConfiguration.get(context);
    }

    @Test
    public void enableDevServerParameter_expressBuildFeatureFlagIsON_resetsEnableDevServerToFalse() {
        FeatureFlags featureFlags = Mockito.mock(FeatureFlags.class);
        Mockito.when(featureFlags.isEnabled(FeatureFlags.EXPRESS_BUILD))
                .thenReturn(true);
        VaadinContext vaadinContext = Mockito.mock(VaadinContext.class);

        class EmptyEnumeration implements Enumeration<String> {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public String nextElement() {
                return null;
            }
        }

        Mockito.when(vaadinContext.getContextParameterNames())
                .thenReturn(new EmptyEnumeration());

        try (MockedStatic<FeatureFlags> featureFlagsStatic = Mockito
                .mockStatic(FeatureFlags.class)) {
            featureFlagsStatic.when(() -> FeatureFlags.get(vaadinContext))
                    .thenReturn(featureFlags);
            ApplicationConfiguration applicationConfiguration = new DefaultApplicationConfigurationFactory() {
                @Override
                protected String getTokenFileFromClassloader(
                        VaadinContext context) {
                    return null;
                }
            }.create(vaadinContext);
            Assert.assertFalse(
                    "Expected dev server to be disabled when the "
                            + "Express Build feature flag is ON",
                    applicationConfiguration.enableDevServer());
        }
    }

}

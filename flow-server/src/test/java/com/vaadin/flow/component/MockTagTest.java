/*
 *
 *
 *  * Copyright 2000-2024 Vaadin Ltd.
 *
 *  *
 *
 *  * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *
 *  * use this file except in compliance with the License. You may obtain a copy of
 *
 *  * the License at
 *
 *  *
 *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  *
 *
 *  * Unless required by applicable law or agreed to in writing, software
 *
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *
 *  * License for the specific language governing permissions and limitations under
 *
 *  * the License.
 *
 *
 */

package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Set;

import com.vaadin.flow.component.internal.ComponentMetaData;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.SessionExpiredException;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinSession;

public class MockTagTest {
    public static class MockComponent extends Component {
        @Tag("mock-tag")
        public static class Sample extends Component {}
    }

    public static class AnotherMockComponent extends Component {
        @Tag("another-tag")
        public static class Sample extends Component {}
    }

    private ComponentMetaData metaDataSample;
    private ComponentMetaData metaDataAnotherSample;
    private VaadinService mockedVaadinService;
    private DeploymentConfiguration mockedDeploymentConfiguration;

    @Before
    public void setup() throws SessionExpiredException {
        // Create and configure mocks for VaadinService and DeploymentConfiguration
        mockedVaadinService = Mockito.mock(VaadinService.class);
        mockedDeploymentConfiguration = Mockito.mock(DeploymentConfiguration.class);

        Mockito.when(mockedVaadinService.getDeploymentConfiguration()).thenReturn(mockedDeploymentConfiguration);
        Mockito.when(mockedDeploymentConfiguration.isProductionMode()).thenReturn(false);

        VaadinService.setCurrent(mockedVaadinService);

        VaadinSession mockedSession = Mockito.mock(VaadinSession.class);
        Mockito.when(mockedSession.getService()).thenReturn(mockedVaadinService);

        // Correct usage of mockStatic in a try-with-resources statement
        try (MockedStatic<VaadinSession> mocked = Mockito.mockStatic(VaadinSession.class)) {
            mocked.when(VaadinSession::getCurrent).thenReturn(mockedSession);
        }

        // Initialize ComponentMetaData instances for testing
        metaDataSample = new ComponentMetaData(MockComponent.Sample.class);
        metaDataAnotherSample = new ComponentMetaData(AnotherMockComponent.Sample.class);
    }

    @Test
    public void getComponentsByTag_correctlyMapsTags() throws SessionExpiredException {
        Set<Class<? extends Component>> components = metaDataSample.getComponentsByTag("mock-tag");
        Assert.assertTrue("The set should contain the MockComponent.Sample class",
                components.contains(MockComponent.Sample.class));

        Set<Class<? extends Component>> anotherComponents = metaDataAnotherSample.getComponentsByTag("another-tag");
        Assert.assertTrue("The set should contain the AnotherMockComponent.Sample class",
                anotherComponents.contains(AnotherMockComponent.Sample.class));
    }

    @Test
    public void getComponentsByTag_returnsEmptyForUnknownTag() throws SessionExpiredException {
        Set<Class<? extends Component>> components = metaDataSample.getComponentsByTag("non-existent-tag");
        Assert.assertTrue("The set should be empty for a non-existent tag", components.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getComponentsByTag_throwsExceptionInProductionMode() throws SessionExpiredException {
        Mockito.when(mockedDeploymentConfiguration.isProductionMode()).thenReturn(true);
        metaDataSample.getComponentsByTag("mock-tag");
    }
}
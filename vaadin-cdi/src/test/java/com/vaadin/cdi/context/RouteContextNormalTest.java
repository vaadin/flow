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
package com.vaadin.cdi.context;

import java.io.Serializable;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.cdi.annotation.NormalRouteScoped;
import com.vaadin.cdi.context.RouteScopedContext.NavigationData;
import com.vaadin.cdi.util.BeanProvider;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.cdi.SerializationUtils.serializeAndDeserialize;

public class RouteContextNormalTest extends
        AbstractContextTest<RouteContextNormalTest.RouteScopedTestBean> {

    @NormalRouteScoped
    @Route("")
    public static class RouteScopedTestBean extends TestBean
            implements Serializable {
    }

    @Override
    protected Class<RouteScopedTestBean> getBeanType() {
        return RouteScopedTestBean.class;
    }

    @Override
    protected UnderTestContext newContextUnderTest() {
        // Intentionally UI Under Test Context. Nothing else needed.
        UIUnderTestContext context = new UIUnderTestContext() {

            @Override
            public void activate() {
                super.activate();

                NavigationData data = new NavigationData(
                        TestNavigationTarget.class, Collections.emptyList());
                ComponentUtil.setData(getUi(), NavigationData.class, data);
            }
        };

        return context;
    }

    @Override
    protected boolean isNormalScoped() {
        return true;
    }

    @Test
    void activeContext_UISerializable() throws Exception {
        UIUnderTestContext context = (UIUnderTestContext) createContext();
        context.activate();
        BeanProvider.getContextualReference(getBeanType());
        UI ui = context.getUi();
        try (MockedStatic<ApplicationConfiguration> appCfg = Mockito
                .mockStatic(ApplicationConfiguration.class)) {
            appCfg.when(
                    () -> ApplicationConfiguration.get(ArgumentMatchers.any()))
                    .thenReturn(Mockito.mock(ApplicationConfiguration.class));
            UI ui2 = serializeAndDeserialize(ui);
            Assertions.assertNotNull(ui2);
        }
    }

}

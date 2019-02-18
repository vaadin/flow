/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.component;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ui.LoadMode;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

public class HtmlImportJsModuleTest {

    @HtmlImport(value = "lazy.js", loadMode = LoadMode.LAZY)
    @HtmlImport(value = "test/lazy.js", loadMode = LoadMode.LAZY)
    private class HtmlImportComponent extends Component {

    }

    @Test
    public void test() {
        VaadinSession session = Mockito.mock(VaadinSession.class);
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito.mock(DeploymentConfiguration.class);

        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(service.getDeploymentConfiguration()).thenReturn(configuration);
        Mockito.when(configuration.isBowerMode()).thenReturn(false);

        UI ui = Mockito.mock(UI.class);
        Element element = Mockito.mock(Element.class);
        StateNode node = Mockito.mock(StateNode.class);
        Page page = Mockito.mock(Page.class);

        Mockito.when(ui.getElement()).thenReturn(element);
        Mockito.when(ui.getPage()).thenReturn(page);
        Mockito.when(ui.getUI()).thenReturn(java.util.Optional.of(ui));
        Mockito.when(element.getNode()).thenReturn(node);
        Mockito.when(element.isEnabled()).thenReturn(true);
        Mockito.when(node.isEnabledSelf()).thenReturn(true);

        UIInternals uiInternals = new UIInternals(ui);
        uiInternals.setSession(session);

        uiInternals.addComponentDependencies(HtmlImportComponent.class);

        ArgumentCaptor<String> valueArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LoadMode> loadModeArgumentCaptor = ArgumentCaptor.forClass(LoadMode.class);

        Mockito.verify(page).addJsModule(valueArgumentCaptor.capture(), loadModeArgumentCaptor.capture());


        System.out.println(valueArgumentCaptor.getValue());
        System.out.println(loadModeArgumentCaptor.getValue());
    }

}

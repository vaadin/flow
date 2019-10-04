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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.internal.DependencyTreeCache;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ui.LoadMode;

public class HtmlImportJsModuleTest {

    private VaadinSession session;
    private DeploymentConfiguration configuration;
    private UI ui;
    private Page page;

    @Before
    public void setupMocks() {
        session = Mockito.mock(VaadinSession.class);
        configuration = Mockito.mock(DeploymentConfiguration.class);

        VaadinService service = Mockito.mock(VaadinService.class);
        VaadinContext context = Mockito.mock(VaadinContext.class);

        Mockito.when(service.getContext()).thenReturn(context);

        Mockito.when(session.getService()).thenReturn(service);
        Mockito.when(session.getConfiguration()).thenReturn(configuration);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        ui = Mockito.mock(UI.class);
        page = Mockito.mock(Page.class);

        Mockito.when(ui.getSession()).thenReturn(session);
        Mockito.when(service.getContext())
                .thenReturn(Mockito.mock(VaadinContext.class));

        Element element = Mockito.mock(Element.class);
        StateNode node = Mockito.mock(StateNode.class);

        Mockito.when(ui.getElement()).thenReturn(element);
        Mockito.when(ui.getPage()).thenReturn(page);
        Mockito.when(ui.getUI()).thenReturn(java.util.Optional.of(ui));
        Mockito.when(element.getNode()).thenReturn(node);
        Mockito.when(element.isEnabled()).thenReturn(true);
        Mockito.when(node.isEnabledSelf()).thenReturn(true);

        // Used only for HtmlImport (isBowerMode = true).
        DependencyTreeCache<String> dependencyCache = Mockito
                .mock(DependencyTreeCache.class);

        Mockito.when(dependencyCache.getDependencies(Mockito.anyString()))
                .thenAnswer(new Answer<Object>() {
                    @Override
                    public Object answer(InvocationOnMock invocation) {
                        Set set = new HashSet<>(1);
                        set.add(invocation.getArguments()[0]);
                        return set;
                    }
                });

        Mockito.when(service.getHtmlImportDependencyCache())
                .thenReturn(dependencyCache);
    }

    @Test
    public void htmlImport_useBower_false() {
        assertComponentImport(HtmlImportComponent.class,
                "/node_modules/@vaadin/vaadin-ordered-layout/src/vaadin-vertical-layout.js",
                LoadMode.LAZY, false);

    }

    @Test
    public void htmlImport_useBower_true() {
        assertComponentImport(HtmlImportComponent.class,
                "frontend://bower_components/vaadin-ordered-layout/src/vaadin-vertical-layout.html",
                LoadMode.LAZY, true);

    }

    @Test
    public void htmlImport_jsModule_useBower_false() {
        assertComponentImport(HtmlImportJsModuleComponent.class,
                "/node_modules/@vaadin/vaadin-ordered-layout/src/vaadin-vertical-layout.js",
                LoadMode.INLINE, false);

    }

    @Test
    public void htmlImport_jsModule_useBower_true() {
        assertComponentImport(HtmlImportJsModuleComponent.class,
                "frontend://bower_components/vaadin-ordered-layout/src/vaadin-vertical-layout.html",
                LoadMode.EAGER, true);

    }

    private void assertComponentImport(Class componentClass, String importValue,
            LoadMode importLoadMode, boolean isBowerMode) {

        Mockito.when(configuration.isCompatibilityMode())
                .thenReturn(isBowerMode);

        UIInternals uiInternals = new UIInternals(ui);
        uiInternals.setSession(session);

        ArgumentCaptor<String> valueArgumentCaptor = ArgumentCaptor
                .forClass(String.class);
        ArgumentCaptor<LoadMode> loadModeArgumentCaptor = ArgumentCaptor
                .forClass(LoadMode.class);

        uiInternals.addComponentDependencies(componentClass);

        if (isBowerMode) {
            Mockito.verify(page).addHtmlImport(valueArgumentCaptor.capture(),
                    loadModeArgumentCaptor.capture());
            Assert.assertEquals("Incorrect import value.", importValue,
                    valueArgumentCaptor.getValue());
            Assert.assertEquals("Incorrect import load mode.", importLoadMode,
                    loadModeArgumentCaptor.getValue());
        } else {
            Mockito.verify(page, Mockito.never())
                    .addHtmlImport(Mockito.anyString());
        }
    }

    @HtmlImport(value = "frontend://bower_components/vaadin-ordered-layout/src/vaadin-vertical-layout.html", loadMode = LoadMode.LAZY)
    private class HtmlImportComponent extends Component {

    }

    @HtmlImport(value = "frontend://bower_components/vaadin-ordered-layout/src/vaadin-vertical-layout.html", loadMode = LoadMode.EAGER)
    @JsModule(value = "/node_modules/@vaadin/vaadin-ordered-layout/src/vaadin-vertical-layout.js", loadMode = LoadMode.INLINE)
    private class HtmlImportJsModuleComponent extends Component {

    }

}

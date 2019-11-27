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
 */

package com.vaadin.flow.internal.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.RepeatIndex;
import com.vaadin.flow.component.polymertemplate.TemplateParser.TemplateData;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ConstantPoolKey;
import com.vaadin.flow.internal.HasCurrentService;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonArray;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author Vaadin Ltd
 * @since 1.0.
 */
@NotThreadSafe
public class PolymerServerEventHandlersTest extends HasCurrentService {
    private StateNode stateNode;
    private PolymerServerEventHandlers handlers;
    private Collection<Method> methodCollector;
    private Map<String, Method> correctlyAnnotatedHandlers;
    private Map<String, Method> wronglyAnnotatedHandlers;

    @Tag("polymer")
    private static class CorrectAnnotationUsage
            extends PolymerTemplate<TemplateModel> {

        CorrectAnnotationUsage() {
            super((clazz, tag, service) -> new TemplateData("",
                    Jsoup.parse("<dom-module id='polymer'></dom-module>")));
        }

        @EventHandler
        public void noParams() {
        }

        @EventHandler
        public void eventDataParam(@EventData("test") String test) {
        }

        @EventHandler
        public void repeatIndexParam1(@RepeatIndex int index) {
        }

        @EventHandler
        public void repeatIndexParam2(@RepeatIndex Integer index) {
        }

        @EventHandler
        public void eventDataAndRepeatIndexOnDifferentParams(
                @EventData("test") String test, @RepeatIndex int index) {
        }

    }

    private static class WrongAnnotationUsage {
        @EventHandler
        public void notAnnotatedParam(int iCauseExceptions) {
        }

        @EventHandler
        public void wrongTypeOfRepeatIndexParam(
                @RepeatIndex String iCauseThemToo) {
        }

        @EventHandler
        public void eventDataAndRepeatIndexOnOneParam(
                @EventData("test") @RepeatIndex int meToo) {
        }
    }

    private static Map<String, Method> getEventHandlerNamesAndMethods(
            Class<?> clazz) {
        return Stream.of(clazz.getMethods())
                .filter(method -> Objects
                        .nonNull(method.getAnnotation(EventHandler.class)))
                .collect(
                        Collectors.toMap(Method::getName, Function.identity()));
    }

    @Before
    public void setUp() {
        Collection<Class<? extends NodeFeature>> features = BasicElementStateProvider
                .getFeatures();
        stateNode = new StateNode(features.toArray(new Class[features.size()]));
        stateNode.getFeature(ElementData.class).setTag("test");
        handlers = new PolymerServerEventHandlers(stateNode);
        methodCollector = new ArrayList<>();
        correctlyAnnotatedHandlers = getEventHandlerNamesAndMethods(
                CorrectAnnotationUsage.class);
        wronglyAnnotatedHandlers = getEventHandlerNamesAndMethods(
                WrongAnnotationUsage.class);

    }

    @Override
    protected VaadinService createService() {
        VaadinService service = Mockito.mock(VaadinService.class);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(configuration.isProductionMode()).thenReturn(true);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        return service;
    }

    private void addAndVerifyMethod(Method method) {
        handlers.addHandlerMethod(method, methodCollector);

        assertEquals(1, methodCollector.size());
        assertEquals(method, methodCollector.iterator().next());
        assertEquals(method.getParameters().length,
                extractParametersData(method).length());
    }

    private JreJsonArray extractParametersData(Method method) {
        ConstantPoolKey parametersData = (ConstantPoolKey) stateNode
                .getFeature(PolymerEventListenerMap.class)
                .get(method.getName());
        assertNotNull(parametersData);

        JsonObject json = Json.createObject();
        parametersData.export(json);
        return json.get(parametersData.getId());
    }

    @Test
    public void testNoParamsMethod() {
        addAndVerifyMethod(correctlyAnnotatedHandlers.get("noParams"));
    }

    @Test
    public void testCorrectMethodWithDifferentAnnotations()
            throws NoSuchFieldException, IllegalAccessException {
        // Insert component without using ComponentMapping::setComponent as we
        // only want to map and test the method
        // `eventDataAndRepeatIndexOnDifferentParams`
        Field component = ComponentMapping.class.getDeclaredField("component");
        component.setAccessible(true);
        component.set(stateNode.getFeature(ComponentMapping.class),
                new CorrectAnnotationUsage());
        addAndVerifyMethod(correctlyAnnotatedHandlers
                .get("eventDataAndRepeatIndexOnDifferentParams"));
    }

    @Test
    public void testEventDataParam()
            throws NoSuchFieldException, IllegalAccessException {
        // Insert component without using ComponentMapping::setComponent as we
        // only want to map and test the method `eventDataParam`
        Field component = ComponentMapping.class.getDeclaredField("component");
        component.setAccessible(true);
        component.set(stateNode.getFeature(ComponentMapping.class),
                new CorrectAnnotationUsage());
        addAndVerifyMethod(correctlyAnnotatedHandlers.get("eventDataParam"));
    }

    @Test
    public void testRepeatIndexParam1() {
        addAndVerifyMethod(correctlyAnnotatedHandlers.get("repeatIndexParam1"));
    }

    @Test
    public void testRepeatIndexParam2() {
        addAndVerifyMethod(correctlyAnnotatedHandlers.get("repeatIndexParam2"));
    }

    @Test(expected = IllegalStateException.class)
    public void testNotAnnotatedParam() {
        addAndVerifyMethod(wronglyAnnotatedHandlers.get("notAnnotatedParam"));
    }

    @Test(expected = IllegalStateException.class)
    public void testWrongTypeOfRepeatIndexParam() {
        addAndVerifyMethod(
                wronglyAnnotatedHandlers.get("wrongTypeOfRepeatIndexParam"));
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleAnnotationsOnOneParam() {
        addAndVerifyMethod(wronglyAnnotatedHandlers
                .get("eventDataAndRepeatIndexOnOneParam"));
    }
}

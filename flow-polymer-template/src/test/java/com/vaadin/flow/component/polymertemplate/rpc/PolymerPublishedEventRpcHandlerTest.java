/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.polymertemplate.rpc;

import java.lang.reflect.Type;
import java.util.Optional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.component.polymertemplate.TemplateParser;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.ModelType;
import com.vaadin.flow.templatemodel.TemplateModel;

public class PolymerPublishedEventRpcHandlerTest {

    private VaadinService service;

    private PolymerPublishedEventRpcHandler handler;

    @Before
    public void setup() {
        service = Mockito.mock(VaadinService.class);
        VaadinService.setCurrent(service);
        DeploymentConfiguration configuration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);

        handler = new PolymerPublishedEventRpcHandler();
    }

    @After
    public void tearDown() {
        VaadinService.setCurrent(null);
    }

    @Tag("likeable-element")
    @JsModule("./frontend/likeable-element-view.js")
    public class TestModule extends PolymerTemplate<ModelClass> {

        public TestModule() {
            super((clazz, tag, service) -> new TemplateParser.TemplateData("",
                    Jsoup.parse("<dom-module id='div'></dom-module>")));
        }
    }

    public interface ModelClass extends TemplateModel {
        void setMessage(String message);
    }

    @Tag("my-component")
    public class TestComponent extends Component {
    }

    @Test
    public void templateWithModel_payloadAsExpected_returnsTrue() {
        TestModule instance = new TestModule();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("nodeId", 0);
        json.put("message", "bar");

        boolean isModelValue = handler.isTemplateModelValue(instance, json,
                String.class);

        Assert.assertTrue(isModelValue);
    }

    @Test
    public void templateWithModel_payloadMissingNodeId_returnsFalse() {
        TestModule instance = new TestModule();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("message", "bar");

        boolean isModelValue = handler.isTemplateModelValue(instance, json,
                String.class);

        Assert.assertFalse(isModelValue);
    }

    @Test
    public void templateWithModel_unsupportedType_returnsFalse() {
        TestModule instance = new TestModule();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("message", "bar");

        boolean isModelValue = handler.isTemplateModelValue(instance, json,
                Boolean.class);

        Assert.assertFalse(isModelValue);
    }

    @Test
    public void templateWithModel_faultyPayloadAsNoJsonObject_returnsFalse() {
        TestModule instance = new TestModule();

        boolean isModelValue = handler.isTemplateModelValue(instance,
                JacksonUtils.createArrayNode(), String.class);

        Assert.assertFalse(isModelValue);
    }

    @Test
    public void normalComponent_returnsFalse() {
        TestComponent instance = new TestComponent();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("nodeId", 0);
        json.put("message", "bar");

        boolean isModelValue = handler.isTemplateModelValue(instance, json,
                String.class);

        Assert.assertFalse(isModelValue);
    }

    public static Object getTemplateItem(Component template, JsonNode argValue,
            Type convertedType) {
        final Optional<UI> ui = template.getUI();
        if (ui.isPresent()) {
            StateNode node = ui.get().getInternals().getStateTree()
                    .getNodeById(argValue.get("nodeId").intValue());

            ModelType propertyType = ((PolymerTemplate<?>) template)
                    .getModelType(convertedType);

            return propertyType.modelToApplication(node);
        }
        throw new IllegalStateException(
                "Event sent for a non attached template component");
    }

    @Test(expected = IllegalArgumentException.class)
    public void templateNotConnectedToUI_throws() throws NoSuchMethodException {
        TestModule instance = new TestModule();

        ObjectNode json = JacksonUtils.createObjectNode();
        json.put("nodeId", 0);
        json.put("message", "bar");
        final Type messageType = ModelClass.class
                .getMethod("setMessage", String.class)
                .getGenericParameterTypes()[0];

        handler.getTemplateItem(instance, json, messageType);
    }
}

package com.vaadin.flow.component.polymertemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;

public class BundleParserTest {

    private static final String statsFile = VAADIN_SERVLET_RESOURCES
            + "config/stats.json";

    private static JsonObject stats;

    private MockVaadinServletService service;
    private DeploymentConfiguration configuration;

    @BeforeClass
    public static void initClass() throws IOException {
        InputStream stream = BundleParserTest.class.getClassLoader()
                .getResourceAsStream(statsFile);
        String statsFileContents = IOUtils.toString(stream,
                StandardCharsets.UTF_8);
        stats = BundleParser.parseJsonStatistics(statsFileContents);
    }

    @Before
    public void init() {
        configuration = Mockito.mock(DeploymentConfiguration.class);
        Mockito.when(configuration.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.when(configuration.getFlowResourcesFolder()).thenReturn(
                Paths.get("target", FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER)
                        .toString());

        Properties properties = new Properties();
        Mockito.when(configuration.getInitParameters()).thenReturn(properties);

        Instantiator instantiator = Mockito.mock(Instantiator.class);
        Mockito.when(instantiator.getServiceInitListeners())
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getDependencyFilters(Mockito.any()))
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getBootstrapListeners(Mockito.any()))
                .thenReturn(Stream.empty());
        Mockito.when(instantiator.getIndexHtmlRequestListeners(Mockito.any()))
                .thenReturn(Stream.empty());
        service = new MockVaadinServletService(configuration);
        service.init(instantiator);
    }

    @Test
    public void nonLocalTemplate_sourcesShouldBeFound() {
        final String source = BundleParser.getSourceFromStatistics(
                "./src/hello-world.js", stats, service);
        Assert.assertNotNull("Source expected in stats.json", source);
    }

    @Test
    public void nonLocalTemplate_sourcesShouldBeFoundInTargetFolder() {
        final String source = BundleParser.getSourceFromStatistics(
                "./src/hello-world2.js", stats, service);
        Assert.assertNotNull("Source expected in stats.json", source);
    }

    @Test
    public void nonLocalTemplate_windowsPath_sourcesShouldBeFoundInTargetFolder() {
        Mockito.when(configuration.getFlowResourcesFolder()).thenReturn(
                "target\\" + FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER);
        final String source = BundleParser.getSourceFromStatistics(
                "./src/hello-world2.js", stats, service);
        Assert.assertNotNull("Source expected in stats.json", source);
    }

    @Test
    public void frontendPrefix_sourcesShouldBeFound() {
        final String source = BundleParser.getSourceFromStatistics(
                "./frontend/src/hello-world.js", stats, service);
        Assert.assertNotNull("Source expected in stats.json", source);
    }

    @Test
    public void typeScriptExtension_sourcesShouldBeFound() {
        final String source = BundleParser.getSourceFromStatistics(
                "./frontend/my-form.ts", stats, service);
        Assert.assertNotNull("TypeScript sources expected in stats.json",
                source);
    }

    @Test
    public void frontendProtocol_sourcesShouldBeFound() {
        final String source = BundleParser.getSourceFromStatistics(
                "frontend:///src/hello-world.js", stats, service);
        Assert.assertNotNull("Source expected in stats.json", source);
    }

    @Test
    public void startsWithSingleLetterDirector_sourcesShouldNotBeFound() {
        // This test exposes a common error in String#replaceFirst (unescaped
        // period in regex) in BundleParser#getSourceFromObject
        final JsonObject module = Json.createObject();
        module.put("name", "a/src/hello-world.js");
        module.put("source", "some-source.js");
        final String source = BundleParser.getSourceFromStatistics(
                "a/frontend/src/hello-world.js", module, service);
        Assert.assertNull("Source not expected in module", source);
    }

    @Test
    public void parseTemplateElement_stringContentNotSeenAsComment() {
        String source = "static get template() { return html`<vaadin-text-field label=\"Nats Url(s)\" placeholder=\"nats://server:port\" id=\"natsUrlTxt\" style=\"width:100%\"></vaadin-text-field>`;}";
        Element element = BundleParser.parseTemplateElement("nats.js", source);

        Element natsElement = element.getElementById("natsUrlTxt");
        Assert.assertNotNull("Found element by Id", natsElement);
        Assert.assertEquals("Invalid tag for element", "vaadin-text-field",
                natsElement.tagName());

        Assert.assertEquals(
                "Parsed value for attribute 'placeholder' was wrong.",
                "nats://server:port", natsElement.attr("placeholder"));

    }

    @Test
    public void parseTemplateElement_spacesBetweenHtmlAndTick() {
        String source = "static get template() { return html    `<div id='bar'></div>`;}";
        Element element = BundleParser.parseTemplateElement("foo.js", source);

        Assert.assertNotNull(element.getElementById("bar"));
    }
}

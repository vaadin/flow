/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.fusion.utils.TestUtils;

public class ClientAPIGeneratorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void relativizeEndpointPrefixWithUrlMapping_Should_workWithBothDefaultValues() {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());
        String result = generator.relativizeEndpointPrefixWithUrlMapping(
                ClientAPIGenerator.DEFAULT_PREFIX, "/*");
        Assert.assertEquals("connect", result);
    }

    @Test
    public void relativizeEndpointPrefixWithUrlMapping_should_workWithCustomUrlMappingAndCustomEndpointPrefix() {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());
        String result = generator.relativizeEndpointPrefixWithUrlMapping(
                "/my-connect", "/myapp/*");
        Assert.assertEquals("../my-connect", result);
    }

    @Test
    public void relativizeEndpointPrefixWithUrlMapping_should_workWithCustomUrlMappingAndDefaultEndpointPrefix() {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());
        String result = generator.relativizeEndpointPrefixWithUrlMapping(
                ClientAPIGenerator.DEFAULT_PREFIX, "/myapp/*");
        Assert.assertEquals("../connect", result);
    }

    @Test
    public void relativizeEndpointPrefixWithUrlMapping_should_workWithDefaultUrlMappingAndCustomEndpointPrefix() {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());
        String result = generator
                .relativizeEndpointPrefixWithUrlMapping("/my-connect", "/*");
        Assert.assertEquals("my-connect", result);
    }

    @Test
    public void relativizeEndpointPrefixWithUrlMapping_Should_WorkWithMultipleLevelUrlMappingAndCustomEndpointPrefix() {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());
        String result = generator.relativizeEndpointPrefixWithUrlMapping(
                "/my-connect", "/myapp/yourapp/*");
        Assert.assertEquals("../../my-connect", result);
    }

    @Test
    public void relativizeEndpointPrefixWithUrlMapping_Should_WorkWithMultipleLevelUrlMappingAndMultipleLevelEndpointPrefix() {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());
        String result = generator.relativizeEndpointPrefixWithUrlMapping(
                "/my-connect/your-connect", "/myapp/yourapp/*");
        Assert.assertEquals("../../my-connect/your-connect", result);
    }

    @Test
    public void should_GenerateConnectClientDefault_When_ApplicationPropertiesInput()
            throws Exception {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(),
                TestUtils.readProperties(getClass()
                        .getResource("application.properties.for.testing")
                        .getPath()));

        generator.generate();

        Path outputPath = generator.getOutputFilePath();

        Assert.assertTrue(outputPath.toFile().exists());
        String actualJson = StringUtils.toEncodedString(
                Files.readAllBytes(outputPath), StandardCharsets.UTF_8).trim();
        String expectedJson = TestUtils.readResource(
                getClass().getResource("expected-connect-client-custom.ts"));
        Assert.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void should_GenerateConnectClientDefault_When_NoApplicationPropertiesInput()
            throws Exception {
        ClientAPIGenerator generator = new ClientAPIGenerator(
                temporaryFolder.getRoot().toPath(), new Properties());

        generator.generate();

        Path outputPath = generator.getOutputFilePath();

        Assert.assertTrue(outputPath.toFile().exists());
        String actualJson = StringUtils.toEncodedString(
                Files.readAllBytes(outputPath), StandardCharsets.UTF_8).trim();
        String expectedJson = TestUtils.readResource(
                getClass().getResource("expected-connect-client-default.ts"));
        Assert.assertEquals(expectedJson, actualJson);
    }
}

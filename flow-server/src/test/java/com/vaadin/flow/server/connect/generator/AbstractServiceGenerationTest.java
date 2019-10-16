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

package com.vaadin.flow.server.connect.generator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.connect.VaadinService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public abstract class AbstractServiceGenerationTest {
    private static final List<Class<?>> JSON_NUMBER_CLASSES = Arrays.asList(
            Number.class, byte.class, char.class, short.class, int.class,
            long.class, float.class, double.class);

    @Rule
    public TemporaryFolder outputDirectory = new TemporaryFolder();

    protected Path openApiJsonOutput;

    private final List<Class<?>> serviceClasses = new ArrayList<>();
    private final List<Class<?>> nonServiceClasses = new ArrayList<>();
    private final Set<String> schemaReferences = new HashSet<>();

    private final Package testPackage;

    public AbstractServiceGenerationTest(List<Class<?>> testClasses) {
        collectServiceClasses(serviceClasses, nonServiceClasses, testClasses);
        testPackage = getClass().getPackage();
    }

    private void collectServiceClasses(List<Class<?>> serviceClasses,
            List<Class<?>> nonServiceClasses, List<Class<?>> inputClasses) {
        for (Class<?> testServiceClass : inputClasses) {
            if (testServiceClass.isAnnotationPresent(VaadinService.class)) {
                serviceClasses.add(testServiceClass);
            } else {
                nonServiceClasses.add(testServiceClass);
            }
            collectServiceClasses(serviceClasses, nonServiceClasses,
                    Arrays.asList(testServiceClass.getDeclaredClasses()));
        }
    }

    @Before
    public void setUpOutputFile() {
        openApiJsonOutput = java.nio.file.Paths.get(
                outputDirectory.getRoot().getAbsolutePath(), "openapi.json");
    }

    protected List<File> getTsFiles(File directory) {
        return Arrays.asList(
                directory.listFiles((dir, name) -> name.endsWith(".ts")));
    }

    protected String readFile(Path file) {
        try {
            return StringUtils.toEncodedString(Files.readAllBytes(file),
                    StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new AssertionError(
                    String.format("Failed to read the file '%s'", file));
        }
    }

    protected void verifyOpenApiObjectAndGeneratedTs() {
        generateAndVerify(null, null);
    }

    protected void verifyGenerationFully(URL customApplicationProperties,
            URL expectedOpenApiJsonResourceUrl) {
        generateAndVerify(customApplicationProperties, Objects.requireNonNull(
                expectedOpenApiJsonResourceUrl,
                "Full verification requires an expected open api spec file"));
    }

    private void generateAndVerify(URL customApplicationProperties,
            URL expectedOpenApiJsonResourceUrl) {
        PropertiesConfiguration applicationProperties = customApplicationProperties == null
                ? new PropertiesConfiguration()
                : TestUtils
                        .readProperties(customApplicationProperties.getPath());
        new OpenApiSpecGenerator(applicationProperties).generateOpenApiSpec(
                Collections
                        .singletonList(java.nio.file.Paths.get("src/test/java",
                                testPackage.getName().replace('.',
                                        File.separatorChar))),
                openApiJsonOutput);

        Assert.assertTrue(
                String.format("No generated json found at path '%s'",
                        openApiJsonOutput),
                openApiJsonOutput.toFile().exists());

        verifyOpenApiObject();
        if (expectedOpenApiJsonResourceUrl != null) {
            verifyOpenApiJson(expectedOpenApiJsonResourceUrl);
        }

        // TODO: call verifyTsModule and verifyModelTsModule when moving the
        // generator to flow
    }

    private void verifyOpenApiObject() {
        OpenAPI actualOpenAPI = getOpenApiObject();
        assertPaths(actualOpenAPI.getPaths(), serviceClasses);

        if (!nonServiceClasses.isEmpty()) {
            assertComponentSchemas(actualOpenAPI.getComponents().getSchemas(),
                    nonServiceClasses);
        } else {
            Map<String, Schema> componentSchemas = Optional
                    .ofNullable(actualOpenAPI.getComponents())
                    .map(Components::getSchemas).orElse(Collections.emptyMap());
            assertTrue(String.format(
                    "Got schemas that correspond to no class provided in test parameters, schemas: '%s'",
                    componentSchemas), componentSchemas.isEmpty());
        }

        verifySchemaReferences();
    }

    private OpenAPI getOpenApiObject() {
        OpenApiObjectGenerator generator = new OpenApiObjectGenerator();

        Path javaSourcePath = java.nio.file.Paths.get("src/test/java/",
                testPackage.getName().replace('.', File.separatorChar));
        generator.addSourcePath(javaSourcePath);

        generator.setOpenApiConfiguration(new OpenApiConfiguration("Test title",
                "0.0.1", "https://server.test", "Test description"));

        return generator.getOpenApi();
    }

    private void assertPaths(Paths actualPaths,
            List<Class<?>> testServiceClasses) {
        int pathCount = 0;
        for (Class<?> testServiceClass : testServiceClasses) {
            for (Method expectedServiceMethod : testServiceClass
                    .getDeclaredMethods()) {
                // TODO: Check the security annotation as well when moving
                // the access checker to flow. Ref:
                // https://github.com/vaadin/vaadin-connect/blob/master/vaadin-connect-maven-plugin/src/test/java/com/vaadin/connect/plugin/generator/services/AbstractServiceGenerationTest.java#L228
                if (!Modifier.isPublic(expectedServiceMethod.getModifiers())) {
                    continue;
                }
                pathCount++;
                String expectedServiceUrl = String.format("/%s/%s",
                        getServiceName(testServiceClass),
                        expectedServiceMethod.getName());
                PathItem actualPath = actualPaths.get(expectedServiceUrl);
                assertNotNull(String.format(
                        "Expected to find a path '%s' for the service method '%s' in the class '%s'",
                        expectedServiceUrl, expectedServiceMethod,
                        testServiceClass), actualPath);
                assertPath(testServiceClass, expectedServiceMethod, actualPath);
            }
        }
        assertEquals("Unexpected number of OpenAPI paths found", pathCount,
                actualPaths.size());
    }

    private String getServiceName(Class<?> testServiceClass) {
        String customName = testServiceClass.getAnnotation(VaadinService.class)
                .value();
        return customName.isEmpty() ? testServiceClass.getSimpleName()
                : customName;
    }

    private void assertPath(Class<?> testServiceClass,
            Method expectedServiceMethod, PathItem actualPath) {
        Operation actualOperation = actualPath.getPost();
        assertEquals("Unexpected tag in the OpenAPI spec",
                actualOperation.getTags(),
                Collections.singletonList(testServiceClass.getSimpleName()));
        assertTrue(String.format(
                "Unexpected OpenAPI operation id: does not contain the service name of the class '%s'",
                testServiceClass.getSimpleName()),
                actualOperation.getOperationId()
                        .contains(getServiceName(testServiceClass)));
        assertTrue(String.format(
                "Unexpected OpenAPI operation id: does not contain the name of the service method '%s'",
                expectedServiceMethod.getName()),
                actualOperation.getOperationId()
                        .contains(expectedServiceMethod.getName()));

        if (expectedServiceMethod.getParameterCount() > 0) {
            Schema requestSchema = extractSchema(
                    actualOperation.getRequestBody().getContent());
            assertRequestSchema(requestSchema,
                    expectedServiceMethod.getParameterTypes());
        } else {
            assertNull(String.format(
                    "No request body should be present in path schema for service method with no parameters, method: '%s'",
                    expectedServiceMethod), actualOperation.getRequestBody());
        }

        ApiResponses responses = actualOperation.getResponses();
        assertEquals(
                "Every operation is expected to have a single '200' response",
                1, responses.size());
        ApiResponse apiResponse = responses.get("200");
        assertNotNull(
                "Every operation is expected to have a single '200' response",
                apiResponse);

        if (expectedServiceMethod.getReturnType() != void.class) {
            assertSchema(extractSchema(apiResponse.getContent()),
                    expectedServiceMethod.getReturnType());
        } else {
            assertNull(String.format(
                    "No response is expected to be present for void method '%s'",
                    expectedServiceMethod), apiResponse.getContent());
        }

        // TODO: test the AllowAnonymous annotation when moving access check
        // to flow. Ref:
        // https://github.com/vaadin/vaadin-connect/blob/master/vaadin-connect-maven-plugin/src/test/java/com/vaadin/connect/plugin/generator/services/AbstractServiceGenerationTest.java#L297
    }

    private void assertRequestSchema(Schema requestSchema,
            Class<?>... parameterTypes) {
        Map<String, Schema> properties = requestSchema.getProperties();
        assertEquals(
                "Request schema should have the same amount of properties as the corresponding service method parameters number",
                parameterTypes.length, properties.size());
        int index = 0;
        for (Schema propertySchema : properties.values()) {
            assertSchema(propertySchema, parameterTypes[index]);
            index++;
        }

        verifyThatAllPropertiesAreRequired(requestSchema, properties);
    }

    private Schema extractSchema(Content content) {
        assertEquals("Expecting a single application content — a json schema",
                1, content.size());
        return content.get("application/json").getSchema();
    }

    private void assertComponentSchemas(Map<String, Schema> actualSchemas,
            List<Class<?>> testServiceClasses) {
        int schemasCount = 0;
        for (Class<?> expectedSchemaClass : testServiceClasses) {
            schemasCount++;
            Schema actualSchema = actualSchemas
                    .get(expectedSchemaClass.getCanonicalName());
            assertNotNull(String.format(
                    "Expected to have a schema defined for a class '%s'",
                    expectedSchemaClass), actualSchema);
            assertSchema(actualSchema, expectedSchemaClass);
        }
        assertEquals("Expected to have all service classes defined in schemas",
                schemasCount, actualSchemas.size());
    }

    private void assertSchema(Schema actualSchema,
            Class<?> expectedSchemaClass) {
        if (assertSpecificJavaClassSchema(actualSchema, expectedSchemaClass)) {
            return;
        }

        if (actualSchema.get$ref() != null) {
            assertNull(actualSchema.getProperties());
            schemaReferences.add(actualSchema.get$ref());
        } else {
            if (actualSchema instanceof StringSchema) {
                assertTrue(String.class.isAssignableFrom(expectedSchemaClass));
            } else if (actualSchema instanceof BooleanSchema) {
                assertTrue((boolean.class.isAssignableFrom(expectedSchemaClass)
                        || Boolean.class
                                .isAssignableFrom(expectedSchemaClass)));
            } else if (actualSchema instanceof NumberSchema) {
                assertTrue(JSON_NUMBER_CLASSES.stream()
                        .anyMatch(jsonNumberClass -> jsonNumberClass
                                .isAssignableFrom(expectedSchemaClass)));
            } else if (actualSchema instanceof ArraySchema) {
                if (expectedSchemaClass.isArray()) {
                    assertSchema(((ArraySchema) actualSchema).getItems(),
                            expectedSchemaClass.getComponentType());
                } else {
                    assertTrue(Collection.class
                            .isAssignableFrom(expectedSchemaClass));
                }
            } else if (actualSchema instanceof MapSchema) {
                assertTrue(Map.class.isAssignableFrom(expectedSchemaClass));
            } else if (actualSchema instanceof DateTimeSchema) {
                assertTrue(Instant.class.isAssignableFrom(expectedSchemaClass)
                        || LocalDateTime.class
                                .isAssignableFrom(expectedSchemaClass));
            } else if (actualSchema instanceof DateSchema) {
                assertTrue(Date.class.isAssignableFrom(expectedSchemaClass)
                        || LocalDate.class
                                .isAssignableFrom(expectedSchemaClass));
            } else if (actualSchema instanceof ComposedSchema) {
                List<Schema> allOf = ((ComposedSchema) actualSchema).getAllOf();
                if (allOf.size() > 1) {
                    // Inherited schema
                    for (Schema schema : allOf) {
                        if (expectedSchemaClass.getCanonicalName()
                                .equals(schema.getName())) {
                            assertSchemaProperties(expectedSchemaClass, schema);
                            break;
                        }
                    }
                } else {
                    // Nullable schema for referring schema object
                    assertEquals(1, allOf.size());
                    assertEquals(expectedSchemaClass.getCanonicalName(),
                            allOf.get(0).getName());
                }
            } else if (actualSchema instanceof ObjectSchema) {
                assertSchemaProperties(expectedSchemaClass, actualSchema);
            } else {
                throw new AssertionError(
                        String.format("Unknown schema '%s' for class '%s'",
                                actualSchema.getClass(), expectedSchemaClass));
            }
        }
    }

    private boolean assertSpecificJavaClassSchema(Schema actualSchema,
            Class<?> expectedSchemaClass) {
        if (expectedSchemaClass == Optional.class) {
            assertTrue(actualSchema.getNullable());
            if (actualSchema instanceof ComposedSchema) {
                assertEquals(1,
                        ((ComposedSchema) actualSchema).getAllOf().size());
            }
        } else if (expectedSchemaClass == Object.class) {
            assertNull(actualSchema.getProperties());
            assertNull(actualSchema.getAdditionalProperties());
            assertNull(actualSchema.get$ref());
            assertNull(actualSchema.getRequired());
        } else {
            return false;
        }
        return true;
    }

    private void assertSchemaProperties(Class<?> expectedSchemaClass,
            Schema schema) {
        int expectedFieldsCount = 0;
        Map<String, Schema> properties = schema.getProperties();
        assertNotNull(properties);
        assertTrue(properties.size() > 0);
        for (Field expectedSchemaField : expectedSchemaClass
                .getDeclaredFields()) {
            if (Modifier.isTransient(expectedSchemaField.getModifiers())
                    || Modifier.isStatic(expectedSchemaField.getModifiers())
                    || expectedSchemaField
                            .isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }

            expectedFieldsCount++;
            Schema propertySchema = properties
                    .get(expectedSchemaField.getName());
            assertNotNull(String.format("Property schema is not found %s",
                    expectedSchemaField.getName()), propertySchema);
            assertSchema(propertySchema, expectedSchemaField.getType());
        }
        assertEquals(expectedFieldsCount, properties.size());

        verifyThatAllPropertiesAreRequired(schema, properties);
    }

    private void verifyThatAllPropertiesAreRequired(Schema schema,
            Map<String, Schema> properties) {
        if (properties.isEmpty()) {
            assertNull(schema.getRequired());
        } else {
            for (Map.Entry<String, Schema> propertySchema : properties
                    .entrySet()) {
                if (BooleanUtils
                        .isNotTrue(propertySchema.getValue().getNullable())) {
                    assertTrue(schema.getRequired()
                            .contains(propertySchema.getKey()));
                }
            }
        }
    }

    private void verifySchemaReferences() {
        nonServiceClasses.stream().map(Class::getCanonicalName)
                .forEach(schemaClass -> schemaReferences.removeIf(ref -> ref
                        .endsWith(String.format("/%s", schemaClass))));
        String errorMessage = String.format(
                "Got schema references that are not in the OpenAPI schemas: '%s'",
                StringUtils.join(schemaReferences, ","));
        Assert.assertTrue(errorMessage, schemaReferences.isEmpty());
    }

    private void verifyOpenApiJson(URL expectedOpenApiJsonResourceUrl) {
        assertEquals(TestUtils.readResource(expectedOpenApiJsonResourceUrl),
                readFile(openApiJsonOutput));
    }

    private void verifyTsModule() {
        List<File> foundFiles = getTsFiles(outputDirectory.getRoot());
        assertEquals(String.format(
                "Expected to have only %s classes processed in the test '%s', but found the following files: '%s'",
                serviceClasses.size(), serviceClasses, foundFiles),
                serviceClasses.size(), foundFiles.size());
        for (Class<?> expectedClass : serviceClasses) {
            assertClassGeneratedTs(expectedClass);
        }
    }

    private void verifyModelTsModule() {
        nonServiceClasses.forEach(this::assertModelClassGeneratedTs);
    }

    private void assertClassGeneratedTs(Class<?> expectedClass) {
        String classResourceUrl = String.format("expected-%s.ts",
                expectedClass.getSimpleName());
        URL expectedResource = this.getClass().getResource(classResourceUrl);
        Assert.assertNotNull(String.format("Expected file is not found at %s",
                classResourceUrl), expectedResource);
        String expectedTs = TestUtils.readResource(expectedResource);

        Path outputFilePath = outputDirectory.getRoot().toPath()
                .resolve(expectedClass.getSimpleName() + ".ts");

        Assert.assertEquals(
                String.format(
                        "Class '%s' has unexpected json produced in file '%s'",
                        expectedClass, expectedResource.getPath()),
                expectedTs, readFile(outputFilePath));
    }

    private void assertModelClassGeneratedTs(Class<?> expectedClass) {
        String canonicalName = expectedClass.getCanonicalName();
        String modelResourceUrl = String.format("expected-model-%s.ts",
                canonicalName);
        URL expectedResource = this.getClass().getResource(modelResourceUrl);
        Assert.assertNotNull(String.format("Expected file is not found at %s",
                modelResourceUrl), expectedResource);
        String expectedTs = TestUtils.readResource(expectedResource);

        Path outputFilePath = outputDirectory.getRoot().toPath().resolve(
                StringUtils.replaceChars(canonicalName, '.', '/') + ".ts");

        Assert.assertEquals(String.format(
                "Model class '%s' has unexpected typescript produced in file '%s'",
                expectedClass, expectedResource.getPath()), expectedTs,
                readFile(outputFilePath));
    }
}

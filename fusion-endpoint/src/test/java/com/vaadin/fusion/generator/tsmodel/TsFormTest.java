/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.tsmodel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.vaadin.fusion.generator.endpoints.AbstractEndpointGeneratorBaseTest;
import com.vaadin.fusion.utils.TestUtils;

import elemental.json.JsonObject;
import static com.vaadin.fusion.generator.OpenAPIObjectGenerator.CONSTRAINT_ANNOTATIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TsFormTest extends AbstractEndpointGeneratorBaseTest {
    private Pattern TS_FORM_FILE_LINK_PATTERN = Pattern
            .compile("file://.*\\.java");

    public TsFormTest() {
        super(Collections.singletonList(TsFormEndpoint.class));
    }

    @Test
    public void should_addEntityJavaAnnotations_toOpenApi() {
        generateOpenApi(null);

        JsonObject apiJson = readJsonFile(openApiJsonOutput);

        String modelName = TsFormEndpoint.MyEntity.class.getName().replace('$',
                '.');

        JsonObject props = apiJson.getObject("components").getObject("schemas")
                .getObject(modelName).getArray("allOf").getObject(1)
                .getObject("properties");

        assertFalse(props.getObject("foo").hasKey(CONSTRAINT_ANNOTATIONS));
        assertEquals("AssertFalse()", props.getObject("assertFalse")
                .getArray(CONSTRAINT_ANNOTATIONS).getString(0));
        assertEquals("AssertTrue()", props.getObject("assertTrue")
                .getArray(CONSTRAINT_ANNOTATIONS).getString(0));
        assertEquals("Digits({integer:5, fraction:2})",
                props.getObject("digits").getArray(CONSTRAINT_ANNOTATIONS)
                        .getString(0));
        assertEquals("NotEmpty()", props.getObject("notEmpty")
                .getArray(CONSTRAINT_ANNOTATIONS).getString(0));
        assertEquals("NotNull()", props.getObject("notEmpty")
                .getArray(CONSTRAINT_ANNOTATIONS).getString(1));
        assertEquals("NotNull()", props.getObject("notNullEntity")
                .getArray(CONSTRAINT_ANNOTATIONS).getString(0));
    }

    @Test
    public void should_generate_FormModels() throws IOException {
        generateOpenApi(null);

        generateTsEndpoints();

        String entityIdPath = TsFormEndpoint.MyEntityId.class.getName()
                .replaceAll("[\\.\\$]", "/");
        String entityPath = TsFormEndpoint.MyEntity.class.getName()
                .replaceAll("[\\.\\$]", "/");

        File entityIdFile = new File(outputDirectory.getRoot(),
                entityIdPath + ".ts");
        File formModelIdFile = new File(outputDirectory.getRoot(),
                entityIdPath + "Model.ts");
        File entityFile = new File(outputDirectory.getRoot(),
                entityPath + ".ts");
        File formModelFile = new File(outputDirectory.getRoot(),
                entityPath + "Model.ts");

        assertTrue(entityIdFile.exists());
        assertTrue(formModelIdFile.exists());
        assertTrue(entityFile.exists());
        assertTrue(formModelFile.exists());

        String actual = new String(Files.readAllBytes(formModelFile.toPath()));
        final String expected = new String(Files.readAllBytes(new File(
                getClass().getResource("expected-TsFormEndpoint.ts").getFile())
                        .toPath()));

        final Matcher matcher = TS_FORM_FILE_LINK_PATTERN.matcher(actual);
        if (matcher.find()) {
            final String uri = matcher.group(0);
            final String updatedUri = uri.replace('\\', '/').replaceAll(
                    "file://.*/fusion-endpoint", "file:///.../fusion-endpoint");
            actual = actual.replace(uri, updatedUri);
        }

        TestUtils.equalsIgnoreWhiteSpaces(expected, actual);
    }

    private boolean isFileRefenreceLine(String line) {
        return line.contains("@see {@link file:");
    }
}

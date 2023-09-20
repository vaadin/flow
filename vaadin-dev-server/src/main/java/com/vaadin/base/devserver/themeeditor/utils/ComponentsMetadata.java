package com.vaadin.base.devserver.themeeditor.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ComponentsMetadata {
    public static String getMetadataContent(String componentName)
            throws IOException {
        try (InputStream is = ComponentsMetadata.class.getResourceAsStream(
                String.format("/META-INF/metadata/%s.json", componentName))) {
            return readFromInputStream(is);
        }
    }

    private static String readFromInputStream(InputStream is)
            throws IOException {
        try (BufferedReader bf = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            bf.lines().forEach(builder::append);
            return builder.toString();
        }
    }

}

/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.hummingbird.template.parser.TemplateParser;
import com.vaadin.hummingbird.template.parser.TemplateResolver;

/**
 * Builder for template includes.
 *
 * @author Vaadin Ltd
 */
public class TemplateIncludeBuilder implements TemplateNodeBuilder {

    private String[] relativeFilenames;
    private TemplateResolver templateResolver;

    /**
     * Creates a new builder for the given filename using the given resolver.
     *
     * @param relativeFilenames
     *            the file names for the included file
     * @param templateResolver
     *            the resolver to use to find the file
     */
    public TemplateIncludeBuilder(TemplateResolver templateResolver,
            String... relativeFilenames) {
        this.relativeFilenames = relativeFilenames;
        this.templateResolver = templateResolver;
    }

    @Override
    public Collection<? extends TemplateNode> build(TemplateNode parent) {
        assert parent instanceof AbstractElementTemplateNode : "@include@ parent must be an instance of "
                + AbstractElementTemplateNode.class;

        return Stream.of(relativeFilenames).map(this::parseInclude)
                .collect(Collectors.toList());
    }

    private TemplateNode parseInclude(String includeFileName) {
        // Need a new resolver so that includes from the included file are
        // relative to that file (directory)
        DelegateResolver subResolver = new DelegateResolver(templateResolver,
                getFolder(includeFileName));
        try (InputStream templateContentStream = templateResolver
                .resolve(includeFileName)) {
            return TemplateParser.parse(templateContentStream, subResolver);
        } catch (IOException e) {
            throw new TemplateParseException(
                    "Unable to read template include for '" + includeFileName
                            + "'",
                    e);
        }
    }

    private static String getFolder(String relativeFilename) {
        String folder = new File(relativeFilename).getParent();
        if (folder != null) {
            return folder;
        } else {
            return ".";
        }
    }

}

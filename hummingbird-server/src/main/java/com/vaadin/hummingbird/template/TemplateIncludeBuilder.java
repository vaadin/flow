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

import java.io.IOException;
import java.io.InputStream;

import com.vaadin.hummingbird.template.parser.TemplateParser;
import com.vaadin.hummingbird.template.parser.TemplateResolver;

/**
 * Builder for template includes.
 *
 * @author Vaadin Ltd
 */
public class TemplateIncludeBuilder implements TemplateNodeBuilder {

    private String relativeFilename;
    private TemplateResolver templateResolver;

    /**
     * Creates a new builder for the given filename using the given resolver.
     *
     * @param relativeFilename
     *            the file name for the included file
     * @param templateResolver
     *            the resolver to use to find the file
     */
    public TemplateIncludeBuilder(String relativeFilename,
            TemplateResolver templateResolver) {
        this.relativeFilename = relativeFilename;
        this.templateResolver = templateResolver;
    }

    @Override
    public TemplateNode build(TemplateNode parent) {
        assert parent instanceof AbstractElementTemplateNode : "@include@ parent must be an instance of "
                + AbstractElementTemplateNode.class;

        try (InputStream templateContentStream = templateResolver
                .resolve(relativeFilename)) {
            return TemplateParser.parse(templateContentStream,
                    templateResolver);
        } catch (IOException e) {
            throw new TemplateParseException(
                    "Unable to read template include for '" + relativeFilename
                            + "'",
                    e);
        }
    }
}

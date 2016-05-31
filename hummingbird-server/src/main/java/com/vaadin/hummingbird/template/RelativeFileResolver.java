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
import java.io.InputStream;

import com.vaadin.hummingbird.template.parser.TemplateResolver;

/**
 * A resolver capable of finding a file given its name relative to a reference
 * file and a reference class.
 *
 * @author Vaadin Ltd
 */
public class RelativeFileResolver implements TemplateResolver {

    private Class<?> referenceClass;
    private String referenceDir;

    /**
     * Creates a new resolver for the given class and reference file.
     *
     * @param referenceClass
     *            the class the reference file name is relative to
     * @param referenceFileName
     *            the reference file to use when resolving other files
     */
    public RelativeFileResolver(Class<?> referenceClass,
            String referenceFileName) {
        this.referenceClass = referenceClass;
        referenceDir = new File(referenceFileName).getParent();
        if (referenceDir == null) {
            referenceDir = "";
        } else if (!referenceDir.endsWith("/")) {
            referenceDir += "/";
        }
    }

    @Override
    public InputStream resolve(String relativeOrAbsoluteFilename)
            throws TemplateParseException {
        if (relativeOrAbsoluteFilename.startsWith("/")) {
            return resolveAbsolute(relativeOrAbsoluteFilename);
        } else {
            return resolveRelative(relativeOrAbsoluteFilename);
        }
    }

    private InputStream resolveRelative(String relativeOrAbsoluteFilename)
            throws TemplateParseException {
        String fileRelativeToClass = referenceDir + relativeOrAbsoluteFilename;
        InputStream templateContentStream = referenceClass
                .getResourceAsStream(fileRelativeToClass);
        if (templateContentStream == null) {
            throw new TemplateParseException(relativeOrAbsoluteFilename
                    + " was resolved to " + fileRelativeToClass
                    + " but that file could not be resolved relative to "
                    + referenceClass.getName());
        }
        return templateContentStream;
    }

    private InputStream resolveAbsolute(String absoluteFilename)
            throws TemplateParseException {
        assert absoluteFilename.startsWith("/");
        InputStream templateContentStream = referenceClass.getClassLoader()
                .getResourceAsStream(absoluteFilename.substring(1));
        if (templateContentStream == null) {
            throw new TemplateParseException(
                    "Template " + absoluteFilename + " could not be found");
        }
        return templateContentStream;
    }

}

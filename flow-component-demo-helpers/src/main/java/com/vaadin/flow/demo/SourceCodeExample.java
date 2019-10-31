/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.demo;

import java.io.Serializable;

/**
 * Object that defines a source code example to be shown together with a demo.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SourceCodeExample implements Serializable {

    /**
     * Defines which language the source code is in.
     */
    public enum SourceType {
        JAVA, CSS, HTML, UNDEFINED
    }

    private String heading;
    private String sourceCode;
    private SourceType sourceType;

    /**
     * Constructs a new source code example.
     */
    public SourceCodeExample() {
        sourceType = SourceType.UNDEFINED;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }
}

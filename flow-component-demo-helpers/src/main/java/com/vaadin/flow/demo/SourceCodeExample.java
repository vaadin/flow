/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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

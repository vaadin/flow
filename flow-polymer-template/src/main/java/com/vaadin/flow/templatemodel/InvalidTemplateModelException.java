/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.templatemodel;

import com.vaadin.flow.component.littemplate.LitTemplate;

/**
 * Exception thrown when encountering an invalid type in a template model.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * 
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@link LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a> For lit templates, you can use {@code @Id}
 *             mapping and the component API or the element API with property
 *             synchronization instead.
 */
@Deprecated
public class InvalidTemplateModelException extends RuntimeException {
    /**
     * Creates a new exception with the given message and cause.
     * 
     * @param message
     *            the exception message
     * @param cause
     *            the cause of the exception
     */
    public InvalidTemplateModelException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new exception with the given message.
     *
     * @param message
     *            the exception message
     */
    public InvalidTemplateModelException(String message) {
        super(message);
    }

}

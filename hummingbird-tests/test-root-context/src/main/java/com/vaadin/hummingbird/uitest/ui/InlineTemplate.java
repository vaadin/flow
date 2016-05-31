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
package com.vaadin.hummingbird.uitest.ui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.ui.Template;

/**
 * @author Vaadin Ltd
 *
 */
public class InlineTemplate extends Template {

    /**
     * Creates a template instance with the given template HTML text.
     * <p>
     * Note: super constructor uses the {@code String} parameter as a file path,
     * not as a content.
     *
     * @param templateHtml
     *            the template HTML
     */
    public InlineTemplate(String templateHtml) {
        super(new ByteArrayInputStream(
                templateHtml.getBytes(StandardCharsets.UTF_8)));
    }

}

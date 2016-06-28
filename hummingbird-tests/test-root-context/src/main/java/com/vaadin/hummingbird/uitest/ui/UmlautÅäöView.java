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

import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Template;

public class UmlautÅäöView extends Template implements View {

    public UmlautÅäöView() {
        super(new ByteArrayInputStream(
                "<div id='content'>Umlaut view: åäöü. a\u00adb</div>"
                        .getBytes(StandardCharsets.UTF_8)));

    }
}

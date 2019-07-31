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
package com.vaadin.flow.migration.samplecode;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.component.dependency.HtmlImport;

@HtmlImport("frontend://foo.html")
@HtmlImport("base://foo1.html")
@HtmlImport("context://foo2.html")
@HtmlImport("bar.html")
@HtmlImport("/bar1.html")
@HtmlImport("src/baz.html")
@HtmlImport("frontend://bower_components/vaadin-button/src/vaadin-button.html")
@HtmlImport("bower_components/vaadin-text-field/src/vaadin-text-field.html")
public class Component1<T extends List<?> & Serializable>
        extends GenericComponent<T, String> {

}

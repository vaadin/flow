/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.legacylitaddon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * An add-on component that pins the Lit packages Flow also depends on, to the
 * versions that were current when the add-on was written. Lit itself is not
 * pinned, so {@code lit} resolves to the version Flow manages while
 * {@code lit-element} and {@code lit-html} resolve to the pinned ones, which
 * the two cannot be combined with.
 */
@Tag("legacy-lit-addon")
@NpmPackage(value = "lit-element", version = "^3.2.2")
@NpmPackage(value = "lit-html", version = "2.4.0")
@JsModule("./legacy-lit-addon.js")
public class LegacyLitAddon extends Component {
}

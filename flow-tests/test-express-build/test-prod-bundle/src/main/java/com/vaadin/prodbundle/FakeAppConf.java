/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.prodbundle;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

@Theme("vaadin-prod-bundle")
@CssImport("@vaadin/vaadin-lumo-styles/utility.css")
@PWA(name = "vaadin-prod-bundle", shortName = "vaadin-prod-bundle")
@JsModule("@vaadin/horizontal-layout")
@NpmPackage(value = "@vaadin/horizontal-layout", version = TestVersion.VAADIN)
public class FakeAppConf implements AppShellConfigurator {

}

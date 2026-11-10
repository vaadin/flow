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
package com.vaadin.flow.devloop.test.app;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;

/**
 * A supertype that declares a frontend import of its own.
 * <p>
 * Every real Vaadin view has one - {@code MainView extends VerticalLayout}, and
 * VerticalLayout declares {@code @JsModule} and {@code @NpmPackage}. A fixture
 * built on bare {@code flow-html-components} does not, and that gap is what let
 * a regression through: all of these annotations are {@code @Inherited}, so a
 * view inherits its supertype's whole import closure, and a check that compared
 * the inherited closure rather than what the class declares escalated an
 * ordinary method-body edit to a restart.
 * <p>
 * Outside the {@code mutable} package on purpose: the ITs rewrite that package,
 * and this class is scenery rather than subject.
 */
@JsModule("./greeting.ts")
public class ImportingLayout extends Div {
}

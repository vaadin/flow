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
package com.vaadin.flow.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.Component;

/**
 * Associates an integration test with the view it exercises. When present, the
 * default {@link AbstractDefaultIT#getTestPath()} derives the path to open from
 * the view's {@link com.vaadin.flow.router.Route @Route}, so the test does not
 * need to repeat the route.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestFor {

    /**
     * The view under test. It must declare an explicit
     * {@link com.vaadin.flow.router.Route @Route} value.
     *
     * @return the view class
     */
    Class<? extends Component> value();
}

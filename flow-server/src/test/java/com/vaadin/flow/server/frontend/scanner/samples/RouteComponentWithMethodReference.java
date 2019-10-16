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

package com.vaadin.flow.server.frontend.scanner.samples;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

@Route("")
public class RouteComponentWithMethodReference extends Component implements HasComponents {

    @JsModule("foo.js")
    public static class MyComponent extends Component {
    }

    @JsModule("bar.js")
    public static class AnotherComponent extends Component {
    }

    @JsModule("baz.js")
    public static class YetAnotherComponent extends Component {
    }

    private Supplier<Component> fieldGenerator = YetAnotherComponent::new;

    public RouteComponentWithMethodReference() {
        List<Supplier<Component>> suppliers =
                Collections.singletonList(AnotherComponent::new);

        Supplier<Component> generator = MyComponent::new;
        add(generator.get(), fieldGenerator.get());
    }
}

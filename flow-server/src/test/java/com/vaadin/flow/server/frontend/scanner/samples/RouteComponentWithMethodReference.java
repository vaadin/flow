/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
public class RouteComponentWithMethodReference extends Component
        implements HasComponents {

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
        List<Supplier<Component>> suppliers = Collections
                .singletonList(AnotherComponent::new);

        Supplier<Component> generator = MyComponent::new;
        add(generator.get(), fieldGenerator.get());
    }
}

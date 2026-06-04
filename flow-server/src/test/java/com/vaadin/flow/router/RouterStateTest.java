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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterStateTest {

    @Tag("test-view")
    private static class TestView extends Component {
    }

    @Test
    void rejectsNullLocation() {
        assertThrows(NullPointerException.class, () -> new RouterState(null,
                RouteParameters.empty(), Collections.emptyList(), null));
    }

    @Test
    void rejectsNullRouteParameters() {
        assertThrows(NullPointerException.class,
                () -> new RouterState(new Location(""), null,
                        Collections.emptyList(), null));
    }

    @Test
    void rejectsNullActiveChain() {
        assertThrows(NullPointerException.class,
                () -> new RouterState(new Location(""), RouteParameters.empty(),
                        null, null));
    }

    @Test
    void activeChainIsDefensivelyCopied() {
        TestView view = new TestView();
        List<HasElement> source = new ArrayList<>();
        source.add(view);

        RouterState state = new RouterState(new Location("foo"),
                RouteParameters.empty(), source, TestView.class);

        // Mutating the source list must not affect the snapshot
        source.clear();
        assertEquals(1, state.activeChain().size());
        assertSame(view, state.activeChain().get(0));

        // The exposed list must be unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> state.activeChain().add(new TestView()));
    }

    @Test
    void currentViewReturnsLeafWhenChainNonEmpty() {
        TestView leaf = new TestView();
        HasElement parent = () -> new Element("div");

        RouterState state = new RouterState(new Location("x"),
                RouteParameters.empty(), List.of(leaf, parent), TestView.class);

        assertTrue(state.currentView().isPresent());
        assertSame(leaf, state.currentView().get());
    }

    @Test
    void currentViewIsEmptyBeforeFirstNavigation() {
        RouterState state = new RouterState(new Location(""),
                RouteParameters.empty(), Collections.emptyList(), null);

        assertTrue(state.currentView().isEmpty());
    }
}

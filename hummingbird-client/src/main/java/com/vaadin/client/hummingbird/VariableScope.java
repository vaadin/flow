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
package com.vaadin.client.hummingbird;

import java.util.Optional;

import com.vaadin.client.hummingbird.collection.JsCollections;
import com.vaadin.client.hummingbird.collection.JsMap;

/**
 * @author Vaadin Ltd
 *
 */
public class VariableScope {

    private final JsMap<String, StateNode> variables;
    private final VariableScope parentScope;

    public VariableScope(StateNode node) {
        this("", node, null);
    }

    public VariableScope(String variableName, StateNode variable,
            VariableScope parent) {
        variables = JsCollections.map();
        variables.set(variableName, variable);
        parentScope = parent;
    }

    public Optional<StateNode> getVariable(String name) {
        VariableScope scope = this;
        while (scope != null) {
            if (scope.variables.has(name)) {
                return Optional.ofNullable(scope.variables.get(name));
            }
            scope = scope.parentScope;
        }
        return Optional.empty();
    }

    public Optional<StateNode> getCurrent() {
        return getVariable("");
    }
}

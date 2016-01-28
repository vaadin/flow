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
package com.vaadin.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestComponentContainer extends AbstractComponentContainer {

    private List<Component> components = new ArrayList<Component>();

    @Override
    public void replaceComponent(Component oldComponent,
            Component newComponent) {
        throw new UnsupportedOperationException();

    }

    @Override
    public void addComponent(Component c) {
        super.addComponent(c);
        components.add(c);
    }

    @Override
    public void removeComponent(Component c) {
        super.removeComponent(c);
        components.remove(c);
    }

    @Override
    public int getComponentCount() {
        return components.size();
    }

    @Override
    public Iterator<Component> iterator() {
        return components.iterator();
    }

    public Component getComponent(int i) {
        return components.get(i);
    }

}

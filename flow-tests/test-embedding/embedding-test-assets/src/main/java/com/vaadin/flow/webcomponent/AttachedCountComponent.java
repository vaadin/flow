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
package com.vaadin.flow.webcomponent;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;

/**
 * Shows how many instances of this component are attached to the UI at the time
 * it is attached itself.
 */
public class AttachedCountComponent extends Div {

    public AttachedCountComponent() {
        setId("count");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        Set<AttachedCountComponent> attached = getAttached(attachEvent.getUI());
        attached.add(this);
        setText(String.valueOf(attached.size()));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        getAttached(detachEvent.getUI()).remove(this);
    }

    @SuppressWarnings("unchecked")
    private static Set<AttachedCountComponent> getAttached(UI ui) {
        Set<AttachedCountComponent> attached = (Set<AttachedCountComponent>) ComponentUtil
                .getData(ui, AttachedCountComponent.class.getName());
        if (attached == null) {
            attached = new HashSet<>();
            ComponentUtil.setData(ui, AttachedCountComponent.class.getName(),
                    attached);
        }
        return attached;
    }
}

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

package com.vaadin.flow.misc.ui;

import java.io.Serializable;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;

public class UITrackerListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        UITracker tracker = new UITracker();
        event.getSource().addUIInitListener(uiEvent -> {
            UI ui = uiEvent.getUI();
            tracker.add(ui);
            ComponentUtil.setData(ui, UITracker.class, tracker);
        });
    }

    public static class UITracker {
        private Map<Key, WeakReference<UI>> uiMap = new HashMap<>();

        private record Key(String sessionId, int uIid) implements Serializable {
        }

        private void add(UI ui) {
            Key key = new Key(
                    ui.getInternals().getSession().getSession().getId(),
                    ui.getUIId());
            uiMap.put(key, new WeakReference<>(ui, new ReferenceQueue<>()));
        }

        public Set<Integer> getCollectedUIs(VaadinSession vaadinSession) {
            String sessionId = vaadinSession.getSession().getId();
            return uiMap.entrySet().stream()
                    .filter(e -> sessionId.equals(e.getKey().sessionId()))
                    .filter(e -> e.getValue().refersTo(null))
                    .map(e -> e.getKey().uIid()).collect(Collectors.toSet());
        }
    }
}

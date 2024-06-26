/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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

        private static class Key implements Serializable {
            private final String sessionId;
            private final int uIid;

            public Key(String sessionId, int uIid) {
                this.sessionId = sessionId;
                this.uIid = uIid;
            }

            int uIid() {
                return uIid;
            }

            String sessionId() {
                return sessionId;
            }
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
                    .filter(e -> e.getValue().get() == null)
                    .map(e -> e.getKey().uIid()).collect(Collectors.toSet());
        }
    }
}

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
package com.vaadin.base.devserver.devloop;

import jakarta.annotation.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.HotswapCompleteEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;

/**
 * An observer on Flow's own {@link VaadinHotswapper} SPI, so the dev loop can
 * report what Flow did with a redefine rather than guess.
 * <p>
 * The daemon needs neither a completion signal nor a classification of its own:
 * Flow already computes both, and {@code onHotswapComplete} is the
 * authoritative "the runtime leg is done" event that a transaction gates on.
 * <p>
 * The {@link Priority} is deliberately large so this runs after Flow's own
 * hotswappers (they sort ascending), meaning anything they decided has already
 * been decided by the time this sees the event.
 * <p>
 * Flow does not expose the refresh strategy it computed - the public
 * {@code getUIUpdateStrategy} only reports a strategy a hotswapper explicitly
 * requested - so the strategy is logged for diagnosis but never used as the
 * apply classification. The daemon reports what it did itself instead.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@Priority(10_000)
public class DevLoopHotswapper implements VaadinHotswapper {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DevLoopHotswapper.class);

    /**
     * Set by {@link #onInit}, which Flow's {@code Hotswapper} calls on every
     * instance it obtained from {@code Lookup}, so this points at the instance
     * that actually receives the events.
     */
    private static volatile DevLoopHotswapper active;

    private volatile boolean completed;
    private volatile boolean pageReloadRequired;

    /**
     * The instance receiving hotswap events, or {@code null} before Flow's
     * hotswapper has initialized.
     *
     * @return the active instance, or {@code null}
     */
    public static DevLoopHotswapper getActive() {
        return active;
    }

    @Override
    public void onInit(VaadinService vaadinService) {
        if (!DevLoopRegistration.isDaemonLaunched()) {
            // A hotswap agent creates Flow's hotswapper in applications the
            // daemon never launched, and every VaadinHotswapper on the
            // classpath
            // joins that chain. This one then has nothing to report to, so it
            // never becomes the active instance and every callback below is a
            // no-op.
            return;
        }
        active = this;
        LOGGER.debug("onInit");
    }

    /**
     * Whether this instance is the one the dev loop is reporting through.
     * <p>
     * False for an instance that joined a hotswapper chain the daemon did not
     * ask for, which is what keeps the class free of cost for everyone else.
     */
    private boolean isActive() {
        return active == this;
    }

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        if (!isActive()) {
            return;
        }
        // Guarded, because names() sorts a stream and the arguments of a debug
        // call are evaluated whether or not anything logs them.
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "onClassesChange(global) classes={} redefined={} requiresPageReload={} anyUIRequiresPageReload={}",
                    names(event), event.isRedefined(),
                    event.requiresPageReload(),
                    event.anyUIRequiresPageReload());
        }
        if (event.requiresPageReload() || event.anyUIRequiresPageReload()) {
            pageReloadRequired = true;
        }
    }

    @Override
    public void onClassesChange(HotswapClassSessionEvent event) {
        if (!isActive() || !LOGGER.isDebugEnabled()) {
            return;
        }
        List<String> perUi = new ArrayList<>();
        try {
            for (UI ui : event.getVaadinSession().getUIs()) {
                perUi.add(ui.getUIId() + "=" + event.getUIUpdateStrategy(ui)
                        .map(UIUpdateStrategy::name).orElse("none"));
            }
        } catch (RuntimeException ex) {
            perUi.add("error:" + ex);
        }
        LOGGER.debug("onClassesChange(session) classes={} uis={}", names(event),
                perUi);
    }

    @Override
    public void onHotswapComplete(HotswapCompleteEvent event) {
        if (!isActive()) {
            return;
        }
        completed = true;
        LOGGER.debug(
                "onHotswapComplete classes={} redefined={}", event.getClasses()
                        .stream().map(Class::getName).sorted().toList(),
                event.isRedefined());
    }

    /**
     * Clears recorded state before the next transaction.
     */
    public void reset() {
        completed = false;
        pageReloadRequired = false;
    }

    /**
     * Whether Flow reported the hotswap complete since the last
     * {@link #reset()}.
     *
     * @return {@code true} if {@code onHotswapComplete} was called
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Whether Flow decided a page reload is required for the changes seen since
     * the last {@link #reset()}.
     *
     * @return {@code true} if a page reload is required
     */
    public boolean isPageReloadRequired() {
        return pageReloadRequired;
    }

    private static String names(HotswapClassEvent event) {
        return event.getChangedClasses().stream().map(Class::getSimpleName)
                .sorted().collect(Collectors.toList()).toString();
    }
}

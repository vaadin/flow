/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */
package com.vaadin.flow.server.jrebel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.javarebel.ClassEventListener;
import org.zeroturnaround.javarebel.ReloaderFactory;
import org.zeroturnaround.javarebel.integration.generic.ClassEventListenerAdapter;
import org.zeroturnaround.javarebel.integration.util.WeakUtil;

/**
 * Initialize and maintain JRebel {@link ClassEventListener} which receives
 * class change events.
 */
public class JRebelInitializer implements VaadinServiceInitListener {

    static final int RELOAD_DELAY = 100;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        VaadinService vaadinService = event.getSource();

        ClassEventListener listener = new ClassEventListenerImpl(vaadinService);
        ReloaderFactory.getInstance()
                .addClassReloadListener(WeakUtil.weak(listener));

        VaadinContext vaadinContext = vaadinService.getContext();
        vaadinContext.setAttribute(new JRebelListenerReference(listener));

        getLogger().info("Started JRebel initializer");
    }

    static class JRebelListenerReference {

        final ClassEventListener listener;

        private JRebelListenerReference(ClassEventListener listener) {
            this.listener = listener;
        }
    }

    private static class ClassEventListenerImpl
            extends ClassEventListenerAdapter {

        private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                1);

        private ScheduledFuture<?> schedule;
        private ClassEventRunnable command;

        private Lock lock = new ReentrantLock(true);

        private VaadinService vaadinService;

        private ClassEventListenerImpl(VaadinService vaadinService) {
            super(0);

            this.vaadinService = vaadinService;
        }

        @Override
        public void onClassEvent(int eventType, Class<?> klass) {
            lock.lock();

            try {
                getLogger().info(
                        "JRebel class event with type {}, on class {}",
                        eventType, klass.getName());

                if (command != null) {
                    command.cancel.set(true);
                    schedule.cancel(false);
                }

                command = new ClassEventRunnable();
                schedule = executor.schedule(command, RELOAD_DELAY,
                        TimeUnit.MILLISECONDS);
            } finally {
                lock.unlock();
            }
        }

        private class ClassEventRunnable implements Runnable {

            private AtomicBoolean cancel = new AtomicBoolean(false);

            @Override
            public void run() {
                lock.lock();

                try {
                    if (cancel.get()) {
                        return;
                    }

                    BrowserLiveReloadAccess liveReloadAccess = vaadinService
                            .getInstantiator()
                            .getOrCreate(BrowserLiveReloadAccess.class);
                    BrowserLiveReload liveReload = liveReloadAccess
                            .getLiveReload(vaadinService);
                    if (liveReload != null) {
                        liveReload.reload();
                        getLogger().info("Browser reloaded.");
                    } else {
                        getLogger().warn("BrowserLiveReload not loaded.");
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JRebelInitializer.class.getName());
    }

}

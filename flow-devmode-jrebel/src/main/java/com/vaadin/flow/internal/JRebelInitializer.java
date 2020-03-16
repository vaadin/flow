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
 */
package com.vaadin.flow.internal;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import org.zeroturnaround.javarebel.ClassEventListener;
import org.zeroturnaround.javarebel.ReloaderFactory;
import org.zeroturnaround.javarebel.integration.generic.ClassEventListenerAdapter;
import org.zeroturnaround.javarebel.integration.util.WeakUtil;

/**
 * Initialize and maintain JRebel {@link ClassEventListener} which receives
 * class change events.
 */
public class JRebelInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx)
            throws ServletException {

        System.err.println("JRebel.onStartup");

        final VaadinContext vaadinContext = new VaadinServletContext(ctx);

        ClassEventListener listener = new ClassEventListenerImpl(vaadinContext);

        ReloaderFactory.getInstance()
                .addClassReloadListener(WeakUtil.weak(listener));

        vaadinContext.setAttribute(new JRebelListenerReference(listener));
    }

    private static class ClassEventListenerImpl
            extends ClassEventListenerAdapter {

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                1);

        ScheduledFuture<?> schedule;
        ClassEventRunnable command;

        Lock lock = new ReentrantLock(true);

        VaadinContext context;

        Set<Class<?>> addedClasses = new HashSet<>();
        Set<Class<?>> modifiedClasses = new HashSet<>();
        Set<Class<?>> deletedClasses = new HashSet<>();

        public ClassEventListenerImpl(VaadinContext context) {
            super(0);

            this.context = context;
        }

        @Override
        public void onClassEvent(int eventType, Class<?> klass)
                throws Exception {

            lock.lock();

            try {
                System.err.println("JRebel.onClassEvent " + eventType + " on "
                        + klass.getName());

                switch (eventType) {
                    case ClassEventListener.EVENT_LOADED:
                        addedClasses.add(klass);
                        break;
                    case ClassEventListener.EVENT_RELOADED:
                        modifiedClasses.add(klass);
                        break;

                    // TODO: EVENT_UNLOADED missing?
                }

                if (command != null) {
                    command.cancel.set(true);
                    schedule.cancel(false);
                }

                command = new ClassEventRunnable();
                schedule = executor.schedule(command, 100,
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

                    // TODO access VaadinServlet

                    final BrowserLiveReloadImpl liveReload = context.getAttribute(BrowserLiveReloadImpl.class);

                    liveReload.reload();

                    addedClasses.clear();
                    modifiedClasses.clear();
                    deletedClasses.clear();

                } finally {
                    lock.unlock();
                }
            }
        }

    }

    private static class JRebelListenerReference {

        private final ClassEventListener listener;

        public JRebelListenerReference(ClassEventListener listener) {
            this.listener = listener;
        }
    }
}

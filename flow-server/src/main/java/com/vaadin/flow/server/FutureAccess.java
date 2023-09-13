/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * Encapsulates a {@link Command} submitted using
 * {@link VaadinSession#access(Command)}. This class is used internally by the
 * framework and is not intended to be directly used by application developers.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class FutureAccess extends FutureTask<Void> {
    private final VaadinSession session;
    private final Command command;
    private final Iterable<VaadinCommandInterceptor> interceptors;
    private final Map<Object, Object> context = new HashMap<>(); // TODO: ConcurrentHashMap?

    /**
     * Creates an instance for the given command.
     *
     * @param session
     *            the session to which the task belongs
     * @param command
     *            the command to run when this task is purged from the queue
     */
    public FutureAccess(Iterable<VaadinCommandInterceptor> interceptors, VaadinSession session, Command command) {
        super(command::execute, null);
        this.session = session;
        this.command = command;
        this.interceptors = interceptors;
    }

    @Override
    public void run() {
        this.interceptors.forEach(interceptor -> interceptor.commandExecutionStart(context, command));
        super.run();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        /*
         * Help the developer avoid programming patterns that cause deadlocks
         * unless implemented very carefully. get(long, TimeUnit) does not have
         * the same detection since a sensible timeout should avoid completely
         * locking up the application.
         *
         * Even though no deadlock could occur after the command has been run,
         * the check is always done as the deterministic behavior makes it
         * easier to detect potential problems.
         */
        VaadinService.verifyNoOtherSessionLocked(session);
        Void unused = super.get();
        interceptors.forEach(interceptor -> interceptor.commandExecutionEnd(context, command));
        return unused;
    }

    /**
     * Handles exceptions thrown during the execution of this task.
     *
     * @param exception
     *            the thrown exception.
     */
    public void handleError(Exception exception) {
        try {
            interceptors.forEach(interceptor -> interceptor.handleException(context, command, exception));
            if (command instanceof ErrorHandlingCommand) {
                ErrorHandlingCommand errorHandlingCommand = (ErrorHandlingCommand) command;

                errorHandlingCommand.handleError(exception);
            } else {
                ErrorEvent errorEvent = new ErrorEvent(exception);

                ErrorHandler errorHandler = ErrorEvent
                        .findErrorHandler(session);

                if (errorHandler == null) {
                    errorHandler = new DefaultErrorHandler();
                }

                errorHandler.error(errorEvent);
            }
        } catch (Exception e) {
            getLogger().error(e.getMessage(), e);
        } finally {
            interceptors.forEach(interceptor -> interceptor.commandExecutionEnd(context, command));
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FutureAccess.class.getName());
    }
}

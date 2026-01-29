package com.vaadin.flow.server;

import java.util.concurrent.FutureTask;

/**
 * Represents an abstract base class for tasks wrapped in a {@link FutureTask}.
 * This class enables submitting tasks for asynchronous execution and provides
 * a mechanism to handle exceptions raised during task execution.
 *
 * @see FutureTask
 */
public abstract class AbstractFutureAccess extends FutureTask<Void> {

	/**
	 * Constructs an instance of {@code AbstractFutureAccess}.
	 *
	 * @param runnable the {@code Runnable} task to be executed asynchronously.
	 *                 This task will be wrapped in a {@link FutureTask}.
	 * @param result   the result to return upon the completion of the task. It is
	 *                 typically {@code null} since the task's result is not used.
	 */
	public AbstractFutureAccess(Runnable runnable, Void result) {
		super(runnable, result);
	}

	/**‚
	 * Handles an exception that occurs during the execution of a task.
	 *
	 * @param exception the exception that was thrown during task execution
	 */
	public abstract void handleError(Exception exception);
}

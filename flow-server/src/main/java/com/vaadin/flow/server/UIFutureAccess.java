package com.vaadin.flow.server;

import com.vaadin.flow.component.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class UIFutureAccess extends AbstractFutureAccess{

	private final UI ui;
	private final Command command;

	/**
	 * Creates an instance for the given command.
	 *
	 * @param ui
	 *            the UI to which the task belongs
	 * @param command
	 *            the command to run when this task is purged from the queue
	 */
	public UIFutureAccess(UI ui, Command command) {
		super(command::execute, null);
		this.ui = ui;
		this.command = command;
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
		ui.checkHasLock();
		return super.get();
	}

	/**
	 * Handles exceptions thrown during the execution of this task.
	 *
	 * @param exception
	 *            the thrown exception.
	 */
	public void handleError(Exception exception) {
		try {
			if (command instanceof ErrorHandlingCommand) {
				ErrorHandlingCommand errorHandlingCommand = (ErrorHandlingCommand) command;

				errorHandlingCommand.handleError(exception);
			} else {
				ErrorEvent errorEvent = new ErrorEvent(exception);

				ErrorHandler errorHandler = ErrorEvent
						.findErrorHandler(ui.getSession());

				if (errorHandler == null) {
					errorHandler = new DefaultErrorHandler();
				}

				errorHandler.error(errorEvent);
			}
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		}
	}

	private static Logger getLogger() {
		return LoggerFactory.getLogger(FutureAccess.class.getName());
	}
}

package com.vaadin.flow.server;

import java.util.concurrent.FutureTask;

public abstract class AbstractFutureAccess extends FutureTask<Void> {

	public AbstractFutureAccess(Runnable runnable, Void result) {
		super(runnable, result);
	}

	public abstract void handleError(Exception exception);
}

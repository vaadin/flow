package com.vaadin.flow.server;

import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public interface AccessSupport {

	Lock getLockInstance();

	void lock();

	void unlock();

	void checkHasLock();

	void checkHasLock(String message);

	void runPendingAccessTasks();

	void ensureAccessQueuePurged();

	Future<Void> access(Command command);

	void accessSynchronously(Command command);

	Queue<AbstractFutureAccess> getPendingAccessQueue();

	long getLastLocked();

	long getLastUnlocked();

	default boolean hasLock() {
		ReentrantLock l = ((ReentrantLock) getLockInstance());
		return l.isHeldByCurrentThread();
	}

}

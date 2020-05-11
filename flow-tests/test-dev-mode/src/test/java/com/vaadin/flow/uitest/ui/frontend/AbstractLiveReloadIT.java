package com.vaadin.flow.uitest.ui.frontend;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;

import com.vaadin.flow.testutil.ChromeBrowserTest;

// These tests are not parallelizable, nor should they be run at the same time
// as other tests in the same module, due to live-reload affecting the whole
// application
public abstract class AbstractLiveReloadIT extends ChromeBrowserTest {
    private static final Lock lock = new ReentrantLock();

    @Before
    @Override
    public void setup() throws Exception {
        lock.lock();
        super.setup();
    }

    @After
    public void tearDown() {
        lock.unlock();
    }
}

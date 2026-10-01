package com.vaadin.base.devserver;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class FileWatcherTest {

    @Test
    public void fileWatcherTriggeredForModification() throws Exception {
        AtomicReference<File> changed = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        File dir = Files.createTempDirectory("watched").toFile();
        FileWatcher watcher = new FileWatcher(file -> {
            changed.set(file);
            latch.countDown();
        }, dir);

        watcher.start();

        try {
            File newFile = new File(dir, "newFile.txt");
            newFile.createNewFile();

            // The watcher is supposed to be triggered immediately, but a
            // loaded machine can deliver the event later
            Assert.assertTrue("The watcher was not triggered",
                    latch.await(10, TimeUnit.SECONDS));
            Assert.assertEquals(newFile, changed.get());
        } finally {
            watcher.stop();
        }
    }
}

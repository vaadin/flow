package com.vaadin.base.devserver;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class FileWatcherTest {

    @Test
    public void fileWatcherTriggeredForModification() throws Exception {
        AtomicReference<File> changed = new AtomicReference<>();

        File dir = Files.createTempDirectory("watched").toFile();
        FileWatcher watcher = new FileWatcher(file -> {
            changed.set(file);
        }, dir);

        watcher.start();

        try {
            File newFile = new File(dir, "newFile.txt");
            newFile.createNewFile();

            // The watcher is supposed to be triggered immediately, but a
            // loaded machine can deliver the event later
            long deadline = System.currentTimeMillis() + 10000;
            while (changed.get() == null
                    && System.currentTimeMillis() < deadline) {
                Thread.sleep(50);
            }
            Assert.assertEquals(newFile, changed.get());
        } finally {
            watcher.stop();
        }
    }
}

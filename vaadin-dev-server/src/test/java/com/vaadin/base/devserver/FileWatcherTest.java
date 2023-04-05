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

        File newFile = new File(dir, "newFile.txt");
        newFile.createNewFile();

        Thread.sleep(50); // The watcher is supposed to be triggered immediately
        Assert.assertEquals(newFile, changed.get());
    }
}

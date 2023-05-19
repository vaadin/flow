package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public static void triggerReload() {
        try {
            String classFile = Application.class.getName().replace(".", "/")
                    + ".class";
            touch(new File("target/classes/" + classFile));
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }

    }

    private static void touch(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        file.setLastModified(System.currentTimeMillis());
    }

}

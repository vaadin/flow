package com.vaadin.gradle;

import org.gradle.api.Transformer;

import java.io.File;

public class FileExistsFilter implements Transformer<File, File> {
    @Override
    public File transform(File file) {
        // workaround for https://github.com/gradle/gradle/issues/12388 - because of this bug
        // we can't implement this in Kotlin.
        return file.exists() ? file : null;
    }
}

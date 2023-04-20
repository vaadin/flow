package com.vaadin.flow.server.frontend;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;

public class TaskCopyBundleFiles implements FallibleCommand {

    private final Options options;

    public TaskCopyBundleFiles(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        URL statsJson = BundleValidationUtil
                .getProdBundleResource("config/stats.json");
        if (statsJson == null) {
            throw new IllegalStateException(
                    "Could not copy production bundle files, because couldn't find production bundle in the class-path");
        }
        String pathToJar = statsJson.getPath();
        int index = pathToJar.lastIndexOf(".jar!/");
        if (index >= 0) {
            // exclude relative path starting from !/
            pathToJar = pathToJar.substring(0, index + 4);
        }
        try {
            URI jarUri = new URI(pathToJar);
            JarContentsManager jarContentsManager = new JarContentsManager();
            jarContentsManager.copyIncludedFilesFromJarTrimmingBasePath(
                    new File(jarUri), Constants.PROD_BUNDLE_NAME,
                    options.getResourceOutputDirectory(), "**/*.*");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}

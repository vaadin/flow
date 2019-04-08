package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeExecutorConfig;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;

import com.vaadin.flow.plugin.common.FrontendToolsLocator;

// TODO kb either move to a separate mojo or make it a part of the common abstract NPM class
public class TestVerificationClass {

    public static void main(String[] args) {
        FrontendToolsLocator toolsLocator = new FrontendToolsLocator();
        Optional<File> nodeLocation = toolsLocator.tryLocateTool("node");
        Optional<File> npmLocation = toolsLocator.tryLocateTool("npm");

        if (!nodeLocation.isPresent() || !npmLocation.isPresent()) {
            System.out.println("Installing local node");
            NodeExecutorConfig nodeExecutorConfig = installLocalNode();
            File nodePath = nodeExecutorConfig.getNodePath();
            System.out.println(nodePath);
            File npmPath = nodeExecutorConfig.getNpmPath();
            System.out.println(npmPath);
        }
    }

    private static NodeExecutorConfig installLocalNode() {
        File workingDirectory = new File("/Users/someonetoignore/Downloads");
        FrontendPluginFactory factory = new FrontendPluginFactory(
                workingDirectory, workingDirectory);
        String nodeVersion = "v8.15.1";
        ProxyConfig proxyConfig = new ProxyConfig(Collections.emptyList());

        try {
            factory.getNodeInstaller(proxyConfig).setNodeVersion(nodeVersion)
                    .setNpmVersion("provided")
                    .setNodeDownloadRoot(
                            NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)
                    .install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to download node", e);
        }

        try {
            Method getExecutorConfig = factory.getClass()
                    .getDeclaredMethod("getExecutorConfig");
            getExecutorConfig.setAccessible(true);
            return (NodeExecutorConfig) getExecutorConfig.invoke(factory);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }

    }
}

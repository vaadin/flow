package com.vaadin.flow.plugin.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendToolsSettings;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.polymer2lit.FrontendConverter;
import com.vaadin.polymer2lit.ServerConverter;

public class ConvertPolymerExecutor implements AutoCloseable {
    private PluginAdapterBase adapter;

    private String frontendGlob;

    private String serverGlob;

    private boolean useLit1;

    private boolean disableOptionalChaining;

    private FrontendConverter frontendConverter;

    private ServerConverter serverConverter;

    public ConvertPolymerExecutor(PluginAdapterBase adapter, String serverGlob,
            String frontendGlob, boolean useLit1,
            boolean disableOptionalChaining)
            throws URISyntaxException, IOException {
        this.adapter = adapter;
        this.serverGlob = serverGlob;
        this.frontendGlob = frontendGlob;
        this.useLit1 = useLit1;
        this.disableOptionalChaining = disableOptionalChaining;

        this.serverConverter = new ServerConverter();

        FrontendTools tools = new FrontendTools(
                getFrontendToolsSettings(adapter));
        this.frontendConverter = new FrontendConverter(tools);
    }

    @Override
    public void close() throws IOException {
        this.frontendConverter.close();
    }

    public void execute() throws IOException, InterruptedException {
        adapter.logInfo(
                "Collecting server files by the glob " + serverGlob + "...");

        for (Path filePath : getFilePathsByGlob(adapter.projectBaseDirectory(),
                serverGlob)) {
            adapter.logInfo("Processing " + filePath.toString() + "...");

            serverConverter.convertFile(filePath);
        }

        adapter.logInfo("Collecting frontend files by the glob " + frontendGlob
                + "...");

        for (Path filePath : getFilePathsByGlob(adapter.projectBaseDirectory(),
                frontendGlob)) {
            adapter.logInfo("Processing " + filePath.toString() + "...");

            frontendConverter.convertFile(filePath, useLit1,
                    disableOptionalChaining);
        }
    }

    private List<Path> getFilePathsByGlob(Path baseDir, String glob)
            throws IOException {
        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(baseDir)) {
            return walk.filter(path -> matcher.matches(path))
                    .filter(path -> !path.toString().contains("node_modules"))
                    .collect(Collectors.toList());
        }
    }

    private FrontendToolsSettings getFrontendToolsSettings(
            PluginAdapterBase adapter) throws URISyntaxException {
        FrontendToolsSettings settings = new FrontendToolsSettings(
                adapter.npmFolder().getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setNodeDownloadRoot(adapter.nodeDownloadRoot());
        settings.setNodeVersion(adapter.nodeVersion());
        settings.setAutoUpdate(adapter.nodeAutoUpdate());
        settings.setUseGlobalPnpm(adapter.useGlobalPnpm());
        settings.setForceAlternativeNode(adapter.requireHomeNodeExec());
        return settings;
    }
}

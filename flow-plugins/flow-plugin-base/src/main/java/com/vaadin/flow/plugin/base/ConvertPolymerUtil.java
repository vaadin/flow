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

public class ConvertPolymerUtil {

    public static void convertFrontend(PluginAdapterBase adapter, String glob,
            boolean useLit1, boolean disableOptionalChaining)
            throws IOException, InterruptedException, URISyntaxException {
        adapter.logInfo("Collecting frontend files to convert...");

        FrontendToolsSettings settings = getFrontendToolsSettings(adapter);
        FrontendTools tools = new FrontendTools(settings);

        try (FrontendConverter converter = new FrontendConverter(tools)) {
            for (Path filePath : getFilePathsByGlob(
                    adapter.projectBaseDirectory(), glob)) {
                adapter.logInfo("Processing " + filePath.toString() + "...");

                converter.convertFile(filePath, useLit1,
                        disableOptionalChaining);
            }
        }
    }

    public static void convertServer(PluginAdapterBase adapter, String glob)
            throws IOException {
        adapter.logInfo("Collecting server files to convert...");

        for (Path filePath : getFilePathsByGlob(adapter.projectBaseDirectory(),
                glob)) {
            adapter.logInfo("Processing " + filePath.toString() + "...");

            ServerConverter.convertFile(filePath);
        }
    }

    private static List<Path> getFilePathsByGlob(Path baseDir, String glob)
            throws IOException {
        PathMatcher matcher = FileSystems.getDefault()
                .getPathMatcher("glob:" + glob);

        try (Stream<Path> walk = Files.walk(baseDir)) {
            return walk.filter(path -> matcher.matches(path))
                    .filter(path -> !path.toString().contains("node_modules"))
                    .collect(Collectors.toList());
        }
    }

    private static FrontendToolsSettings getFrontendToolsSettings(
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

/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.plugin.maven;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import com.vaadin.flow.plugin.common.AnnotationValuesExtractor;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.plugin.common.FrontendDataProvider;
import com.vaadin.flow.plugin.common.FrontendToolsManager;
import com.vaadin.flow.plugin.production.TranspilationStep;

/**
 * Goal that prepares all web files from
 * {@link PackageFormProductionMojo#transpileEs6SourceDirectory} for production
 * mode: minifies, transpiles and bundles them.
 */
@Mojo(name = "package-for-production", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class PackageFormProductionMojo extends AbstractMojo {
    @Parameter(name = "transpileEs6SourceDirectory", defaultValue = "${project.build.directory}/frontend/", required = true)
    private File transpileEs6SourceDirectory;

    @Parameter(name = "transpileWorkingDirectory", defaultValue = "${project.build.directory}/", required = true)
    private File transpileWorkingDirectory;

    @Parameter(name = "transpileOutputDirectory", defaultValue = "${project.build.directory}/${project.build.finalName}/", required = true)
    private File transpileOutputDirectory;

    @Parameter(name = "es6OutputDirectoryName", defaultValue = "frontend-es6", required = true)
    private String es6OutputDirectoryName;

    @Parameter(name = "es5OutputDirectoryName", defaultValue = "frontend-es5", required = true)
    private String es5OutputDirectoryName;

    @Parameter(property = "skipEs5", defaultValue = "false", required = true)
    private boolean skipEs5;

    @Parameter
    private List<Fragment> fragments;

    @Parameter(property = "bundle", defaultValue = "true", required = true)
    private boolean bundle;

    @Parameter(property = "bundleConfiguration", defaultValue = "${project.basedir}/bundle-configuration.json")
    private File bundleConfiguration;

    @Parameter(name = "nodeVersion", defaultValue = "v8.9.0", required = true)
    private String nodeVersion;

    @Parameter(name = "yarnVersion", defaultValue = "v1.3.2", required = true)
    private String yarnVersion;

    @Parameter(property = "ignoreMavenProxies", defaultValue = "true", required = true)
    private boolean ignoreMavenProxies;

    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        FrontendDataProvider frontendDataProvider = new FrontendDataProvider(
                bundle, transpileEs6SourceDirectory, new AnnotationValuesExtractor(getProjectClassPathUrls()), bundleConfiguration, getFragmentsData(fragments));
        FrontendToolsManager frontendToolsManager = new FrontendToolsManager(
                transpileWorkingDirectory, es5OutputDirectoryName, es6OutputDirectoryName, frontendDataProvider);
        new TranspilationStep(frontendToolsManager, getProxyConfig(), nodeVersion, yarnVersion).transpileFiles(transpileEs6SourceDirectory, transpileOutputDirectory, skipEs5);
    }

    private Map<String, Set<String>> getFragmentsData(List<Fragment> mavenFragments) {
        return Optional.ofNullable(mavenFragments).orElse(Collections.emptyList()).stream()
                .peek(this::verifyFragment)
                .collect(Collectors.toMap(Fragment::getName, Fragment::getFiles));
    }

    private void verifyFragment(Fragment fragment) {
        if (fragment.getName() == null || fragment.getFiles() == null || fragment.getFiles().isEmpty()) {
            throw new IllegalArgumentException(String.format("Each fragment definition should have a name and list of files to include defined. Got incorrect definition: '%s'", fragment));
        }
    }

    private URL[] getProjectClassPathUrls() {
        final List<String> runtimeClasspathElements;
        try {
            runtimeClasspathElements = project.getRuntimeClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format("Failed to retrieve runtime classpath elements from project '%s'", project), e);
        }
        return runtimeClasspathElements.stream()
                .map(File::new)
                .map(FlowPluginFileUtils::convertToUrl)
                .toArray(URL[]::new);
    }

    private ProxyConfig getProxyConfig() {
        if (ignoreMavenProxies) {
            return new ProxyConfig(Collections.emptyList());
        }
        return new ProxyConfig(getMavenProxies().stream()
                .filter(Proxy::isActive)
                .map(proxy -> decrypter.decrypt(new DefaultSettingsDecryptionRequest(proxy)))
                .map(SettingsDecryptionResult::getProxy)
                .map(this::createProxy)
                .collect(Collectors.toList()));
    }

    private List<Proxy> getMavenProxies() {
        if (session == null || session.getSettings() == null
                || session.getSettings().getProxies() == null || session.getSettings().getProxies().isEmpty()) {
            return Collections.emptyList();
        }
        return session.getSettings().getProxies();
    }

    private ProxyConfig.Proxy createProxy(Proxy proxy) {
        return new ProxyConfig.Proxy(
                proxy.getId(),
                proxy.getProtocol(),
                proxy.getHost(),
                proxy.getPort(),
                proxy.getUsername(),
                proxy.getPassword(),
                proxy.getNonProxyHosts());
    }
}

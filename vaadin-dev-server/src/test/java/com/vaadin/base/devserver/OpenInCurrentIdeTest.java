package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessHandle.Info;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenInCurrentIdeTest {

    @Test
    public void vsCodeOnMacDetected() {
        String cmd1 = "/opt/homebrew/Cellar/openjdk@11/11.0.16.1_1/libexec/openjdk.jdk/Contents/Home/bin/java";
        String[] args1 = new String[] {
                "@/var/folders/3r/3_0g1bhn44j1vvvpfksrz1z40000gn/T/cp_etuvzkvp19t6wovy4ilfmnb6l.argfile",
                "com.example.application.Application", };

        String cmd2 = "/bin/zsh";
        String[] args2 = new String[] { "-l", };
        String cmd3 = "/Applications/Visual Studio Code.app/Contents/Frameworks/Code Helper (Renderer).app/Contents/MacOS/Code Helper (Renderer)";
        String[] args3 = new String[] { "--ms-enable-electron-run-as-node",
                "/Applications/Visual Studio Code.app/Contents/Resources/app/out/bootstrap-fork",
                "--type=ptyHost", "--logsPath",
                "/Users/artur/Library/Application Support/Code/logs/20221225T212012", };

        String cmd4 = "/Applications/Visual Studio Code.app/Contents/Frameworks/Code Helper (Renderer).app/Contents/MacOS/Code Helper (Renderer)";
        String[] args4 = new String[] { "--type=renderer",
                "--user-data-dir=/Users/artur/Library/Application Support/Code",
                "--standard-schemes=vscode-webview,vscode-file",
                "--secure-schemes=vscode-webview,vscode-file",
                "--bypasscsp-schemes",
                "--cors-schemes=vscode-webview,vscode-file",
                "--fetch-schemes=vscode-webview,vscode-file",
                "--service-worker-schemes=vscode-webview",
                "--streaming-schemes",
                "--app-path=/Applications/Visual Studio Code.app/Contents/Resources/app",
                "--no-sandbox", "--no-zygote", "--node-integration-in-worker",
                "--lang=en-GB", "--num-raster-threads=4", "--enable-zero-copy",
                "--enable-gpu-memory-buffer-compositor-resources",
                "--enable-main-frame-before-activation",
                "--renderer-client-id=20", "--launch-time-ticks=444960654929",
                "--shared-files",
                "--field-trial-handle=1718379636,r,11433658063312687633,15858774526786421505,131072",
                "--enable-features=AutoDisableAccessibility",
                "--disable-features=CalculateNativeWinOcclusion,SpareRendererForSitePerProcess",
                "--vscode-window-config=vscode:0d71a071-dced-4d7f-b3aa-1a4862b8d021",
                "--vscode-window-kind=shared-process", };
        String cmd5 = "/Applications/Visual Studio Code.app/Contents/MacOS/Electron";
        String[] args5 = new String[] {};

        List<Info> processes = new ArrayList<>();
        processes.add(mock(cmd1, args1));
        processes.add(mock(cmd2, args2));
        processes.add(mock(cmd3, args3));
        processes.add(mock(cmd4, args4));
        processes.add(mock(cmd5, args5));

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertFalse(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertTrue(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isEclipse(ideCommand.get()));

        // THe binary is not used for VSCode
        // Assert.assertEquals("",
        // OpenInCurrentIde.getBinary(ideCommand.get()));

    }

    @Test
    public void eclipseOnMacDetected() {
        String[] arguments = new String[] { "-Dfile.encoding=UTF-8",
                "-classpath",
                "/home/sombody/.m2/repository/com/vaadin/vaadin/24.0-SNAPSHOT/vaadin-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-core/24.0-SNAPSHOT/vaadin-core-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-server/24.0-SNAPSHOT/flow-server-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/servletdetector/throw-if-servlet3/1.0.2/throw-if-servlet3-1.0.2.jar:/home/sombody/.m2/repository/com/vaadin/flow-commons-upload/24.0-SNAPSHOT/flow-commons-upload-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.1/jackson-core-2.14.1.jar:/home/sombody/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar:/home/sombody/.m2/repository/com/helger/ph-css/6.5.0/ph-css-6.5.0.jar:/home/sombody/.m2/repository/com/helger/commons/ph-commons/10.1.6/ph-commons-10.1.6.jar:/home/sombody/.m2/repository/com/vaadin/external/gentyref/1.2.0.vaadin1/gentyref-1.2.0.vaadin1.jar:/home/sombody/.m2/repository/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar:/home/sombody/.m2/repository/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar:/home/sombody/.m2/repository/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-dev-server/24.0-SNAPSHOT/vaadin-dev-server-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/sombody/.m2/repository/com/vaadin/flow-lit-template/24.0-SNAPSHOT/flow-lit-template-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-push/24.0-SNAPSHOT/flow-push-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.0.slf4jvaadin2/atmosphere-runtime-3.0.0.slf4jvaadin2.jar:/home/sombody/.m2/repository/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar:/home/sombody/.m2/repository/com/vaadin/flow-client/24.0-SNAPSHOT/flow-client-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-html-components/24.0-SNAPSHOT/flow-html-components-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-data/24.0-SNAPSHOT/flow-data-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-dnd/24.0-SNAPSHOT/flow-dnd-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/org/webjars/npm/vaadin__vaadin-mobile-drag-drop/1.0.1/vaadin__vaadin-mobile-drag-drop-1.0.1.jar:/home/sombody/.m2/repository/org/webjars/npm/mobile-drag-drop/2.3.0-rc.2/mobile-drag-drop-2.3.0-rc.2.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-lumo-theme/24.0-SNAPSHOT/vaadin-lumo-theme-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-material-theme/24.0-SNAPSHOT/vaadin-material-theme-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-accordion-flow/24.0-SNAPSHOT/vaadin-accordion-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-avatar-flow/24.0-SNAPSHOT/vaadin-avatar-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-button-flow/24.0-SNAPSHOT/vaadin-button-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-checkbox-flow/24.0-SNAPSHOT/vaadin-checkbox-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-combo-box-flow/24.0-SNAPSHOT/vaadin-combo-box-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-confirm-dialog-flow/24.0-SNAPSHOT/vaadin-confirm-dialog-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-custom-field-flow/24.0-SNAPSHOT/vaadin-custom-field-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-date-picker-flow/24.0-SNAPSHOT/vaadin-date-picker-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-date-time-picker-flow/24.0-SNAPSHOT/vaadin-date-time-picker-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-details-flow/24.0-SNAPSHOT/vaadin-details-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-time-picker-flow/24.0-SNAPSHOT/vaadin-time-picker-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-select-flow/24.0-SNAPSHOT/vaadin-select-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-dialog-flow/24.0-SNAPSHOT/vaadin-dialog-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-form-layout-flow/24.0-SNAPSHOT/vaadin-form-layout-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-field-highlighter-flow/24.0-SNAPSHOT/vaadin-field-highlighter-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-grid-flow/24.0-SNAPSHOT/vaadin-grid-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-icons-flow/24.0-SNAPSHOT/vaadin-icons-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-virtual-list-flow/24.0-SNAPSHOT/vaadin-virtual-list-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-list-box-flow/24.0-SNAPSHOT/vaadin-list-box-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-login-flow/24.0-SNAPSHOT/vaadin-login-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-messages-flow/24.0-SNAPSHOT/vaadin-messages-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-ordered-layout-flow/24.0-SNAPSHOT/vaadin-ordered-layout-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-progress-bar-flow/24.0-SNAPSHOT/vaadin-progress-bar-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-radio-button-flow/24.0-SNAPSHOT/vaadin-radio-button-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-renderer-flow/24.0-SNAPSHOT/vaadin-renderer-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-split-layout-flow/24.0-SNAPSHOT/vaadin-split-layout-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-tabs-flow/24.0-SNAPSHOT/vaadin-tabs-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-text-field-flow/24.0-SNAPSHOT/vaadin-text-field-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-upload-flow/24.0-SNAPSHOT/vaadin-upload-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-notification-flow/24.0-SNAPSHOT/vaadin-notification-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-app-layout-flow/24.0-SNAPSHOT/vaadin-app-layout-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-context-menu-flow/24.0-SNAPSHOT/vaadin-context-menu-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-menu-bar-flow/24.0-SNAPSHOT/vaadin-menu-bar-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-board-flow/24.0-SNAPSHOT/vaadin-board-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-charts-flow/24.0-SNAPSHOT/vaadin-charts-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-cookie-consent-flow/24.0-SNAPSHOT/vaadin-cookie-consent-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-crud-flow/24.0-SNAPSHOT/vaadin-crud-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-flow-components-base/24.0-SNAPSHOT/vaadin-flow-components-base-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-grid-pro-flow/24.0-SNAPSHOT/vaadin-grid-pro-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-map-flow/24.0-SNAPSHOT/vaadin-map-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-rich-text-editor-flow/24.0-SNAPSHOT/vaadin-rich-text-editor-flow-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/collaboration-engine/6.0-SNAPSHOT/collaboration-engine-6.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.14.1/jackson-databind-2.14.1.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.14.1/jackson-annotations-2.14.1.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.14.1/jackson-datatype-jsr310-2.14.1.jar:/home/sombody/.m2/repository/com/vaadin/license-checker/1.11-SNAPSHOT/license-checker-1.11-SNAPSHOT.jar:/home/sombody/.m2/repository/com/github/oshi/oshi-core/6.1.6/oshi-core-6.1.6.jar:/home/sombody/.m2/repository/net/java/dev/jna/jna-platform/5.11.0/jna-platform-5.11.0.jar:/home/sombody/.m2/repository/com/nimbusds/nimbus-jose-jwt/9.23/nimbus-jose-jwt-9.23.jar:/home/sombody/.m2/repository/org/lucee/jcip-annotations/1.0.0/jcip-annotations-1.0.0.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-dev-bundle/24.0-SNAPSHOT/vaadin-dev-bundle-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-spring-boot-starter/24.0-SNAPSHOT/vaadin-spring-boot-starter-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-spring/24.0-SNAPSHOT/vaadin-spring-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/org/springframework/spring-webmvc/6.0.2/spring-webmvc-6.0.2.jar:/home/sombody/.m2/repository/org/springframework/spring-aop/6.0.2/spring-aop-6.0.2.jar:/home/sombody/.m2/repository/org/springframework/spring-beans/6.0.2/spring-beans-6.0.2.jar:/home/sombody/.m2/repository/org/springframework/spring-expression/6.0.2/spring-expression-6.0.2.jar:/home/sombody/.m2/repository/org/springframework/spring-websocket/6.0.2/spring-websocket-6.0.2.jar:/home/sombody/.m2/repository/org/reflections/reflections/0.10.2/reflections-0.10.2.jar:/home/sombody/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-starter-web/3.0.0/spring-boot-starter-web-3.0.0.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-starter-json/3.0.0/spring-boot-starter-json-3.0.0.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.14.1/jackson-datatype-jdk8-2.14.1.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/module/jackson-module-parameter-names/2.14.1/jackson-module-parameter-names-2.14.1.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-starter-tomcat/3.0.0/spring-boot-starter-tomcat-3.0.0.jar:/home/sombody/.m2/repository/org/apache/tomcat/embed/tomcat-embed-core/10.1.1/tomcat-embed-core-10.1.1.jar:/home/sombody/.m2/repository/org/apache/tomcat/embed/tomcat-embed-websocket/10.1.1/tomcat-embed-websocket-10.1.1.jar:/home/sombody/.m2/repository/org/springframework/spring-web/6.0.2/spring-web-6.0.2.jar:/home/sombody/.m2/repository/io/micrometer/micrometer-observation/1.10.2/micrometer-observation-1.10.2.jar:/home/sombody/.m2/repository/io/micrometer/micrometer-commons/1.10.2/micrometer-commons-1.10.2.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-starter-validation/3.0.0/spring-boot-starter-validation-3.0.0.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-starter/3.0.0/spring-boot-starter-3.0.0.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-starter-logging/3.0.0/spring-boot-starter-logging-3.0.0.jar:/home/sombody/.m2/repository/ch/qos/logback/logback-classic/1.4.5/logback-classic-1.4.5.jar:/home/sombody/.m2/repository/ch/qos/logback/logback-core/1.4.5/logback-core-1.4.5.jar:/home/sombody/.m2/repository/org/apache/logging/log4j/log4j-to-slf4j/2.19.0/log4j-to-slf4j-2.19.0.jar:/home/sombody/.m2/repository/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar:/home/sombody/.m2/repository/org/slf4j/jul-to-slf4j/2.0.4/jul-to-slf4j-2.0.4.jar:/home/sombody/.m2/repository/jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1.jar:/home/sombody/.m2/repository/org/yaml/snakeyaml/1.33/snakeyaml-1.33.jar:/home/sombody/.m2/repository/org/apache/tomcat/embed/tomcat-embed-el/10.1.1/tomcat-embed-el-10.1.1.jar:/home/sombody/.m2/repository/org/hibernate/validator/hibernate-validator/8.0.0.Final/hibernate-validator-8.0.0.Final.jar:/home/sombody/.m2/repository/jakarta/validation/jakarta.validation-api/3.0.2/jakarta.validation-api-3.0.2.jar:/home/sombody/.m2/repository/org/jboss/logging/jboss-logging/3.5.0.Final/jboss-logging-3.5.0.Final.jar:/home/sombody/.m2/repository/com/fasterxml/classmate/1.5.1/classmate-1.5.1.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-devtools/3.0.0/spring-boot-devtools-3.0.0.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot/3.0.0/spring-boot-3.0.0.jar:/home/sombody/.m2/repository/org/springframework/spring-context/6.0.2/spring-context-6.0.2.jar:/home/sombody/.m2/repository/org/springframework/boot/spring-boot-autoconfigure/3.0.0/spring-boot-autoconfigure-3.0.0.jar:/home/sombody/.m2/repository/net/bytebuddy/byte-buddy/1.12.19/byte-buddy-1.12.19.jar:/home/sombody/.m2/repository/org/springframework/spring-core/6.0.2/spring-core-6.0.2.jar:/home/sombody/.m2/repository/org/springframework/spring-jcl/6.0.2/spring-jcl-6.0.2.jar:/home/sombody/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:/home/sombody/.m2/repository/org/javassist/javassist/3.28.0-GA/javassist-3.28.0-GA.jar:/home/sombody/.m2/repository/com/vaadin/external/gwt/gwt-elemental/2.8.2.vaadin2/gwt-elemental-2.8.2.vaadin2.jar:/home/sombody/.m2/repository/org/slf4j/slf4j-api/2.0.4/slf4j-api-2.0.4.jar:/home/sombody/.m2/repository/net/java/dev/jna/jna/5.11.0/jna-5.11.0.jar:/home/sombody/.m2/repository/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar",
                "-XX:+ShowCodeDetailsInExceptionMessages",
                "com.vaadin.base.devserver.OpenInCurrentIde" };
        Info info1 = mock(
                "/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home/bin/java",
                arguments);

        String[] arguments2 = new String[] { "-data",
                "file:/some/folder/to/workspace/", "-os", "macosx", "-ws",
                "cocoa", "-arch", "aarch64", "-showsplash",
                "/home/somebody/.p2/pool/plugins/org.eclipse.epp.package.common_4.25.0.20220908-1200/splash.bmp",
                "-launcher",
                "/eclipse/install/dir/Eclipse.app/Contents/MacOS/eclipse",
                "-name", "Eclipse", "--launcher.library",
                "/home/somebody/.p2/pool/plugins/org.eclipse.equinox.launcher.cocoa.macosx.aarch64_1.2.600.v20220720-1916/eclipse_11700.so",
                "-startup",
                "/eclipse/install/dir/Eclipse.app/Contents/MacOS//../Eclipse/plugins/org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar",
                "--launcher.appendVmargs", "-product",
                "org.eclipse.epp.package.jee.product", "-keyring",
                "/home/somebody/.eclipse_keyring", "-vm",
                "/Library/Java/JavaVirtualMachines/jdk-17.0.1.jdk/Contents/Home/bin/../lib/server/libjvm.dylib",
                "-vmargs", "-Dosgi.requiredJavaVersion=17",
                "-Dosgi.instance.area.default=@user.home/eclipse-workspace",
                "-Dsun.java.command=Eclipse", "-XX:+UseG1GC",
                "-XX:+UseStringDeduplication", "--add-modules=ALL-SYSTEM",
                "-XstartOnFirstThread",
                "-Dorg.eclipse.swt.internal.carbon.smallFonts",
                "-Dosgi.requiredJavaVersion=11",
                "-Dosgi.dataAreaRequiresExplicitInit=true",
                "-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true",
                "-Xms256m", "-Xmx2048m", "--add-modules=ALL-SYSTEM",
                "-Xdock:icon=../Resources/Eclipse.icns", "-XstartOnFirstThread",
                "-Dorg.eclipse.swt.internal.carbon.smallFonts",
                "-Declipse.p2.max.threads=10",
                "-Doomph.update.url=https://download.eclipse.org/oomph/updates/milestone/latest",
                "-Doomph.redirection.index.redirection=index:/->http://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/" };

        Info info2 = mock(
                "/eclipse/install/dir/Eclipse.app/Contents/MacOS/eclipse",
                arguments2);

        List<Info> processes = new ArrayList<>();
        processes.add(info1);
        processes.add(info2);
        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertEquals(Optional.of(info2), ideCommand);

        Assert.assertEquals("/eclipse/install/dir/Eclipse.app",
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    @Test
    public void ideaOnLinuxDetected() throws IOException {
        File baseDirectory = Files.createTempDirectory("testIntellij").toFile();
        File binDirectory = new File(baseDirectory, "bin");
        binDirectory.mkdir();
        File bin = new File(binDirectory, "idea");
        bin.createNewFile();

        String cmd1 = "/home/sombody/.sdkman/candidates/java/17.0.5-tem/bin/java";
        String cmdLine1 = "-javaagent:/home/sombody/local/tools/idea/2021.3/lib/idea_rt.jar=46177:/home/sombody/local/tools/idea/2021.3/bin -Dfile.encoding=UTF-8 -classpath /home/sombody/tmp/my-app-v24/target/classes:/home/sombody/.m2/repository/com/vaadin/vaadin/24.0-SNAPSHOT/vaadin-24.0-20230110.022317-120.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-core/24.0-SNAPSHOT/vaadin-core-24.0-20230110.022311-120.jar:/home/sombody/.m2/repository/com/vaadin/flow-server/24.0-SNAPSHOT/flow-server-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/servletdetector/throw-if-servlet3/1.0.2/throw-if-servlet3-1.0.2.jar:/home/sombody/.m2/repository/com/vaadin/flow-commons-upload/24.0-SNAPSHOT/flow-commons-upload-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.1/jackson-core-2.14.1.jar:/home/sombody/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar:/home/sombody/.m2/repository/com/helger/ph-css/6.5.0/ph-css-6.5.0.jar:/home/sombody/.m2/repository/com/helger/commons/ph-commons/10.1.6/ph-commons-10.1.6.jar:/home/sombody/.m2/repository/com/vaadin/external/gentyref/1.2.0.vaadin1/gentyref-1.2.0.vaadin1.jar:/home/sombody/.m2/repository/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar:/home/sombody/.m2/repository/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar:/home/sombody/.m2/repository/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-dev-server/24.0-SNAPSHOT/vaadin-dev-server-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/sombody/.m2/repository/com/vaadin/flow-lit-template/24.0-SNAPSHOT/flow-lit-template-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-push/24.0-SNAPSHOT/flow-push-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.0.slf4jvaadin2/atmosphere-runtime-3.0.0.slf4jvaadin2.jar:/home/sombody/.m2/repository/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar:/home/sombody/.m2/repository/com/vaadin/flow-client/24.0-SNAPSHOT/flow-client-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-html-components/24.0-SNAPSHOT/flow-html-components-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-data/24.0-SNAPSHOT/flow-data-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-dnd/24.0-SNAPSHOT/flow-dnd-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/org/webjars/npm/vaadin__vaadin-mobile-drag-drop/1.0.1/vaadin__vaadin-mobile-drag-drop-1.0.1.jar:/home/sombody/.m2/repository/org/webjars/npm/mobile-drag-drop/2.3.0-rc.2/mobile-drag-drop-2.3.0-rc.2.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-lumo-theme/24.0-SNAPSHOT/vaadin-lumo-theme-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-material-theme/24.0-SNAPSHOT/vaadin-material-theme-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-accordion-flow/24.0-SNAPSHOT/vaadin-accordion-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-avatar-flow/24.0-SNAPSHOT/vaadin-avatar-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-button-flow/24.0-SNAPSHOT/vaadin-button-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-checkbox-flow/24.0-SNAPSHOT/vaadin-checkbox-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-combo-box-flow/24.0-SNAPSHOT/vaadin-combo-box-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-confirm-dialog-flow/24.0-SNAPSHOT/vaadin-confirm-dialog-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-custom-field-flow/24.0-SNAPSHOT/vaadin-custom-field-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-date-picker-flow/24.0-SNAPSHOT/vaadin-date-picker-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-date-time-picker-flow/24.0-SNAPSHOT/vaadin-date-time-picker-flow-24.0-2023010";

        String cmd2 = "/home/sombody/local/tools/idea/2021.3/jbr/bin/java";
        String cmdLine2 = "/home/sombody/.sdkman/candidates/java/17/bin/java -javaagent:/home/sombody/local/tools/idea/2021.3/lib/idea_rt.jar=46177:/home/sombody/local/tools/idea/2021.3/bin -Dfile.encoding=UTF-8 -classpath /home/sombody/tmp/my-app-v24/target/classes:/home/sombody/.m2/repository/com/vaadin/vaadin/24.0-SNAPSHOT/vaadin-24.0-20230110.022317-120.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-core/24.0-SNAPSHOT/vaadin-core-24.0-20230110.022311-120.jar:/home/sombody/.m2/repository/com/vaadin/flow-server/24.0-SNAPSHOT/flow-server-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/servletdetector/throw-if-servlet3/1.0.2/throw-if-servlet3-1.0.2.jar:/home/sombody/.m2/repository/com/vaadin/flow-commons-upload/24.0-SNAPSHOT/flow-commons-upload-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/sombody/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.1/jackson-core-2.14.1.jar:/home/sombody/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar:/home/sombody/.m2/repository/com/helger/ph-css/6.5.0/ph-css-6.5.0.jar:/home/sombody/.m2/repository/com/helger/commons/ph-commons/10.1.6/ph-commons-10.1.6.jar:/home/sombody/.m2/repository/com/vaadin/external/gentyref/1.2.0.vaadin1/gentyref-1.2.0.vaadin1.jar:/home/sombody/.m2/repository/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar:/home/sombody/.m2/repository/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar:/home/sombody/.m2/repository/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-dev-server/24.0-SNAPSHOT/vaadin-dev-server-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/sombody/.m2/repository/com/vaadin/flow-lit-template/24.0-SNAPSHOT/flow-lit-template-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-push/24.0-SNAPSHOT/flow-push-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.0.slf4jvaadin2/atmosphere-runtime-3.0.0.slf4jvaadin2.jar:/home/sombody/.m2/repository/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar:/home/sombody/.m2/repository/com/vaadin/flow-client/24.0-SNAPSHOT/flow-client-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-html-components/24.0-SNAPSHOT/flow-html-components-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-data/24.0-SNAPSHOT/flow-data-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/com/vaadin/flow-dnd/24.0-SNAPSHOT/flow-dnd-24.0-SNAPSHOT.jar:/home/sombody/.m2/repository/org/webjars/npm/vaadin__vaadin-mobile-drag-drop/1.0.1/vaadin__vaadin-mobile-drag-drop-1.0.1.jar:/home/sombody/.m2/repository/org/webjars/npm/mobile-drag-drop/2.3.0-rc.2/mobile-drag-drop-2.3.0-rc.2.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-lumo-theme/24.0-SNAPSHOT/vaadin-lumo-theme-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-material-theme/24.0-SNAPSHOT/vaadin-material-theme-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-accordion-flow/24.0-SNAPSHOT/vaadin-accordion-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-avatar-flow/24.0-SNAPSHOT/vaadin-avatar-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-button-flow/24.0-SNAPSHOT/vaadin-button-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-checkbox-flow/24.0-SNAPSHOT/vaadin-checkbox-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-combo-box-flow/24.0-SNAPSHOT/vaadin-combo-box-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-confirm-dialog-flow/24.0-SNAPSHOT/vaadin-confirm-dialog-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-custom-field-flow/24.0-SNAPSHOT/vaadin-custom-field-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-date-picker-flow/24.0-SNAPSHOT/vaadin-date-picker-flow-24.0-20230109.170111-251.jar:/home/sombody/.m2/repository/com/vaadin/vaadin-date-time-picker-flow/24.0-SNAPSHOT/vaadin-date-time-picker-flow-24.0-2023010";

        String baseDirInCommands = "/home/sombody/local/tools/idea/2021.3";
        String baseDir = baseDirectory.getAbsolutePath();

        Info info1 = mock(cmd1.replace(baseDirInCommands, baseDir),
                cmdLine1.replace(baseDirInCommands, baseDir));
        Info info2 = mock(cmd2.replace(baseDirInCommands, baseDir),
                cmdLine2.replace(baseDirInCommands, baseDir));
        Info info3 = mock("/usr/lib/systemd/systemd",
                new String[] { "--user" });

        List<Info> processes = new ArrayList<>();
        processes.add(info1);
        processes.add(info2);
        processes.add(info3);

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);

        Assert.assertTrue(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isEclipse(ideCommand.get()));

        Assert.assertEquals(new File(binDirectory, "idea").getAbsolutePath(),
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    @Test
    public void ideaOnWindowsDetected() throws IOException {
        String cmd1 = "C:\\dev\\devtools\\java\\eclipse-temurin-hotspot\\jdk-17.0.4.1+1\\bin\\java.exe";
        String cmd2 = "C:\\Users\\Somebody\\AppData\\Local\\JetBrains\\Toolbox\\apps\\IDEA-U\\ch-0\\223.8214.52\\bin\\idea64.exe";

        Info info1 = mock(cmd1);
        Info info2 = mock(cmd2);

        List<Info> processes = new ArrayList<>();
        processes.add(info1);
        processes.add(info2);

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);

        Assert.assertTrue(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isEclipse(ideCommand.get()));

        Assert.assertEquals(
                "C:\\Users\\Somebody\\AppData\\Local\\JetBrains\\Toolbox\\apps\\IDEA-U\\ch-0\\223.8214.52\\bin\\idea64.exe",
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    private Info mock(String cmd) {
        return mock(cmd, cmd);
    }

    private Info mock(String cmd, String[] arguments) {
        Info info = Mockito.mock(Info.class);
        Mockito.when(info.command()).thenReturn(Optional.of(cmd));
        Mockito.when(info.arguments()).thenReturn(Optional.of(arguments));
        Mockito.when(info.commandLine()).thenReturn(
                Optional.of(cmd + " " + String.join(" ", arguments)));

        return info;
    }

    private Info mock(String cmd, String cmdline) {
        Info info = Mockito.mock(Info.class);
        Mockito.when(info.command()).thenReturn(Optional.of(cmd));
        Mockito.when(info.arguments()).thenReturn(Optional.empty());
        Mockito.when(info.commandLine()).thenReturn(Optional.of(cmdline));

        return info;
    }
}

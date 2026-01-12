/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessHandle.Info;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.open.OSUtils;

public class OpenInCurrentIdeTest {

    @Test
    public void ideaOnMacDetected() throws IOException {
        File baseDirectory = setupIntelliJDirectory(true, "idea");

        String cmd1 = "/Users/somebody/.sdkman/candidates/java/17.0.1-zulu/zulu-17.jdk/Contents/Home/bin/java";
        String[] args1 = new String[] {
                "-javaagent:/Users/somebody/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-0/223.8214.52/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=58063:/Users/somebody/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-0/223.8214.52/IntelliJ IDEA.app/Contents/bin",
                "-Dfile.encoding=UTF-8", "-classpath",
                "/Users/somebody/Downloads/processtree-main/target/classes:/Users/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/Users/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar",
                "com.example.application.Application", };

        String cmd2 = "/Users/somebody/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-0/223.8214.52/IntelliJ IDEA.app/Contents/MacOS/idea";
        String[] args2 = new String[] {};

        String baseDirInCommands = "/Users/somebody/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-0/223.8214.52/IntelliJ IDEA.app/Contents";
        String baseDir = baseDirectory.getAbsolutePath();

        List<Info> processes = new ArrayList<>();

        processes.add(mock(cmd1.replace(baseDirInCommands, baseDir),
                replaceInArray(args1, baseDirInCommands, baseDir)));
        processes.add(mock(cmd2.replace(baseDirInCommands, baseDir),
                replaceInArray(args2, baseDirInCommands, baseDir)));

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertTrue(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isEclipse(ideCommand.get()));

        // The binary on Mac is /.../IntelliJ IDEA.app/Contents/MacOS/idea
        Assert.assertEquals(
                new File(baseDirectory, "MacOS/idea").getAbsolutePath(),
                new File(OpenInCurrentIde.getBinary(ideCommand.get()))
                        .getAbsolutePath());

    }

    @Test
    public void ideaOnLinuxDetected() throws IOException {
        File baseDirectory = setupIntelliJDirectory(false, "idea");

        String cmd1 = "/home/somebody/.sdkman/candidates/java/17.0.5-tem/bin/java";
        String cmdLine1 = "-javaagent:/home/somebody/local/tools/idea/2021.3/lib/idea_rt.jar=46177:/home/somebody/local/tools/idea/2021.3/bin -Dfile.encoding=UTF-8 -classpath /home/somebody/tmp/my-app-v24/target/classes:/home/somebody/.m2/repository/com/vaadin/vaadin/24.0-SNAPSHOT/vaadin-24.0-20240110.022317-120.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-core/24.0-SNAPSHOT/vaadin-core-24.0-20240110.022311-120.jar:/home/somebody/.m2/repository/com/vaadin/flow-server/24.0-SNAPSHOT/flow-server-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/servletdetector/throw-if-servlet3/1.0.2/throw-if-servlet3-1.0.2.jar:/home/somebody/.m2/repository/com/vaadin/flow-commons-upload/24.0-SNAPSHOT/flow-commons-upload-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.1/jackson-core-2.14.1.jar:/home/somebody/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar:/home/somebody/.m2/repository/com/helger/ph-css/6.5.0/ph-css-6.5.0.jar:/home/somebody/.m2/repository/com/helger/commons/ph-commons/10.1.6/ph-commons-10.1.6.jar:/home/somebody/.m2/repository/com/vaadin/external/gentyref/1.2.0.vaadin1/gentyref-1.2.0.vaadin1.jar:/home/somebody/.m2/repository/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar:/home/somebody/.m2/repository/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar:/home/somebody/.m2/repository/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-dev-server/24.0-SNAPSHOT/vaadin-dev-server-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/somebody/.m2/repository/com/vaadin/flow-lit-template/24.0-SNAPSHOT/flow-lit-template-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-push/24.0-SNAPSHOT/flow-push-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.0.slf4jvaadin2/atmosphere-runtime-3.0.0.slf4jvaadin2.jar:/home/somebody/.m2/repository/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar:/home/somebody/.m2/repository/com/vaadin/flow-client/24.0-SNAPSHOT/flow-client-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-html-components/24.0-SNAPSHOT/flow-html-components-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-data/24.0-SNAPSHOT/flow-data-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-dnd/24.0-SNAPSHOT/flow-dnd-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-lumo-theme/24.0-SNAPSHOT/vaadin-lumo-theme-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-material-theme/24.0-SNAPSHOT/vaadin-material-theme-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-accordion-flow/24.0-SNAPSHOT/vaadin-accordion-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-avatar-flow/24.0-SNAPSHOT/vaadin-avatar-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-button-flow/24.0-SNAPSHOT/vaadin-button-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-checkbox-flow/24.0-SNAPSHOT/vaadin-checkbox-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-combo-box-flow/24.0-SNAPSHOT/vaadin-combo-box-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-confirm-dialog-flow/24.0-SNAPSHOT/vaadin-confirm-dialog-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-custom-field-flow/24.0-SNAPSHOT/vaadin-custom-field-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-date-picker-flow/24.0-SNAPSHOT/vaadin-date-picker-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-date-time-picker-flow/24.0-SNAPSHOT/vaadin-date-time-picker-flow-24.0-2024010";

        String cmd2 = "/home/somebody/local/tools/idea/2021.3/jbr/bin/java";
        String cmdLine2 = "/home/somebody/local/tools/idea/2021.3/jbr/bin/java -classpath /home/somebody/local/tools/idea/2021.3/lib/util.jar:/home/somebody/local/tools/idea/2021.3/lib/app.jar:/home/somebody/local/tools/idea/2021.3/lib/3rd-party-rt.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-statistics-devkit.jar:/home/somebody/local/tools/idea/2021.3/lib/jps-model.jar:/home/somebody/local/tools/idea/2021.3/lib/stats.jar:/home/somebody/local/tools/idea/2021.3/lib/protobuf.jar:/home/somebody/local/tools/idea/2021.3/lib/external-system-rt.jar:/home/somebody/local/tools/idea/2021.3/lib/forms_rt.jar:/home/somebody/local/tools/idea/2021.3/lib/intellij-test-discovery.jar:/home/somebody/local/tools/idea/2021.3/lib/annotations.jar:/home/somebody/local/tools/idea/2021.3/lib/groovy.jar:/home/somebody/local/tools/idea/2021.3/lib/3rd-party-native.jar:/home/somebody/local/tools/idea/2021.3/lib/annotations-java5.jar:/home/somebody/local/tools/idea/2021.3/lib/byte-buddy-agent.jar:/home/somebody/local/tools/idea/2021.3/lib/error-prone-annotations.jar:/home/somebody/local/tools/idea/2021.3/lib/externalProcess-rt.jar:/home/somebody/local/tools/idea/2021.3/lib/idea_rt.jar:/home/somebody/local/tools/idea/2021.3/lib/intellij-coverage-agent-1.0.682.jar:/home/somebody/local/tools/idea/2021.3/lib/jsch-agent.jar:/home/somebody/local/tools/idea/2021.3/lib/jsp-base.jar:/home/somebody/local/tools/idea/2021.3/lib/junit.jar:/home/somebody/local/tools/idea/2021.3/lib/junit4.jar:/home/somebody/local/tools/idea/2021.3/lib/kotlin-script-runtime.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-duplicates-analysis.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-objectSerializer-annotations.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-structuralSearch.jar:/home/somebody/local/tools/idea/2021.3/lib/rd.jar:/home/somebody/local/tools/idea/2021.3/lib/tools-testsBootstrap.jar:/home/somebody/local/tools/idea/2021.3/lib/util_rt.jar:/home/somebody/local/tools/idea/2021.3/lib/xml-dom-impl.jar:/home/somebody/local/tools/idea/2021.3/lib/xml-dom.jar:/home/somebody/local/tools/idea/2021.3/lib/ant/lib/ant.jar -Xms128m -Xmx750m -XX:ReservedCodeCacheSize=512m -XX:+UseG1GC -XX:SoftRefLRUPolicyMSPerMB=50 -XX:CICompilerCount=2 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:+IgnoreUnrecognizedVMOptions -XX:CompileCommand=exclude,com/intellij/openapi/vfs/impl/FilePartNodeRoot,trieDescend -ea -Dsun.io.useCanonCaches=false -Dsun.java2d.metal=true -Djbr.catch.SIGABRT=true -Djdk.http.auth.tunneling.disabledSchemes=\"\" -Djdk.attach.allowAttachSelf=true -Djdk.module.illegalAccess.silent=true -Dkotlinx.coroutines.debug=off -Dsun.tools.attach.tmp.only=true -Xmx1985m -XX:ErrorFile=/home/somebody/java_error_in_idea_%p.log -XX:HeapDumpPath=/home/somebody/java_error_in_idea_.hprof -Djb.vmOptionsFile=/home/somebody/.config/JetBrains/IdeaIC2022.3/idea64.vmoptions -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.vendor.name=JetBrains -Didea.paths.selector=IdeaIC2022.3 -Djna.boot.library.path=/home/somebody/local/tools/idea/2021.3/lib/jna/amd64 -Dpty4j.preferred.native.folder=/home/somebody/local/tools/idea/2021.3/lib/pty4j -Djna.nosys=true -Djna.nounpack=true -Didea.platform.prefix=Idea -Dsplash=true --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.nio.charset=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.fs=ALL-UNNAMED --add-opens=java.base/sun.security.ssl=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.dnd.peer=ALL";

        String baseDirInCommands = "/home/somebody/local/tools/idea/2021.3";
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

        Assert.assertEquals(
                new File(baseDirectory, "bin/idea").getAbsolutePath(),
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    @Test
    public void ideaOnLinuxDebugModeDetected() throws IOException {
        File baseDirectory = setupIntelliJDirectory(false, "idea");

        String cmd1 = "/home/somebody/.sdkman/candidates/java/17.0.5-tem/bin/java";
        String[] args1 = new String[] {
                "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:46225,suspend=y,server=n",
                "-javaagent:/home/somebody/local/tools/idea/2021.3/plugins/java/lib/rt/debugger-agent.jar",
                "-Dfile.encoding=UTF-8", "-classpath",
                "/home/somebody/projects/vaadin/misc/processtree/target/classes:/home/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/somebody/local/tools/idea/2021.3/lib/idea_rt.jar",
                "com.example.application.Application", };
        String cmd2 = "/home/somebody/local/tools/idea/2021.3/jbr/bin/java";

        String baseDirInCommands = "/home/somebody/local/tools/idea/2021.3";
        String baseDir = baseDirectory.getAbsolutePath();

        Info info1 = mock(cmd1.replace(baseDirInCommands, baseDir),
                replaceInArray(args1, baseDirInCommands, baseDir));
        Info info2 = mock(cmd2.replace(baseDirInCommands, baseDir));
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

        Assert.assertEquals(
                new File(baseDirectory, "bin/idea").getAbsolutePath(),
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    @Test
    public void ideaOnLinuxCustomJavaAgentDetected() throws IOException {
        File baseDirectory = setupIntelliJDirectory(false, "idea");

        String cmd1 = "/home/somebody/.sdkman/candidates/java/17.0.5-tem/bin/java";
        String cmdLine1 = " -javaagent:/home/somebody/.sdkman/candidates/java/11.0.15-trava/lib/hotswap/hotswap-agent.jar=autoHotswap=true -javaagent:/home/somebody/local/tools/idea/2021.3/lib/idea_rt.jar=34585:/home/somebody/local/tools/idea/2021.3/bin -Dfile.encoding=UTF-8 -classpath /home/somebody/tmp/my-app-v24/target/classes:/home/somebody/.m2/repository/com/vaadin/vaadin/24.0-SNAPSHOT/vaadin-24.0-20240110.022317-120.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-core/24.0-SNAPSHOT/vaadin-core-24.0-20240110.022311-120.jar:/home/somebody/.m2/repository/com/vaadin/flow-server/24.0-SNAPSHOT/flow-server-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/servletdetector/throw-if-servlet3/1.0.2/throw-if-servlet3-1.0.2.jar:/home/somebody/.m2/repository/com/vaadin/flow-commons-upload/24.0-SNAPSHOT/flow-commons-upload-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.1/jackson-core-2.14.1.jar:/home/somebody/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar:/home/somebody/.m2/repository/com/helger/ph-css/6.5.0/ph-css-6.5.0.jar:/home/somebody/.m2/repository/com/helger/commons/ph-commons/10.1.6/ph-commons-10.1.6.jar:/home/somebody/.m2/repository/com/vaadin/external/gentyref/1.2.0.vaadin1/gentyref-1.2.0.vaadin1.jar:/home/somebody/.m2/repository/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar:/home/somebody/.m2/repository/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar:/home/somebody/.m2/repository/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-dev-server/24.0-SNAPSHOT/vaadin-dev-server-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/somebody/.m2/repository/com/vaadin/flow-lit-template/24.0-SNAPSHOT/flow-lit-template-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-push/24.0-SNAPSHOT/flow-push-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.0.slf4jvaadin2/atmosphere-runtime-3.0.0.slf4jvaadin2.jar:/home/somebody/.m2/repository/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar:/home/somebody/.m2/repository/com/vaadin/flow-client/24.0-SNAPSHOT/flow-client-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-html-components/24.0-SNAPSHOT/flow-html-components-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-data/24.0-SNAPSHOT/flow-data-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-dnd/24.0-SNAPSHOT/flow-dnd-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-lumo-theme/24.0-SNAPSHOT/vaadin-lumo-theme-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-material-theme/24.0-SNAPSHOT/vaadin-material-theme-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-accordion-flow/24.0-SNAPSHOT/vaadin-accordion-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-avatar-flow/24.0-SNAPSHOT/vaadin-avatar-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-button-flow/24.0-SNAPSHOT/vaadin-button-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-checkbox-flow/24.0-SNAPSHOT/vaadin-checkbox-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-combo-box-flow/24.0-SNAPSHOT/vaadin-combo-box-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-confirm-dialog-flow/24.0-SNAPSHOT/vaadin-confirm-dialog-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-custom-field-flow/24.0-SNAPSHOT/vaadin-custom-field-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-date-picker-flow/24.0-SNAPSHOT/vaadin-date-picker-flow-24.0-20240109.170111-251.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-date-time-picker-flow/24.0-SNAPSHOT/vaadin-date-time-picker-flow-24.0-2024010";

        String cmd2 = "/home/somebody/local/tools/idea/2021.3/jbr/bin/java";
        String cmdLine2 = "/home/somebody/local/tools/idea/2021.3/jbr/bin/java -classpath /home/somebody/local/tools/idea/2021.3/lib/util.jar:/home/somebody/local/tools/idea/2021.3/lib/app.jar:/home/somebody/local/tools/idea/2021.3/lib/3rd-party-rt.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-statistics-devkit.jar:/home/somebody/local/tools/idea/2021.3/lib/jps-model.jar:/home/somebody/local/tools/idea/2021.3/lib/stats.jar:/home/somebody/local/tools/idea/2021.3/lib/protobuf.jar:/home/somebody/local/tools/idea/2021.3/lib/external-system-rt.jar:/home/somebody/local/tools/idea/2021.3/lib/forms_rt.jar:/home/somebody/local/tools/idea/2021.3/lib/intellij-test-discovery.jar:/home/somebody/local/tools/idea/2021.3/lib/annotations.jar:/home/somebody/local/tools/idea/2021.3/lib/groovy.jar:/home/somebody/local/tools/idea/2021.3/lib/3rd-party-native.jar:/home/somebody/local/tools/idea/2021.3/lib/annotations-java5.jar:/home/somebody/local/tools/idea/2021.3/lib/byte-buddy-agent.jar:/home/somebody/local/tools/idea/2021.3/lib/error-prone-annotations.jar:/home/somebody/local/tools/idea/2021.3/lib/externalProcess-rt.jar:/home/somebody/local/tools/idea/2021.3/lib/idea_rt.jar:/home/somebody/local/tools/idea/2021.3/lib/intellij-coverage-agent-1.0.682.jar:/home/somebody/local/tools/idea/2021.3/lib/jsch-agent.jar:/home/somebody/local/tools/idea/2021.3/lib/jsp-base.jar:/home/somebody/local/tools/idea/2021.3/lib/junit.jar:/home/somebody/local/tools/idea/2021.3/lib/junit4.jar:/home/somebody/local/tools/idea/2021.3/lib/kotlin-script-runtime.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-duplicates-analysis.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-objectSerializer-annotations.jar:/home/somebody/local/tools/idea/2021.3/lib/platform-structuralSearch.jar:/home/somebody/local/tools/idea/2021.3/lib/rd.jar:/home/somebody/local/tools/idea/2021.3/lib/tools-testsBootstrap.jar:/home/somebody/local/tools/idea/2021.3/lib/util_rt.jar:/home/somebody/local/tools/idea/2021.3/lib/xml-dom-impl.jar:/home/somebody/local/tools/idea/2021.3/lib/xml-dom.jar:/home/somebody/local/tools/idea/2021.3/lib/ant/lib/ant.jar -Xms128m -Xmx750m -XX:ReservedCodeCacheSize=512m -XX:+UseG1GC -XX:SoftRefLRUPolicyMSPerMB=50 -XX:CICompilerCount=2 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:+IgnoreUnrecognizedVMOptions -XX:CompileCommand=exclude,com/intellij/openapi/vfs/impl/FilePartNodeRoot,trieDescend -ea -Dsun.io.useCanonCaches=false -Dsun.java2d.metal=true -Djbr.catch.SIGABRT=true -Djdk.http.auth.tunneling.disabledSchemes=\"\" -Djdk.attach.allowAttachSelf=true -Djdk.module.illegalAccess.silent=true -Dkotlinx.coroutines.debug=off -Dsun.tools.attach.tmp.only=true -Xmx1985m -XX:ErrorFile=/home/somebody/java_error_in_idea_%p.log -XX:HeapDumpPath=/home/somebody/java_error_in_idea_.hprof -Djb.vmOptionsFile=/home/somebody/.config/JetBrains/IdeaIC2022.3/idea64.vmoptions -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.vendor.name=JetBrains -Didea.paths.selector=IdeaIC2022.3 -Djna.boot.library.path=/home/somebody/local/tools/idea/2021.3/lib/jna/amd64 -Dpty4j.preferred.native.folder=/home/somebody/local/tools/idea/2021.3/lib/pty4j -Djna.nosys=true -Djna.nounpack=true -Didea.platform.prefix=Idea -Dsplash=true --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.ref=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.nio.charset=ALL-UNNAMED --add-opens=java.base/java.text=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/jdk.internal.vm=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.nio.fs=ALL-UNNAMED --add-opens=java.base/sun.security.ssl=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.desktop/com.sun.java.swing.plaf.gtk=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.dnd.peer=ALL";

        String baseDirInCommands = "/home/somebody/local/tools/idea/2021.3";
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

        Assert.assertEquals(
                new File(baseDirectory, "bin/idea").getAbsolutePath(),
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    private File setupIntelliJDirectory(boolean mac, String binaryFilename)
            throws IOException {
        File baseDirectory = Files.createTempDirectory("testIntellij").toFile();
        File binDirectory;
        if (mac) {
            binDirectory = new File(new File(baseDirectory, "MacOS"), "bin");
        } else {
            binDirectory = new File(baseDirectory, "bin");
        }
        binDirectory.mkdirs();
        File bin = new File(binDirectory, binaryFilename);
        bin.createNewFile();
        baseDirectory.deleteOnExit();
        return baseDirectory;
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

    private String[] replaceInArray(String[] args, String find,
            String replace) {
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].replace(find, replace);
        }
        return args;
    }

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
                "/Users/somebody/Library/Application Support/Code/logs/20221225T212012", };

        String cmd4 = "/Applications/Visual Studio Code.app/Contents/Frameworks/Code Helper (Renderer).app/Contents/MacOS/Code Helper (Renderer)";
        String[] args4 = new String[] { "--type=renderer",
                "--user-data-dir=/Users/somebody/Library/Application Support/Code",
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
    public void vsCodeOnWindowsDetected() {

        String cmd1 = "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.3.7-hotspot\\bin\\java.exe";

        String cmd2 = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";

        String cmd3 = "C:\\Users\\Somebody\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe";

        String cmd4 = "C:\\Users\\Somebody\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe";

        String cmd5 = "C:\\Users\\Somebody\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe";

        String cmd6 = "C:\\Windows\\explorer.exe";

        List<Info> processes = new ArrayList<>();
        processes.add(mock(cmd1));
        processes.add(mock(cmd2));
        processes.add(mock(cmd3));
        processes.add(mock(cmd4));
        processes.add(mock(cmd5));
        processes.add(mock(cmd6));

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertFalse(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertTrue(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isEclipse(ideCommand.get()));

        // THe binary is not used for VSCode
        Assert.assertEquals(
                "C:\\Users\\Somebody\\AppData\\Local\\Programs\\Microsoft VS Code\\Code.exe",
                OpenInCurrentIde.getBinary(ideCommand.get()));

    }

    @Test
    public void vsCodeOnLinuxDetected() {
        String cmd1 = "/usr/lib/jvm/java-17-openjdk-17.0.5.0.8-1.fc37.x86_64/bin/java";
        String[] args1 = new String[] { "-Dfile.encoding=UTF-8", "-classpath",
                "/home/somebody/vaadin/temp-projects/processtree/target/classes:/home/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar",
                "com.example.application.Application", };

        String cmd2 = "/usr/lib/jvm/java-17-openjdk-17.0.5.0.8-1.fc37.x86_64/bin/java";
        String[] args2 = new String[] { "-classpath",
                "/home/somebody/.vscode/extensions/asf.apache-netbeans-java-16.0.0/nbcode/java/maven/boot/plexus-classworlds-2.6.0.jar",
                "-Dclassworlds.conf=/home/somebody/.vscode/extensions/asf.apache-netbeans-java-16.0.0/nbcode/java/maven/bin/m2.conf",
                "-Dmaven.home=/home/somebody/.vscode/extensions/asf.apache-netbeans-java-16.0.0/nbcode/java/maven",
                "-Dlibrary.jansi.path=/home/somebody/.vscode/extensions/asf.apache-netbeans-java-16.0.0/nbcode/java/maven/lib/jansi-native",
                "-Dmaven.multiModuleProjectDirectory=/home/somebody/vaadin/temp-projects/processtree",
                "org.codehaus.plexus.classworlds.launcher.Launcher",
                "-Dexec.vmArgs=-Dfile.encoding=UTF-8",
                "-Dexec.args=${exec.vmArgs} -classpath %classpath ${exec.mainClass} ${exec.appArgs}",
                "-Dexec.executable=/usr/lib/jvm/java-17/bin/java",
                "-Dexec.mainClass=com.example.application.Application",
                "-Dexec.classpathScope=runtime", "-Dexec.appArgs=",
                "-Dmaven.ext.class.path=/home/somebody/.vscode/extensions/asf.apache-netbeans-java-16.0.0/nbcode/java/maven-nblib/netbeans-eventspy.jar",
                "process-classes",
                "org.codehaus.mojo:exec-maven-plugin:3.0.0:exec", };

        String cmd3 = "/usr/lib/jvm/java-17-openjdk-17.0.5.0.8-1.fc37.x86_64/bin/java";

        String cmd4 = "/usr/bin/bash";
        String[] args4 = new String[] {
                "/home/somebody/.vscode/extensions/asf.apache-netbeans-java-16.0.0/nbcode/bin/../platform/lib/nbexec",
                "--jdkhome", };

        String cmd5 = "/usr/share/code/code";
        String[] args5 = new String[] { "--ms-enable-electron-run-as-node",
                "--inspect-port=0",
                "/usr/share/code/resources/app/out/bootstrap-fork",
                "--type=extensionHost", "--skipWorkspaceStorageLock", };

        String cmd6 = "/usr/share/code/code";
        String[] args6 = new String[] {};

        String cmd7 = "/usr/bin/gnome-shell";
        String[] args7 = new String[] {};

        String cmd8 = "/usr/lib/systemd/systemd";
        String[] args8 = new String[] { "--user", };

        List<Info> processes = new ArrayList<>();
        processes.add(mock(cmd1, args1));
        processes.add(mock(cmd2, args2));
        processes.add(mock(cmd3));
        processes.add(mock(cmd4, args4));
        processes.add(mock(cmd5, args5));
        processes.add(mock(cmd6, args6));
        processes.add(mock(cmd7, args7));
        processes.add(mock(cmd8, args8));

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertFalse(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertTrue(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isEclipse(ideCommand.get()));

        // THe binary is not used for VSCode
        // Assert.assertEquals("/usr/share/code/code",
        // OpenInCurrentIde.getBinary(ideCommand.get()));

    }

    @Test
    public void eclipseOnWindowsDetected() throws IOException {
        String cmd1 = "C:\\Program Files\\Eclipse Adoptium\\jdk-11.0.15.10-hotspot\\bin\\javaw.exe";

        String cmd2 = "C:\\Users\\Somebody\\eclipse\\java-2022-06\\eclipse\\eclipse.exe";

        String cmd3 = "C:\\Windows\\explorer.exe";

        Info info1 = mock(cmd1);
        Info info2 = mock(cmd2);
        Info info3 = mock(cmd3);

        List<Info> processes = new ArrayList<>();
        processes.add(info1);
        processes.add(info2);
        processes.add(info3);

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);

        Assert.assertFalse(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertTrue(OpenInCurrentIde.isEclipse(ideCommand.get()));

        Assert.assertEquals(
                "C:\\Users\\Somebody\\eclipse\\java-2022-06\\eclipse\\eclipse.exe",
                OpenInCurrentIde.getBinary(ideCommand.get()));
    }

    @Test
    public void eclipseOnMacDetected() {
        String[] arguments = new String[] { "-Dfile.encoding=UTF-8",
                "-classpath",
                "/home/somebody/.m2/repository/com/vaadin/vaadin/24.0-SNAPSHOT/vaadin-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-core/24.0-SNAPSHOT/vaadin-core-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-server/24.0-SNAPSHOT/flow-server-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/servletdetector/throw-if-servlet3/1.0.2/throw-if-servlet3-1.0.2.jar:/home/somebody/.m2/repository/com/vaadin/flow-commons-upload/24.0-SNAPSHOT/flow-commons-upload-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.14.1/jackson-core-2.14.1.jar:/home/somebody/.m2/repository/org/jsoup/jsoup/1.15.3/jsoup-1.15.3.jar:/home/somebody/.m2/repository/com/helger/ph-css/6.5.0/ph-css-6.5.0.jar:/home/somebody/.m2/repository/com/helger/commons/ph-commons/10.1.6/ph-commons-10.1.6.jar:/home/somebody/.m2/repository/com/vaadin/external/gentyref/1.2.0.vaadin1/gentyref-1.2.0.vaadin1.jar:/home/somebody/.m2/repository/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar:/home/somebody/.m2/repository/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar:/home/somebody/.m2/repository/org/apache/httpcomponents/httpcore/4.4.15/httpcore-4.4.15.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-dev-server/24.0-SNAPSHOT/vaadin-dev-server-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/somebody/.m2/repository/com/vaadin/flow-lit-template/24.0-SNAPSHOT/flow-lit-template-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-push/24.0-SNAPSHOT/flow-push-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/external/atmosphere/atmosphere-runtime/3.0.0.slf4jvaadin2/atmosphere-runtime-3.0.0.slf4jvaadin2.jar:/home/somebody/.m2/repository/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar:/home/somebody/.m2/repository/com/vaadin/flow-client/24.0-SNAPSHOT/flow-client-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-html-components/24.0-SNAPSHOT/flow-html-components-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-data/24.0-SNAPSHOT/flow-data-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/flow-dnd/24.0-SNAPSHOT/flow-dnd-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-lumo-theme/24.0-SNAPSHOT/vaadin-lumo-theme-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-material-theme/24.0-SNAPSHOT/vaadin-material-theme-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-accordion-flow/24.0-SNAPSHOT/vaadin-accordion-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-avatar-flow/24.0-SNAPSHOT/vaadin-avatar-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-button-flow/24.0-SNAPSHOT/vaadin-button-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-checkbox-flow/24.0-SNAPSHOT/vaadin-checkbox-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-combo-box-flow/24.0-SNAPSHOT/vaadin-combo-box-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-confirm-dialog-flow/24.0-SNAPSHOT/vaadin-confirm-dialog-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-custom-field-flow/24.0-SNAPSHOT/vaadin-custom-field-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-date-picker-flow/24.0-SNAPSHOT/vaadin-date-picker-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-date-time-picker-flow/24.0-SNAPSHOT/vaadin-date-time-picker-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-details-flow/24.0-SNAPSHOT/vaadin-details-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-time-picker-flow/24.0-SNAPSHOT/vaadin-time-picker-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-select-flow/24.0-SNAPSHOT/vaadin-select-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-dialog-flow/24.0-SNAPSHOT/vaadin-dialog-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-form-layout-flow/24.0-SNAPSHOT/vaadin-form-layout-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-field-highlighter-flow/24.0-SNAPSHOT/vaadin-field-highlighter-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-grid-flow/24.0-SNAPSHOT/vaadin-grid-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-icons-flow/24.0-SNAPSHOT/vaadin-icons-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-virtual-list-flow/24.0-SNAPSHOT/vaadin-virtual-list-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-list-box-flow/24.0-SNAPSHOT/vaadin-list-box-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-login-flow/24.0-SNAPSHOT/vaadin-login-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-messages-flow/24.0-SNAPSHOT/vaadin-messages-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-ordered-layout-flow/24.0-SNAPSHOT/vaadin-ordered-layout-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-progress-bar-flow/24.0-SNAPSHOT/vaadin-progress-bar-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-radio-button-flow/24.0-SNAPSHOT/vaadin-radio-button-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-renderer-flow/24.0-SNAPSHOT/vaadin-renderer-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-split-layout-flow/24.0-SNAPSHOT/vaadin-split-layout-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-tabs-flow/24.0-SNAPSHOT/vaadin-tabs-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-text-field-flow/24.0-SNAPSHOT/vaadin-text-field-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-upload-flow/24.0-SNAPSHOT/vaadin-upload-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-notification-flow/24.0-SNAPSHOT/vaadin-notification-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-app-layout-flow/24.0-SNAPSHOT/vaadin-app-layout-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-context-menu-flow/24.0-SNAPSHOT/vaadin-context-menu-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-menu-bar-flow/24.0-SNAPSHOT/vaadin-menu-bar-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-board-flow/24.0-SNAPSHOT/vaadin-board-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-charts-flow/24.0-SNAPSHOT/vaadin-charts-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-cookie-consent-flow/24.0-SNAPSHOT/vaadin-cookie-consent-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-crud-flow/24.0-SNAPSHOT/vaadin-crud-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-flow-components-base/24.0-SNAPSHOT/vaadin-flow-components-base-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-grid-pro-flow/24.0-SNAPSHOT/vaadin-grid-pro-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-map-flow/24.0-SNAPSHOT/vaadin-map-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-rich-text-editor-flow/24.0-SNAPSHOT/vaadin-rich-text-editor-flow-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/collaboration-engine/6.0-SNAPSHOT/collaboration-engine-6.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/core/jackson-databind/2.14.1/jackson-databind-2.14.1.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/core/jackson-annotations/2.14.1/jackson-annotations-2.14.1.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.14.1/jackson-datatype-jsr310-2.14.1.jar:/home/somebody/.m2/repository/com/vaadin/license-checker/1.11-SNAPSHOT/license-checker-1.11-SNAPSHOT.jar:/home/somebody/.m2/repository/com/github/oshi/oshi-core/6.1.6/oshi-core-6.1.6.jar:/home/somebody/.m2/repository/net/java/dev/jna/jna-platform/5.11.0/jna-platform-5.11.0.jar:/home/somebody/.m2/repository/com/nimbusds/nimbus-jose-jwt/9.23/nimbus-jose-jwt-9.23.jar:/home/somebody/.m2/repository/org/lucee/jcip-annotations/1.0.0/jcip-annotations-1.0.0.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-dev-bundle/24.0-SNAPSHOT/vaadin-dev-bundle-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-spring-boot-starter/24.0-SNAPSHOT/vaadin-spring-boot-starter-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/com/vaadin/vaadin-spring/24.0-SNAPSHOT/vaadin-spring-24.0-SNAPSHOT.jar:/home/somebody/.m2/repository/org/springframework/spring-webmvc/6.0.2/spring-webmvc-6.0.2.jar:/home/somebody/.m2/repository/org/springframework/spring-aop/6.0.2/spring-aop-6.0.2.jar:/home/somebody/.m2/repository/org/springframework/spring-beans/6.0.2/spring-beans-6.0.2.jar:/home/somebody/.m2/repository/org/springframework/spring-expression/6.0.2/spring-expression-6.0.2.jar:/home/somebody/.m2/repository/org/springframework/spring-websocket/6.0.2/spring-websocket-6.0.2.jar:/home/somebody/.m2/repository/org/reflections/reflections/0.10.2/reflections-0.10.2.jar:/home/somebody/.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-starter-web/3.0.0/spring-boot-starter-web-3.0.0.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-starter-json/3.0.0/spring-boot-starter-json-3.0.0.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/datatype/jackson-datatype-jdk8/2.14.1/jackson-datatype-jdk8-2.14.1.jar:/home/somebody/.m2/repository/com/fasterxml/jackson/module/jackson-module-parameter-names/2.14.1/jackson-module-parameter-names-2.14.1.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-starter-tomcat/3.0.0/spring-boot-starter-tomcat-3.0.0.jar:/home/somebody/.m2/repository/org/apache/tomcat/embed/tomcat-embed-core/10.1.1/tomcat-embed-core-10.1.1.jar:/home/somebody/.m2/repository/org/apache/tomcat/embed/tomcat-embed-websocket/10.1.1/tomcat-embed-websocket-10.1.1.jar:/home/somebody/.m2/repository/org/springframework/spring-web/6.0.2/spring-web-6.0.2.jar:/home/somebody/.m2/repository/io/micrometer/micrometer-observation/1.10.2/micrometer-observation-1.10.2.jar:/home/somebody/.m2/repository/io/micrometer/micrometer-commons/1.10.2/micrometer-commons-1.10.2.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-starter-validation/3.0.0/spring-boot-starter-validation-3.0.0.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-starter/3.0.0/spring-boot-starter-3.0.0.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-starter-logging/3.0.0/spring-boot-starter-logging-3.0.0.jar:/home/somebody/.m2/repository/ch/qos/logback/logback-classic/1.4.5/logback-classic-1.4.5.jar:/home/somebody/.m2/repository/ch/qos/logback/logback-core/1.4.5/logback-core-1.4.5.jar:/home/somebody/.m2/repository/org/apache/logging/log4j/log4j-to-slf4j/2.19.0/log4j-to-slf4j-2.19.0.jar:/home/somebody/.m2/repository/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar:/home/somebody/.m2/repository/org/slf4j/jul-to-slf4j/2.0.4/jul-to-slf4j-2.0.4.jar:/home/somebody/.m2/repository/jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1.jar:/home/somebody/.m2/repository/org/yaml/snakeyaml/1.33/snakeyaml-1.33.jar:/home/somebody/.m2/repository/org/apache/tomcat/embed/tomcat-embed-el/10.1.1/tomcat-embed-el-10.1.1.jar:/home/somebody/.m2/repository/org/hibernate/validator/hibernate-validator/8.0.0.Final/hibernate-validator-8.0.0.Final.jar:/home/somebody/.m2/repository/jakarta/validation/jakarta.validation-api/3.0.2/jakarta.validation-api-3.0.2.jar:/home/somebody/.m2/repository/org/jboss/logging/jboss-logging/3.5.0.Final/jboss-logging-3.5.0.Final.jar:/home/somebody/.m2/repository/com/fasterxml/classmate/1.5.1/classmate-1.5.1.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-devtools/3.0.0/spring-boot-devtools-3.0.0.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot/3.0.0/spring-boot-3.0.0.jar:/home/somebody/.m2/repository/org/springframework/spring-context/6.0.2/spring-context-6.0.2.jar:/home/somebody/.m2/repository/org/springframework/boot/spring-boot-autoconfigure/3.0.0/spring-boot-autoconfigure-3.0.0.jar:/home/somebody/.m2/repository/net/bytebuddy/byte-buddy/1.12.19/byte-buddy-1.12.19.jar:/home/somebody/.m2/repository/org/springframework/spring-core/6.0.2/spring-core-6.0.2.jar:/home/somebody/.m2/repository/org/springframework/spring-jcl/6.0.2/spring-jcl-6.0.2.jar:/home/somebody/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar:/home/somebody/.m2/repository/org/javassist/javassist/3.28.0-GA/javassist-3.28.0-GA.jar:/home/somebody/.m2/repository/com/vaadin/external/gwt/gwt-elemental/2.8.2.vaadin2/gwt-elemental-2.8.2.vaadin2.jar:/home/somebody/.m2/repository/org/slf4j/slf4j-api/2.0.4/slf4j-api-2.0.4.jar:/home/somebody/.m2/repository/net/java/dev/jna/jna/5.11.0/jna-5.11.0.jar:/home/somebody/.m2/repository/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar",
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
        Assert.assertFalse(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertTrue(OpenInCurrentIde.isEclipse(ideCommand.get()));

        // THe binary is not used for VSCode
        Assert.assertEquals("/eclipse/install/dir/Eclipse.app",
                OpenInCurrentIde.getBinary(ideCommand.get()));

    }

    @Test
    public void eclipseOnLinuxDetected() {
        String cmd1 = "/usr/lib/jvm/java-17-openjdk-arm64/bin/java";
        String[] args1 = new String[] { "-Dfile.encoding=UTF-8", "-classpath",
                "/home/somebody/processtree/target/classes:/home/somebody/.m2/repository/com/vaadin/open/8.4.0.3/open-8.4.0.3.jar:/home/somebody/.m2/repository/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar",
                "-XX:+ShowCodeDetailsInExceptionMessages",
                "com.example.application.Application", };

        String cmd2 = "/usr/lib/jvm/java-17-openjdk-arm64/bin/java";
        String[] args2 = new String[] { "-Dosgi.requiredJavaVersion=17",
                "-Dosgi.instance.area.default=@user.home/eclipse-workspace",
                "-Dosgi.dataAreaRequiresExplicitInit=true",
                "-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true",
                "-Dsun.java.command=Eclipse", "-Xms256m", "-Xmx2048m",
                "-XX:+UseG1GC", "-XX:+UseStringDeduplication",
                "--add-modules=ALL-SYSTEM", "-Declipse.p2.max.threads=10",
                "-Doomph.update.url=https://download.eclipse.org/oomph/updates/milestone/latest",
                "-Doomph.redirection.index.redirection=index:/->http://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/",
                "-Duser.dir=/home/somebody/eclipse/jee-2022-12/eclipse", "-jar",
                "/home/somebody/eclipse/jee-2022-12/eclipse//plugins/org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar",
                "-os", "linux", "-ws", "gtk", "-arch", "aarch64", "-showsplash",
                "/home/somebody/.p2/pool/plugins/org.eclipse.epp.package.common_4.26.0.20221201-1200/splash.bmp",
                "-launcher",
                "/home/somebody/eclipse/jee-2022-12/eclipse/eclipse", "-name",
                "Eclipse", "--launcher.library",
                "/home/somebody/.p2/pool/plugins/org.eclipse.equinox.launcher.gtk.linux.aarch64_1.2.700.v20221108-1024/eclipse_11801.so",
                "-startup",
                "/home/somebody/eclipse/jee-2022-12/eclipse//plugins/org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar",
                "--launcher.appendVmargs", "-exitdata", "8004", "-product",
                "org.eclipse.epp.package.jee.product", "-vm",
                "/usr/lib/jvm/java-17-openjdk-arm64/bin/java", "-vmargs",
                "-Dosgi.requiredJavaVersion=17",
                "-Dosgi.instance.area.default=@user.home/eclipse-workspace",
                "-Dosgi.dataAreaRequiresExplicitInit=true",
                "-Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true",
                "-Dsun.java.command=Eclipse", "-Xms256m", "-Xmx2048m",
                "-XX:+UseG1GC", "-XX:+UseStringDeduplication",
                "--add-modules=ALL-SYSTEM", "-Declipse.p2.max.threads=10",
                "-Doomph.update.url=https://download.eclipse.org/oomph/updates/milestone/latest",
                "-Doomph.redirection.index.redirection=index:/->http://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/",
                "-Duser.dir=/home/somebody/eclipse/jee-2022-12/eclipse", "-jar",
                "/home/somebody/eclipse/jee-2022-12/eclipse//plugins/org.eclipse.equinox.launcher_1.6.400.v20210924-0641.jar", };

        String cmd3 = "/home/somebody/eclipse/jee-2022-12/eclipse/eclipse";
        String[] args3 = new String[] { "-vm",
                "/usr/lib/jvm/java-17-openjdk-arm64/bin", "-vmargs",
                "-Duser.dir=/home/somebody/eclipse/jee-2022-12/eclipse", };

        String cmd4 = "/usr/lib/systemd/systemd";
        String[] args4 = new String[] { "--user", };
        Info info1 = mock(cmd1, args1);
        Info info2 = mock(cmd2, args2);
        Info info3 = mock(cmd3, args3);
        Info info4 = mock(cmd4, args4);

        List<Info> processes = new ArrayList<>();
        processes.add(info1);
        processes.add(info2);
        processes.add(info3);
        processes.add(info4);

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertFalse(OpenInCurrentIde.isIdea(ideCommand.get()));
        Assert.assertFalse(OpenInCurrentIde.isVSCode(ideCommand.get()));
        Assert.assertTrue(OpenInCurrentIde.isEclipse(ideCommand.get()));

        // THe binary is not used for VSCode
        Assert.assertEquals(
                "/home/somebody/eclipse/jee-2022-12/eclipse/eclipse",
                OpenInCurrentIde.getBinary(ideCommand.get()));

    }

    @Test
    public void runFromCommandLineWorks() {
        String cmd1 = "/opt/homebrew/Cellar/openjdk/19.0.2/libexec/openjdk.jdk/Contents/Home/bin/java";
        String[] args1 = new String[] { "-XX:TieredStopAtLevel=1", "-Xdebug",
                "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5731",
                "-cp", "lotsofjars.jar",
                "com.example.application.Application", };

        String cmd2 = "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home/bin/java";
        String[] args2 = new String[] { "-classpath",
                "/opt/homebrew/Cellar/maven/3.9.0/libexec/boot/plexus-classworlds-2.6.0.jar",
                "-Dclassworlds.conf=/opt/homebrew/Cellar/maven/3.9.0/libexec/bin/m2.conf",
                "-Dmaven.home=/opt/homebrew/Cellar/maven/3.9.0/libexec",
                "-Dlibrary.jansi.path=/opt/homebrew/Cellar/maven/3.9.0/libexec/lib/jansi-native",
                "-Dmaven.multiModuleProjectDirectory=/home/foo/test/openide",
                "org.codehaus.plexus.classworlds.launcher.Launcher",
                "spring-boot:run", };

        String cmd3 = "/bin/zsh";
        String[] args3 = new String[] {};

        String cmd4 = null;
        String[] args4 = null;

        String cmd5 = "/System/Applications/Utilities/Terminal.app/Contents/MacOS/Terminal";
        String[] args5 = new String[] {};

        String cmd6 = null;
        String[] args6 = null;

        Info info1 = mock(cmd1, args1);
        Info info2 = mock(cmd2, args2);
        Info info3 = mock(cmd3, args3);
        Info info4 = mock(cmd4, args4);
        Info info5 = mock(cmd5, args5);
        Info info6 = mock(cmd6, args6);

        List<Info> processes = new ArrayList<>();
        processes.add(info1);
        processes.add(info2);
        processes.add(info3);
        processes.add(info4);
        processes.add(info5);
        processes.add(info6);

        Optional<Info> ideCommand = OpenInCurrentIde.findIdeCommand(processes);
        Assert.assertTrue(ideCommand.isEmpty());
    }

    private Info mock(String cmd) {
        return mock(cmd, cmd);
    }

    private Info mock(String cmd, String[] arguments) {
        Info info = Mockito.mock(Info.class);
        Mockito.when(info.command()).thenReturn(Optional.ofNullable(cmd));
        Mockito.when(info.arguments())
                .thenReturn(Optional.ofNullable(arguments));
        if (cmd != null && arguments != null) {
            Mockito.when(info.commandLine()).thenReturn(
                    Optional.of(cmd + " " + String.join(" ", arguments)));
        }

        return info;
    }

    private Info mock(String cmd, String cmdline) {
        Info info = Mockito.mock(Info.class);
        Mockito.when(info.command()).thenReturn(Optional.of(cmd));
        Mockito.when(info.arguments()).thenReturn(Optional.empty());
        Mockito.when(info.commandLine()).thenReturn(Optional.of(cmdline));

        return info;
    }

    @Test
    public void runThrowsExceptionOnFailure() throws InterruptedException {
        Assume.assumeFalse(OSUtils.isWindows());

        try {
            OpenInCurrentIde.run("/bin/sh", "-c",
                    "echo 'output1'; echo 'output2'; exit 123");
            Assert.fail("Should have thrown exception");
        } catch (IOException e) {
            Assert.assertTrue("Exit code should have been reported",
                    e.getMessage().contains("terminated with exit code 123"));
            Assert.assertTrue("Output should have been included",
                    e.getMessage().contains("output1"));
            Assert.assertTrue("Output should have been included",
                    e.getMessage().contains("output2"));
        }
    }
}

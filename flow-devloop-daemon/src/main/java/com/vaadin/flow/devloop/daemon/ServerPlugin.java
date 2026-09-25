/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.devloop.daemon;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A build plugin that runs an application server for the project, and
 * everything the dev loop needs in order to drive one.
 * <p>
 * Two shapes, and the difference is where the loop's JVM flags go. Jetty runs
 * the application in the build's own JVM, so {@code MAVEN_OPTS} carries them.
 * WildFly, TomEE, both Payaras and Cargo have no such mode - each provisions a
 * server and starts it as a process of its own - so the flags go to a parameter
 * of the plugin, named by {@link #jvmFlagsProperty}, and reach the server's JVM
 * from there.
 * <p>
 * A table rather than a class per server: the entries differ only in
 * coordinates, in which properties keep the server in-process, and in the line
 * the server logs once it has bound a port. Tomcat, added after WildFly and
 * TomEE, cost exactly one entry and one new field - which is the whole reason
 * {@link AppRuntime} is shaped the way it is, and the shape another container
 * later should still fit.
 * <p>
 * Payara then cost two entries and one more field, and the field is worth
 * knowing about because it is not a property of the server at all: Payara
 * Server's channel is declared {@code List<String>}, and Maven splits such a
 * property on commas before the plugin is reached, so {@link #commaSplitFlags}
 * says that a flag with a comma in it has to travel some other way. Payara
 * Micro needed no field, only an entry. That two containers from one vendor
 * differ in the shape of their channel is the argument for the table: neither
 * is a class, and neither is a special case anywhere else in the daemon.
 * <p>
 * Open Liberty then cost one entry and one more field, and that one is worth
 * knowing about because it is the first channel that is not a single value at
 * all: {@code liberty.jvm.<key>} carries one flag, and the plugin writes each
 * such property as its own line of the server's generated {@code jvm.options}.
 * {@link #perPropertyFlags} is what says the flags are handed over one
 * {@code -D} each rather than whitespace-separated in one.
 * <p>
 * TomEE then cost one more field, and that one is not about the channel but
 * about how Maven is asked to run the goal. A goal named on a command line runs
 * in <em>every</em> project in the reactor; a goal bound to a phase runs only
 * where the model carries it. Every other entry survives being named; TomEE's
 * does not, so {@link #boundInTheApplication} has the build extension bind it
 * in the application's module instead.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param name
 *            how this runtime is named in {@code status}, in the log and in
 *            {@code -Dvaadin.dev.runtime}
 * @param groupId
 *            the plugin's group
 * @param artifactId
 *            the plugin's artifact
 * @param goal
 *            the goal that runs the application
 * @param phase
 *            the lifecycle phase to run before the goal, empty when the goal
 *            needs nothing built first. A container that deploys a packaged WAR
 *            needs {@code package}; one that serves the module's own output
 *            does not
 * @param jvmFlagsProperty
 *            the property carrying the flags the application's JVM must start
 *            with, whitespace-separated. Empty when the server runs in the
 *            build's own JVM, where {@code MAVEN_OPTS} is the channel instead
 * @param projectPropertyFlags
 *            whether that property is a Maven <em>project</em> property, which
 *            only
 *            {@link com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension} can
 *            set, rather than a user property a {@code -D} on the command line
 *            sets by itself. True for a plugin whose parameter carries no user
 *            property at all
 * @param shellEscapedFlags
 *            whether that property is parsed the way a shell would, so that a
 *            backslash escapes the character after it and has to be doubled to
 *            survive. False for a plugin that merely splits on whitespace
 * @param commaSplitFlags
 *            whether Maven splits that property's value on commas before the
 *            plugin ever sees it, which it does for a parameter declared as a
 *            {@code List<String>}. True only for a channel of that shape, and
 *            what {@link MavenGoalRuntime} answers by moving a comma-bearing
 *            flag into a JVM argument file
 * @param perPropertyFlags
 *            whether that property carries one flag rather than all of them,
 *            its name being a prefix an index is appended to so that each flag
 *            travels in a {@code -D} of its own. True only for a channel of
 *            that shape, which is Liberty's: every {@code liberty.jvm.*}
 *            property becomes one line of the server's generated
 *            {@code jvm.options}
 * @param boundInTheApplication
 *            whether the loop runs this goal by having
 *            {@link com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension}
 *            bind it to {@link #phase} in the application's module, rather than
 *            by naming it on the command line where it would run in every
 *            module. True only for an entry with no way of its own to be kept
 *            to one module - no skip to invert, no packaging check - and whose
 *            goal forks no lifecycle, since a goal bound to a phase it forks
 *            would fork itself
 * @param goalProperties
 *            properties passed on the Maven command line to keep the server in
 *            the build's own JVM and its rescanner switched off
 * @param competing
 *            configuration that would fight the dev loop if a pom pins it
 * @param serving
 *            matches the line the server logs once it is listening, capturing
 *            the bound port in group 1
 */
record ServerPlugin(String name, String groupId, String artifactId, String goal,
        String phase, String jvmFlagsProperty, boolean projectPropertyFlags,
        boolean shellEscapedFlags, boolean commaSplitFlags,
        boolean perPropertyFlags, boolean boundInTheApplication,
        Map<String, String> goalProperties, List<Competing> competing,
        Pattern serving) {

    /**
     * Where a Jetty connector announces the port it bound.
     * <p>
     * A real line reads {@code Started oejs.ServerConnector@34ab4818} followed
     * by the protocol group and then the host and port one:
     * {@code AbstractConnector} logs "Started {}" and
     * {@code AbstractNetworkConnector}'s toString ends in that second group.
     * Both groups are matched rather than skipped over with a greedy
     * {@code .*}, which is what keeps the scan free of backtracking - the class
     * name is one run up to its {@code @}, and a lookbehind asserts what that
     * run ends with.
     */
    private static final Pattern JETTY_SERVING = Pattern
            .compile("Started\\s++[^\\s{@]*+(?<=Connector)@[^\\s{]*+"
                    + "\\{[^{}]*+\\}\\{[^{}:]*+:(\\d++)\\}");

    /**
     * Where WildFly's Undertow subsystem announces the port it bound.
     * <p>
     * The message is {@code WFLYUT0006: Undertow %s listener %s listening on
     * %s:%d}, read out of {@code wildfly-undertow} itself rather than
     * remembered. Pinned to the plain HTTP listener, because an HTTPS one logs
     * the same message and the loop wants the port a browser is sent to. The
     * repetition before the port eats an IPv6 address's own colons, so the
     * captured group is the port either way.
     */
    private static final Pattern WILDFLY_SERVING = Pattern
            .compile("WFLYUT0006: Undertow HTTP listener \\S++ listening on "
                    + "(?:[^\\s:]*+:)++(\\d++)");

    /**
     * Where TomEE's Tomcat announces the port it bound.
     * <p>
     * Tomcat's own {@code abstractProtocolHandler.start=Starting
     * ProtocolHandler [{0}]}, whose argument is the endpoint name run through
     * {@code ObjectName.quote} - hence the quotes - and reads
     * {@code http-nio-8080}, or {@code http-nio-<address>-8080} when a
     * connector names one. Anchored on {@code http-} so that an AJP connector
     * starting first cannot answer for it.
     */
    private static final Pattern TOMEE_SERVING = Pattern.compile(
            "Starting ProtocolHandler \\[\"http-(?:[^\"-]*+-)*+(\\d++)\"\\]");

    /**
     * Where Liberty announces the application it deployed.
     * <p>
     * {@code CWWKT0016I: Web application available (default_host):
     * http://host:9080/ctx/}, logged once the application is installed and
     * reachable - a stronger signal than a bound socket, and the same reasoning
     * Payara Server's entry gives at greater length.
     * <p>
     * Anchored on the message <em>id</em> and on the URL rather than on the
     * words between them, which is where this entry departs from WildFly's and
     * TomEE's. Liberty ships translated message catalogues and logs in the
     * JVM's own locale, so the text between the two is not dependable; the id
     * and the URL the message is built from are. Requiring the {@code ://} is
     * also what keeps {@code CWWKF0011I}, the server-ready line that carries no
     * port at all, from answering for it.
     * <p>
     * One such line is logged per application, so a server with more than one
     * deployed is read off the first - which is the right answer either way,
     * every application on a Liberty server sharing its HTTP endpoint.
     */
    private static final Pattern LIBERTY_SERVING = Pattern
            .compile("CWWKT0016I:.*?https?://[^\\s:/]*+:(\\d++)");

    /**
     * Where Cargo announces the port the container it started is listening on.
     * <p>
     * Cargo's own line and not the container's:
     * {@code AbstractLocalContainer.start} logs
     * {@code <name> started on port [<cargo.servlet.port>]} once the container
     * has answered its ping, so the message reads e.g.
     * {@code Tomcat 10.x started on port [8080]}. Reading Cargo rather than
     * Tomcat is what makes one entry answer for every container Cargo drives,
     * and it survives a project that sends the container's own output to a file
     * with {@code <container><output>}, where Tomcat's own startup lines would
     * never reach the daemon at all.
     */
    private static final Pattern CARGO_SERVING = Pattern
            .compile("started on port \\[(\\d++)\\]");

    /**
     * Where the Payara Server plugin announces the application it deployed.
     * <p>
     * The plugin's own line and not the server's, for the reason Cargo's entry
     * gives at greater length: the domain is started with no {@code --verbose},
     * and the plugin elsewhere falls back to tailing the domain's own
     * {@code logs/server.log} - so the kernel's listener line may never reach
     * the stream the daemon reads at all. What does reach it is
     * {@code InstanceManager.deployApplication}'s
     * {@code <name> application deployed successfully : <url>}, logged only
     * after the admin endpoint has answered <em>and</em> the deployment
     * succeeded, which makes it a stronger signal than a bound socket.
     * <p>
     * The URL is built from the server's own host and port, so that is where
     * the port is read from. The same message degrades to a portless
     * {@code <name> application deployed successfully.} when the follow-up
     * {@code getApplicationInfo} call fails, and requiring the {@code ://} is
     * what stops that variant matching and being asked for a port it does not
     * carry.
     */
    private static final Pattern PAYARA_SERVING = Pattern.compile(
            "application deployed successfully : https?://[^\\s:/]*+:(\\d++)");

    /**
     * Where Payara Micro's Grizzly announces the port it bound.
     * <p>
     * {@code KernelLoggerInfo.listenerStarted}, whose message is
     * {@code Network Listener {0} started in: {1}ms - bound to [{2}]} and which
     * reads {@code Network Listener http-listener started in: 49ms - bound to
     * [/0.0.0.0:8080]}. Micro's bundled {@code domain.xml} enables only
     * {@code http-listener} and carries no admin listener at all, but the name
     * is still matched literally, so that {@code https-listener} can never
     * answer for it where a project has enabled one.
     * <p>
     * Two details shape the rest. {@code {1}} is a {@code long} put through
     * {@code MessageFormat}, so it is grouped by the JVM's locale and reads
     * {@code 5,072ms} in one and {@code 5 072ms} in another - it is stepped
     * over rather than matched. And Micro's default {@code logging.properties}
     * sets {@code ansiColor=true}, so the line carries escape sequences; they
     * wrap the level and the logger name, both of which precede the message, so
     * anchoring on the message text steps over those too.
     * <p>
     * The repetition before the port eats an IPv6 address's own colons, as
     * WildFly's does, so the captured group is the port either way.
     */
    private static final Pattern PAYARA_MICRO_SERVING = Pattern
            .compile("Network Listener http-listener started in"
                    + "[^\\[]*+\\[(?:[^\\]\\s:]*+:)++(\\d++)\\]");

    /**
     * Every server the dev loop can drive through the project's own build.
     * <p>
     * The order is the order discovery tries them in, which matters only for a
     * project declaring more than one - the CDI starter declares WildFly and
     * TomEE both. WildFly is first there because that starter's own
     * {@code <defaultGoal>} runs it; {@code -Dvaadin.dev.runtime} is how a
     * project says otherwise.
     * <p>
     * Cargo is last, and deliberately: it is the one entry that is not a server
     * but a plugin which drives any of them, and it is also the one a project
     * may well declare for its integration tests alone. A project that declares
     * both it and the container's own plugin means the latter.
     */
    static final List<ServerPlugin> KNOWN = List.of(jetty("ee10"),
            jetty("ee11"), wildfly(), tomee(), payara(), payaraMicro(),
            liberty(), cargo());

    /**
     * A configuration value the dev loop needs to hold but cannot set.
     * <p>
     * This exists because of a Maven rule with real teeth: a
     * {@code <configuration>} value in the pom beats the user property the same
     * parameter exposes, so {@code -Djetty.scan=0} does nothing to a pom that
     * writes {@code <scan>2</scan>}. The daemon cannot win that argument, so it
     * says so rather than letting two things restart the application on
     * schedules of their own.
     *
     * @param element
     *            the {@code <configuration>} element to look at
     * @param acceptable
     *            the values that leave the dev loop in sole charge
     * @param consequence
     *            what happens if the pom keeps its own value
     * @param fix
     *            what the developer should write instead
     */
    record Competing(String element, List<String> acceptable,
            String consequence, String fix) {
    }

    /**
     * Jetty 12's Maven plugin, one entry per Jakarta EE level.
     * <p>
     * {@code EMBED} is already the plugin's default deploy mode; it is passed
     * anyway, so the command line states what the loop depends on rather than
     * resting on a default that could move.
     *
     * @param ee
     *            the Jakarta EE level, as the plugin spells it
     */
    private static ServerPlugin jetty(String ee) {
        return new ServerPlugin("jetty-" + ee, "org.eclipse.jetty." + ee,
                "jetty-" + ee + "-maven-plugin", "run", "", "", false, false,
                false, false, false,
                Map.of("jetty.deployMode", "EMBED", "jetty.scan", "0"),
                List.of(new Competing("deployMode", List.of("EMBED"),
                        "the application would be a grandchild of the daemon, "
                                + "so its exit code would be lost and stopping "
                                + "it could orphan the JVM",
                        "remove <deployMode>, or set it to EMBED"),
                        new Competing("scan", List.of("0", "-1"),
                                "the plugin would redeploy the webapp on a "
                                        + "schedule of its own, competing with "
                                        + "every apply",
                                "set <scan>0</scan>")),
                JETTY_SERVING);
    }

    /**
     * WildFly's Maven plugin.
     * <p>
     * It has no embedded mode. {@code wildfly:run} provisions a server under
     * {@code target/} and starts it as a process of its own, which its
     * {@code javaHome}, {@code javaOpts} and {@code env} parameters - the last
     * documented as "passed to the process being started" - make plain. So the
     * loop's flags travel in {@code wildfly.javaOpts}, which the mojo's own
     * {@code setJavaOpts} splits on whitespace.
     * <p>
     * What it deploys is the packaged WAR, but the goal declares
     * {@code @Execute(phase = PACKAGE)} and forks the packaging itself, so no
     * phase is named here. Naming one would build the WAR twice.
     * <p>
     * That holds for a single-module project alone, and
     * {@link MavenGoalRuntime#invocation} says so: a goal's fork runs the
     * lifecycle of the application's own module, so in a reactor a sibling
     * module would stop at whatever phase the command line names, and one
     * stopped at {@code compile} reaches the WAR as a directory rather than a
     * jar.
     * <p>
     * {@code javaHome} defaults to {@code ${java.home}}, so the server runs on
     * the JVM Maven runs on, and the JDK chosen for the application - a JBR,
     * and with it enhanced class redefinition - carries over unasked.
     * <p>
     * {@code wildfly.skip} with {@code <skip>false</skip>} forced back on top
     * of it is how the goal is kept to the application's own module, the same
     * inversion Cargo and both Payaras need and for the same reason: a goal
     * named on a Maven command line runs on <em>every</em> project in the
     * reactor, and the loop names one with {@code -pl :app -am}. Jetty's mojo
     * supports {@code war} packaging alone and skips anything else;
     * {@code RunMojo} does not look at the packaging at all. It builds the
     * deployment's file name from {@code ${project.build.finalName}} and the
     * project's own packaging and fails outright when no such file exists, so
     * the reactor root ends the build with "The deployment
     * 'target/&lt;root&gt;-&lt;version&gt;.pom' could not be found" before any
     * server has started. A single-module project never shows it.
     * <p>
     * Only {@code run} is switched off. The {@code provision} goal an EAP
     * project binds to its own build reads {@code wildfly.provision.skip}
     * instead, so a server is still provisioned where one is asked for.
     */
    private static ServerPlugin wildfly() {
        return new ServerPlugin("wildfly", "org.wildfly.plugins",
                "wildfly-maven-plugin", "run", "", "wildfly.javaOpts", false,
                false, false, false, false, Map.of("wildfly.skip", "true"),
                List.of(new Competing("skip", List.of("false"),
                        "the run goal would start a server for every module in "
                                + "the reactor, or fail on the first one whose "
                                + "packaging builds no deployment",
                        "remove <skip>, or set it to false"),
                        new Competing("javaOpts", List.of(),
                                "the agents the loop needs would be dropped, "
                                        + "and every apply would restart "
                                        + "instead of hot reloading",
                                "remove <javaOpts>; the dev loop needs that "
                                        + "parameter for its agents")),
                WILDFLY_SERVING);
    }

    /**
     * TomEE's Maven plugin.
     * <p>
     * A fork as well: {@code tomee:run} starts the server through
     * {@code RemoteServer}, and {@code args} - parsed straight into the forked
     * JVM's argument list - is where the loop's flags go. {@code javaagents}
     * would read better and cannot be used: it carries no user property, so no
     * command line can set it.
     * <p>
     * It deploys the packaged WAR and, unlike WildFly's, its goal forks no
     * lifecycle of its own, so {@code package} has to be asked for.
     * <p>
     * Its {@code Args.parse} reads the value the way a shell would, so a
     * backslash escapes whatever follows it and is then dropped. Measured on
     * Windows, {@code C:\\Users\\...\\hotswap-agent.jar} reached the JVM as
     * {@code C:Users...hotswap-agent.jar} and the JVM refused to start - hence
     * {@code shellEscapedFlags}. Quoting is no help: the parser checks for an
     * escape before it checks for a quote, so a backslash inside quotes is
     * eaten just the same.
     * <p>
     * {@code reloadOnUpdate} is TomEE's own redeploy-on-change, the second
     * driver of restarts that {@code jetty.scan} is for Jetty, and a generated
     * starter turns it on alongside a {@code <synchronization>} that copies
     * classes into the deployed webapp. Switching the reload off is what leaves
     * the loop in sole charge; the copying on its own changes nothing.
     * <p>
     * It is also the one entry whose goal is never named on a command line, and
     * this is what that costs when it is. Measured on a three-module reactor,
     * {@code mvn -pl :app -am package tomee:run} ran the goal on the reactor
     * <em>root</em> first - the goal runs in every project, and {@code -pl}
     * chooses which projects are in the reactor, not which of them it runs in.
     * {@code AbstractTomEEMojo.execute} never looks at packaging: it unzipped a
     * TomEE under the root's {@code target/}, started it and blocked. The build
     * never reached modules 2 and 3, nothing was deployed, and nothing failed -
     * so the daemon waits out its whole start window and abandons a launch that
     * was never going to happen.
     * <p>
     * The inversion that answers this for Cargo, WildFly and both Payaras needs
     * a {@code skip} to invert, and this mojo has none:
     * {@code skipCurrentProject} is the only parameter that sounds like one and
     * it guards {@code copyWar} alone, which would leave the root's TomEE
     * running and merely emptier. What answers it instead is
     * {@link #boundInTheApplication} - the command line names {@code package}
     * and no goal, and the extension adds {@code run} to this plugin's
     * executions in the application's module, bound to that same phase, where
     * it runs after the {@code war:war} that builds what it deploys. Binding is
     * not the answer for the rest of the table because {@code tomee:run} forks
     * no lifecycle: a goal declaring {@code @Execute(phase = PACKAGE)}, as
     * WildFly's does, would fork the phase it was bound to.
     */
    private static ServerPlugin tomee() {
        return new ServerPlugin("tomee", "org.apache.tomee.maven",
                "tomee-maven-plugin", "run", "package", "tomee-plugin.args",
                false, true, false, false, true, Map.of(),
                List.of(new Competing("reloadOnUpdate", List.of("false"),
                        "the plugin would redeploy the webapp whenever its "
                                + "synchronization copied a class, competing "
                                + "with every apply",
                        "set <reloadOnUpdate>false</reloadOnUpdate>"),
                        new Competing("args", List.of(),
                                "the agents the loop needs would be dropped, "
                                        + "and every apply would restart "
                                        + "instead of hot reloading",
                                "remove <args>; the dev loop needs that "
                                        + "parameter for its agents")),
                TOMEE_SERVING);
    }

    /**
     * Payara Server's Maven plugin.
     * <p>
     * A fork like WildFly, TomEE and Cargo, but a well-behaved one: the mojo
     * builds the domain's command line itself and starts it with
     * {@code ProcessBuilder} rather than shelling out to
     * {@code asadmin start-domain}, so nothing stands between the flags and the
     * server. {@code daemon} defaults to {@code false}, and the goal then runs
     * the launch on Maven's own thread and ends in {@code Process.waitFor()} -
     * which is what makes it a goal the daemon can own. It declares no
     * {@code @Execute}, so {@code package} has to be asked for.
     * <p>
     * {@code start} and never {@code dev}: {@code DevMojo} forces
     * {@code autoDeploy}, {@code liveReload}, {@code keepState},
     * {@code trimLog} and {@code aiAgent} on. The first is a watcher that
     * re-invokes Maven and redeploys - the same competing rebuilder
     * {@code jetty.scan} and TomEE's {@code reloadOnUpdate} are switched off
     * for; {@code trimLog} rewrites every line the server logs, which is the
     * stream {@link #PAYARA_SERVING} is read from; and {@code aiAgent} turns
     * the process into a prompt reading standard input. {@code start} defaults
     * all five off, so the entries below only hold a pom that asks for them
     * back.
     * <p>
     * The flags travel in {@code payara.javaCommandLineOptions}, and this is
     * the first forked container whose channel a pom cannot take away: that
     * user property feeds a second field which the mojo <em>appends</em> to the
     * {@code <javaCommandLineOptions>} a pom writes, rather than being the same
     * parameter. So unlike WildFly's {@code <javaOpts>} and TomEE's
     * {@code <args>}, no entry here has to be left with no acceptable value.
     * <p>
     * Two properties of that channel are load-bearing, and neither is obvious.
     * <p>
     * It is declared {@code List<String>}, and Maven splits a
     * {@code List<String>} user property on commas - a bare comma split in
     * Plexus's {@code AbstractCollectionConverter}, with no escaping of any
     * kind. The mojo then turns each element into a key and a value at the
     * first {@code =} and <em>drops any element that has none</em>, in silence.
     * So a comma does not merely split the value, it deletes most of it: the
     * loop's own {@code -DdisabledPlugins=Vaadin,Spring,SpringBoot,Jetty} would
     * arrive as {@code -DdisabledPlugins=Vaadin} and nothing would say so. That
     * is what {@link #commaSplitFlags} is for.
     * <p>
     * The same drop rule would take {@code -XX:+AllowEnhancedClassRedefinition}
     * with it, that flag carrying no {@code =} - and with it enhanced class
     * redefinition, which is the whole reason a JBR is chosen. It survives
     * because the flags are handed over as one whitespace-separated value: the
     * split at the first {@code =} and the {@code key=value} that rebuilds it
     * are exact inverses, so the value round-trips byte for byte and
     * {@code JavaUtils.parseParameters} tokenizes it back into separate
     * arguments at the far end. The one requirement is that the value contain
     * an {@code =} somewhere, which the loop's settings always do.
     * <p>
     * That parser treats a backslash as an escape, but a forgiving one: only a
     * quote and another backslash mean anything after it, and a backslash
     * before any other character is kept as it stands. A Windows path therefore
     * arrives intact and must <em>not</em> be doubled the way TomEE's has to
     * be, which is why {@link #shellEscapedFlags} is false here.
     */
    private static ServerPlugin payara() {
        return new ServerPlugin("payara", "fish.payara.maven.plugins",
                "payara-server-maven-plugin", "start", "package",
                "payara.javaCommandLineOptions", false, false, true, false,
                false, Map.of("skip", "true"),
                List.of(new Competing("skip", List.of("false"),
                        "the start goal would run on every module in the "
                                + "reactor, and the first of them - the "
                                + "reactor root - would start a server with "
                                + "the application deployed in it nowhere",
                        "remove <skip>, or set it to false"),
                        new Competing("daemon", List.of("false"),
                                "the goal would return as soon as the server had "
                                        + "started, leaving the daemon owning a Maven "
                                        + "that had already exited",
                                "remove <daemon>, or set it to false"),
                        new Competing("autoDeploy", List.of("false"),
                                "the plugin would rebuild and redeploy the "
                                        + "application on a schedule of its "
                                        + "own, competing with every apply",
                                "set <autoDeploy>false</autoDeploy>"),
                        new Competing("liveReload", List.of("false"),
                                "the plugin would rewrite every line the "
                                        + "server logs and refresh the browser "
                                        + "itself, so the loop could neither "
                                        + "read the server's output nor decide "
                                        + "when a change goes live",
                                "set <liveReload>false</liveReload>"),
                        new Competing("aiAgent", List.of("false"),
                                "the plugin would read from standard input and "
                                        + "write escape sequences into the "
                                        + "application's log",
                                "set <aiAgent>false</aiAgent>")),
                PAYARA_SERVING);
    }

    /**
     * Payara Micro's Maven plugin.
     * <p>
     * The same shape as Payara Server's - a fork, blocking on
     * {@code Process.waitFor()} with {@code daemon} false, declaring no
     * {@code @Execute} so {@code package} is asked for, and {@code start}
     * rather than the {@code dev} whose {@code autoDeploy} watcher would fight
     * every apply.
     * <p>
     * What differs is the channel. No parameter that could carry the loop's
     * flags has a user property - {@code <javaCommandLineOptions>} is a nested
     * list settable from a pom alone, which is exactly Cargo's problem - but
     * the mojo also reads {@code exec.args} straight off the session's user
     * properties and splices its tokens in ahead of {@code -jar}, so a
     * {@code -D} on the command line reaches the Micro JVM as true JVM
     * arguments. It is read from the user properties rather than from the
     * model, so unlike every other entry here a pom cannot override it even by
     * writing an {@code exec.args} property of its own.
     * <p>
     * That it is undocumented is worth recording: there is no constant behind
     * it, the literal name is inline in the mojo, and a Payara release that
     * dropped it would leave the server starting with no agents and every apply
     * restarting. The value is echoed in the launch line the daemon logs, which
     * is where to look first.
     * <p>
     * Being a plain {@code String} rather than a {@code List<String>}, it
     * reaches the plugin exactly as written - no comma splitting, and hence no
     * {@link #commaSplitFlags} - and it is split on whitespace alone, with no
     * quoting and no escape character at all. So a Windows path passes through
     * as it stands, and one containing a space cannot be passed at all;
     * {@code MavenGoalRuntime.unsplittable} already says so.
     * <p>
     * {@code deployWar} defaults to <em>false</em> and it is {@code DevMojo}
     * that turns it on, so a project whose pom relies on
     * {@code payara-micro:dev} declares it nowhere and {@code start} would
     * bring up a server with nothing deployed to it. Hence the goal property,
     * and the entry forcing it on for a pom that pins it off - the same
     * inversion Cargo's {@code <skip>} uses.
     */
    private static ServerPlugin payaraMicro() {
        return new ServerPlugin("payara-micro", "fish.payara.maven.plugins",
                "payara-micro-maven-plugin", "start", "package", "exec.args",
                false, false, false, false, false,
                Map.of("payara.skip", "true", "payara.deploy.war", "true"),
                List.of(new Competing("skip", List.of("false"),
                        "the start goal would run on every module in the "
                                + "reactor, and the first of them - the "
                                + "reactor root - would start a server with "
                                + "the application deployed in it nowhere",
                        "remove <skip>, or set it to false"),
                        new Competing("deployWar", List.of("true"),
                                "the goal would start a server with the "
                                        + "application deployed nowhere in it",
                                "set <deployWar>true</deployWar>"),
                        new Competing("daemon", List.of("false"),
                                "the goal would return as soon as the server "
                                        + "had started, and would stop "
                                        + "forwarding its log at the same "
                                        + "moment",
                                "remove <daemon>, or set it to false"),
                        new Competing("autoDeploy", List.of("false"),
                                "the plugin would rebuild and redeploy the "
                                        + "application on a schedule of its "
                                        + "own, competing with every apply",
                                "set <autoDeploy>false</autoDeploy>"),
                        new Competing("liveReload", List.of("false"),
                                "the plugin would rewrite every line the "
                                        + "server logs and refresh the browser "
                                        + "itself, so the loop could neither "
                                        + "read the server's output nor decide "
                                        + "when a change goes live",
                                "set <liveReload>false</liveReload>")),
                PAYARA_MICRO_SERVING);
    }

    /**
     * Open Liberty's Maven plugin.
     * <p>
     * A fork, like the four entries above it: {@code embedded} defaults to
     * {@code false}, so {@code liberty:run} starts the server as a process of
     * its own and blocks on it. It declares no {@code @Execute} and still needs
     * no phase, which is a combination none of the others has -
     * {@code RunServerMojo} runs {@code resources}, {@code compiler:compile}
     * and, for a WAR it is to package, {@code war:war} itself. Naming
     * {@code package} would only build the WAR twice.
     * <p>
     * It is also the one forked container that keeps itself to the
     * application's own module unasked: the mojo reads the session's
     * {@code ProjectDependencyGraph}, runs the server on the farthest
     * downstream project alone, merely compiles the rest and skips {@code pom}
     * packaging outright. So none of the switch-the-goal-off-and-force-it-back
     * machinery Cargo and both Payaras need appears here.
     * <p>
     * The JVM it runs on is Maven's: the plugin sets {@code JAVA_HOME} for the
     * server only when a Maven toolchain names one, and otherwise the process
     * inherits the environment Maven was started with - where the daemon has
     * already put the JDK {@link Jvm} chose. The JBR carries over unasked, as
     * it does for WildFly and Cargo.
     * <p>
     * The channel is {@code liberty.jvm.<key>}, and it is a shape none of the
     * others has. Every Maven project <em>or system</em> property with that
     * prefix becomes one line of the server's generated {@code jvm.options}, so
     * a {@code -D} on Maven's command line does reach the server's JVM - but
     * each property carries one flag rather than all of them, which is what
     * {@link #perPropertyFlags} says and what
     * {@code MavenGoalRuntime.flagProperties} answers.
     * <p>
     * Two properties of that channel matter. A line is taken whole, so a comma
     * in it divides nothing and a backslash escapes nothing - hence neither
     * {@link #commaSplitFlags} nor {@link #shellEscapedFlags}. And the
     * properties are read out of a {@code Properties}, whose iteration order is
     * unspecified, so the lines may be written in any order at all: that is a
     * second and sharper reason for {@code MavenGoalRuntime.singleToken} to
     * fold a module option onto its value, WildFly's sorting being only the
     * first.
     * <p>
     * A pom cannot take the channel away, which puts Liberty with the Payaras
     * rather than with WildFly and TomEE: the generated file overwrites any
     * {@code jvm.options} the project supplies, and {@code writeJvmOptions}
     * appends a pom's own {@code <jvmOptions>} <em>after</em> the Maven
     * properties rather than instead of them.
     * <p>
     * {@code looseApplication} is forced off, and that is the load-bearing
     * setting here. Liberty's default deploys a loose-application XML pointing
     * straight at {@code target/classes}, and Liberty's own
     * {@code applicationMonitor} defaults to {@code updateTrigger="polled"} -
     * so the server would restart the application under every apply, which is
     * the competing rebuilder {@code jetty.scan} and TomEE's
     * {@code reloadOnUpdate} are switched off for. That one cannot be switched
     * off from the pom at all: it lives in the project's {@code server.xml},
     * where nothing in this table can see it. A packaged WAR does not change
     * between restarts, so the monitor has nothing to react to and the loop is
     * in sole charge without the project having to write anything. What it
     * costs is what every other forked container already costs - a class the
     * application has not loaded yet reads its pre-edit bytes from the deployed
     * copy until the next restart.
     * <p>
     * {@code embedded} is listed as competing but not passed as a {@code -D}:
     * {@code false} is already the default, and the property is generic enough
     * that setting it over the whole reactor would be worse than the warning. A
     * pom that turned it on would run the server in Maven's own JVM, where the
     * {@code jvm.options} just written is never read and the agents would be
     * dropped in silence.
     */
    private static ServerPlugin liberty() {
        return new ServerPlugin("liberty", "io.openliberty.tools",
                "liberty-maven-plugin", "run", "", "liberty.jvm.devloop", false,
                false, false, true, false, Map.of("looseApplication", "false"),
                List.of(new Competing("looseApplication", List.of("false"),
                        "Liberty's own application monitor polls the deployed "
                                + "application and would restart it whenever a "
                                + "class under target/classes changed, "
                                + "competing with every apply",
                        "set <looseApplication>false</looseApplication>"),
                        new Competing("embedded", List.of("false"),
                                "the server would run in Maven's own JVM, "
                                        + "where the jvm.options carrying the "
                                        + "loop's agents is never read, so "
                                        + "every apply would restart instead "
                                        + "of hot reloading",
                                "remove <embedded>, or set it to false")),
                LIBERTY_SERVING);
    }

    /**
     * Codehaus Cargo's Maven plugin, which is how the loop runs Apache Tomcat.
     * <p>
     * Tomcat has no Maven plugin of its own that a Vaadin project could use:
     * {@code tomcat7-maven-plugin} is the only one Apache ever published, it
     * was last released in 2013, and it runs a {@code javax.servlet} container
     * that a Jakarta EE application cannot be deployed to at all. Cargo is what
     * the ecosystem uses instead - this repository's own servlet-container
     * tests included - and one entry for it covers not only Tomcat but every
     * other container Cargo drives, which is why the entry is named after the
     * plugin rather than after Tomcat.
     * <p>
     * A fork, like WildFly and TomEE: {@code cargo:run} installs a container
     * under {@code target/}, deploys the packaged WAR into it and starts it as
     * a process of its own. Its mojo declares no {@code @Execute} of any kind,
     * so unlike WildFly's it forks no lifecycle and {@code package} has to be
     * asked for.
     * <p>
     * What is new here is where the flags go. Every parameter of the run mojo
     * that could carry them - {@code <container>}, {@code <configuration>} - is
     * a nested element with no user property behind it, so no {@code -D} on a
     * command line reaches one. What Cargo does read is any Maven <em>project
     * property</em> whose name begins with {@code cargo.}, which it injects as
     * a container configuration property and which it applies after the pom's
     * own {@code <properties>}; so the property is set on the model by
     * {@link com.vaadin.flow.devloop.mavenext.DevLoopBuildExtension} instead,
     * and {@link #projectPropertyFlags} is what says so.
     * <p>
     * {@code cargo.jvmargs} and not {@code cargo.start.jvmargs}, which is what
     * this entry asked for until a container that is not Tomcat was tried
     * against it. {@code AbstractInstalledLocalContainer} reads both and
     * appends each to the command line of the JVM it launches, so for Tomcat -
     * where that JVM <em>is</em> the container - either name works and the
     * start-only one leaves a project's own heap settings alone. Cargo's
     * GlassFish family is not that shape: there the JVM Cargo launches is the
     * {@code asadmin} client, and the server is a process asadmin starts in
     * turn. {@code AbstractGlassFishInstalledLocalContainer.startInternal}
     * therefore takes {@code cargo.jvmargs} away from asadmin on purpose
     * (CARGO-1255) and
     * {@code AbstractGlassFishStandaloneLocalConfiguration.doConfigure} writes
     * it into the domain's {@code domain.xml} as {@code <jvm-options>} instead,
     * while {@code cargo.start.jvmargs} gets no such treatment and reaches the
     * asadmin client alone. So the loop's agents were loaded into a
     * command-line tool that exits, the application ran without them, and every
     * apply restarted with nothing to say why. One name is correct for both
     * families and this is it.
     * <p>
     * What that costs is the thing the other name was chosen to avoid: a
     * project may well write {@code cargo.jvmargs} for itself. So
     * {@code DevLoopBuildExtension} appends to a project property it finds
     * rather than replacing it, the loop's flags last, which is the precedence
     * {@code MavenGoalRuntime.mavenOpts} already applies to {@code MAVEN_OPTS}
     * - a project's heap setting is honoured and the agents cannot be switched
     * off by one.
     * <p>
     * Cargo parses the value with its own copy of Ant's
     * {@code translateCommandline}: whitespace separates, quotes group, and a
     * backslash is an ordinary character - so a Windows path needs no escaping
     * here, unlike TomEE's.
     * <p>
     * {@code cargo.java.home} defaults to the {@code java.home} of the JVM
     * Maven runs on, so the JDK chosen for the application carries over
     * unasked, exactly as WildFly's {@code javaHome} does.
     * <p>
     * {@code cargo.maven.skip} with {@code <skip>false</skip>} forced back on
     * top of it is how the goal is kept to the application's own module. A goal
     * named on a Maven command line runs on <em>every</em> project in the
     * reactor, and the loop's is named with {@code -pl :app -am} so that a
     * sibling module is built in the same session - which for Jetty is
     * harmless, because its mojo supports {@code war} packaging alone and skips
     * anything else. Cargo's does not: measured against this repository's own
     * multi-module fixture it ran first on the reactor root and failed the
     * build with "For all packaging other than war you need to configure the
     * container you wish to use", before anything had started. So the goal is
     * switched off for the whole reactor by its user property, and switched
     * back on for the one module that declares the plugin by a
     * {@code <configuration>} value - which is the very Maven rule
     * {@link Competing} exists for, used the other way round.
     * <p>
     * Nothing else is listed as competing. Cargo has no rescanner of its own -
     * Tomcat's {@code reloadable} is off unless a project asks for it - and the
     * one setting the loop does take over, {@code cargo.jvmargs}, lives too
     * deep in the plugin's configuration for the daemon to see whether a pom
     * pins it. What is worth warning about there is the daemon having no
     * extension to set it with at all, and {@code MavenGoalRuntime} says that.
     */
    private static ServerPlugin cargo() {
        return new ServerPlugin("cargo", "org.codehaus.cargo",
                "cargo-maven3-plugin", "run", "package", "cargo.jvmargs", true,
                false, false, false, false, Map.of("cargo.maven.skip", "true"),
                List.of(new Competing("skip", List.of("false"),
                        "the run goal would start a container for every module "
                                + "in the reactor, or fail on the first one "
                                + "that is not a WAR",
                        "remove <skip>, or set it to false")),
                CARGO_SERVING);
    }

    /**
     * Whether this server runs the application in the build's own JVM.
     *
     * @return {@code true} when {@code MAVEN_OPTS} carries the loop's JVM
     *         flags, {@code false} when {@link #jvmFlagsProperty} does
     */
    boolean embedded() {
        return jvmFlagsProperty.isBlank();
    }

    /**
     * Whether this entry switches its own goal off for the whole reactor and
     * relies on a forced {@code <skip>false</skip>} to switch it back on for
     * the one module that declares the plugin.
     * <p>
     * A goal named on a Maven command line runs on <em>every</em> project in
     * the reactor, and the loop names one with {@code -pl :app -am} so that a
     * sibling module builds in the same session. Jetty's mojo supports
     * {@code war} packaging alone and skips the rest, so it never noticed;
     * Cargo, Payara Server, Payara Micro and WildFly all do notice. Measured
     * against this repository's own multi-module fixture,
     * {@code payara-micro:start} ran first on the reactor root, started a
     * Payara Micro there, reported {@code Deployed 0 archive(s)} and blocked
     * the reactor before the application module was ever built - the same shape
     * Cargo failed in, and the reason this is a property of the table rather
     * than of one entry.
     * <p>
     * Read off {@link #competing} rather than stored, because the two halves
     * are already there: the {@code skip} the goal properties switch on, and
     * the {@code false} the forced configuration puts back. An entry that had
     * only one half would be worse than neither.
     *
     * @return {@code true} when the goal is switched off outside the
     *         application's own module
     */
    boolean skippedOutsideTheApplication() {
        return competing.stream()
                .anyMatch(value -> "skip".equals(value.element())
                        && value.acceptable().contains("false"));
    }

    /**
     * Which of {@link #goalProperties} is the skip half.
     * <p>
     * The map is not all one thing. Payara Micro's carries
     * {@code payara.deploy.war} beside its skip, and that one has nothing to do
     * with the reactor: {@code deployWar} defaults to {@code false} on
     * {@code start}, so without it the server comes up with the application
     * deployed in it nowhere. {@link MavenGoalRuntime} drops the skip when
     * there is no extension to switch it back on, and has to be able to drop
     * that half alone.
     * <p>
     * Read off the names rather than stored, the way
     * {@link #skippedOutsideTheApplication} is read off {@link #competing}: the
     * property that switches the goal off is the one named after the
     * {@code skip} parameter it sets, whatever prefix its plugin gives it -
     * {@code skip}, {@code wildfly.skip}, {@code payara.skip},
     * {@code cargo.maven.skip}.
     *
     * @return the property name, or empty for an entry that switches its goal
     *         off nowhere
     */
    Optional<String> skipProperty() {
        if (!skippedOutsideTheApplication()) {
            return Optional.empty();
        }
        return goalProperties.keySet().stream()
                .filter(name -> "skip".equals(name) || name.endsWith(".skip"))
                .findFirst();
    }

    /**
     * The configuration the dev loop needs, as {@code element=value} pairs for
     * {@code DevLoopBuildExtension}.
     * <p>
     * Derived from {@link #competing} rather than written out again: those are
     * exactly the values a pom must not be allowed to keep, and the first
     * acceptable one is the value to force.
     *
     * @return the pairs, semicolon-separated
     */
    String forcedConfiguration() {
        StringBuilder forced = new StringBuilder();
        for (Competing value : competing) {
            // An entry with no acceptable value is a warning and nothing more.
            // There is no constant to force: the value it guards - the agents
            // for this launch - is composed per run and cannot be tabulated.
            if (value.acceptable().isEmpty()) {
                continue;
            }
            if (!forced.isEmpty()) {
                forced.append(';');
            }
            forced.append(value.element()).append('=')
                    .append(value.acceptable().get(0));
        }
        return forced.toString();
    }

    /**
     * How the goal is named on a command line, pinned to the version the
     * project declares when it declares one.
     *
     * @param declared
     *            the plugin as the project's poms declare it
     * @return the goal specification, e.g.
     *         {@code org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.1.13:run}
     */
    String goalSpecification(Reactor.PluginConfig declared) {
        return declared.coordinates() + ":" + goal;
    }
}

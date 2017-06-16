[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin-flow/Lobby#?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Flow
======
*[Flow](https://vaadin.com) is a Java framework for building modern web sites that look great, perform well and make you and your users happy.*

For instructions about _using_ Flow to develop applications, please refer to
https://github.com/vaadin/flow/tree/master/flow-documentation

To contribute, first refer to [Contribution Guide](/CONTRIBUTING.md)
for general instructions and requirements for contributing code to Flow.

Flow EAP discussion in Gitter IM at https://gitter.im/vaadin-flow/Lobby

Instructions on how to set up a working environment for developing the Flow project follow below.

Quick Setup
======
To create your own application using Flow, use https://github.com/vaadin/flow-hello-world as a starting point.

To quickly get started with contributing, follow these simple steps.
1. <code>git clone https://github.com/vaadin/flow.git</code>
1. make sure you have a TestBench license (get it otherwise on https://vaadin.com/pro/licenses)
1. <code>mvn install</code>

For more details, see below

Setting up Eclipse to Develop Flow
=========

Import the Project into the Workspace
------------
1. Do *File* -> *Import* -> *General* -> *Existing Maven Project*
1. Select the *flow* folder (where you cloned the project)
1. Ensure all projects are checked
1. Click “finish” to complete the import
1. Disable HTML and XML validation in the workspace to avoid validating Bower dependencies
 1. Eclipse preferences -> *Validation*
 1. Uncheck *Build* for *HTML Syntax Validator*
 1. Uncheck *Build* for *XML Validator*


Note that the first compilation takes a while to finish as Maven downloads dependencies used in the projects.

Compiling the Client Engine
--------
Compile the client engine by executing the eclipse build configuration *Compile ClientEngine* in *flow-client/eclipse*

Set up extra workspace preferences
--------
The following preferences need to be set to keep the project consistent. You need to do this especially to be able to contribute changes to the project.

1. Open *Window* -> *Preferences* (Windows) or *Eclipse* -> *Preferences* (Mac)
1. Go to *General* ->  *Workspace*
 1. Set *Text file encoding* to *UTF-8*
 1. Set *New text file line delimiter* to *Unix*
1. Go to XML -> XML Files -> Editor
 1. Ensure the settings are follows:
<pre><code>Line width: 72
Format comments: true
Join lines: true
Insert whitespace before closing empty end-tags: true
Indent-using spaces: true
Indentation size: 4
</code></pre>

Running tests
=====
The unit tests for the projects can be run using
<pre><code>mvn test</code></pre>

IT tests can be run with
<pre><code>mvn verify</code></pre>

To run IT tests locally, you'll need a [Testbench](https://vaadin.com/testbench) license and a Chrome browser installed (currently this is the only browser that IT tests are run in).
IT tests are run in parallel, one thread per CPU core with thread limit = 5 (see `maven-failsafe-plugin` configuration in root [pom.xml](./pom.xml) for details).

When running IT tests locally, by default, local Chrome is used to run tests.
The other opportunity is to run tests using test hub, to do so, you need to start a hub on a `localhost:4444` address.
This can be done with [Selenoid](https://github.com/aerokube/selenoid), for instance.

Normally, you don't need to run IT tests yourself, Travis can do this for you. See next section for details.

Remote tests execution
--------
Each pull request requires validation via Travis, so before the pull request is merged
all the tests are executed remotely by Travis CI server. 
If you want to run the tests remotely for your branch but don't want to create a PR for your patch then you can use
instructions from [here](https://docs.travis-ci.com/user/triggering-builds).
There is a script <code>scripts/runTests.sh</code> which you can use to run validation tests with various options.
To be able to use the script you need:
1. *nix system or CygWin with <code>curl</code> command installed
1. Read the link [above](https://docs.travis-ci.com/user/triggering-builds): you need an API token to be able to access to Travis API.
1. Once you get your API token you can run the script via <code>runTests.sh -branch yourBranch -token yourAPItoken</code>

The command requests Travis to execute validation tests in your branch which has to be specified as a parameter.
In addition to required parameters you can provide:
1. A commit message using <code>-message commitMessage</code>. This message will appear in the Travis Job. So you can identify your job using the message.
1. The parameter <code>-javadoc</code>. The default validation executes only tests without checking javadocs.   <code>-javadoc</code> parameter requests javadoc validation in addition to the tests.
1. The parameter <code>-all-tests</code>. Some tests are excluded from validation for various reasons (e.g. slow tests, etc.). This parameter requests all tests execution (not only validation).
1. The parameter <code>-sonar</code>. In addition to validation (running the tests) it requests Sonar Qube sources analysis.
1. The parameter <code>-sonaronly</code>. This parameter requests Sonar Qube analysis only (without tests).

Building a package
=====
The distribution package is built and installed into the local Maven repository by doing

1. mvn install

Running SuperDevMode
=====

To start superDevMode do to the flow-client package and run the maven command:

1. mvn -Psdm clean install gwt:compile gwt:run-codeserver

In eclipse run .launch files from flow-client/eclipse in the order:

1. Compile ClientEngine.launch
2. Super Dev Mode.launch

Navigate to [localhost:9876](localhost:9876) and use the bookmarks to control
dev mode.

> NOTE! SuperDevMode should be compiled before the application server is launched
> as else the application won't be able to run SDM and you will receive the 
> exception `Can't find any GWT Modules on this page.`

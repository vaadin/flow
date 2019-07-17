[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin/flow#?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Vaadin Flow
======
*[Vaadin Flow](https://vaadin.com/flow) is the Java framework of Vaadin Platform for building modern web sites that look great, perform well and make you and your users happy.*

**For instructions about developing web applications with Vaadin Flow**, please refer to [the starter packs for Vaadin 14 with Flow](https://vaadin.com/start) or [the documentation](https://vaadin.com/docs/flow/Overview.html).

To contribute, first refer to [Contribution Guide](/CONTRIBUTING.md)
for general instructions and requirements for contributing code to Flow.

Flow EAP discussion in Gitter IM at https://gitter.im/vaadin-flow/Lobby

Instructions on how to set up a working environment for developing the Flow project follow below.

`master` branch is the latest version that will at some point be released in the [Vaadin platform 14.0.0](https://github.com/vaadin/platform). 
See other branches for other framework versions:
* 1.0 branch is Vaadin 10 LTS (Flow version 1.0)
* 1.4 branch is for Vaadin 13 (Flow version 1.4)

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
Compile the client engine by executing the Eclipse build configuration *Compile ClientEngine* in *flow-client/eclipse*

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
If you don't have the license, it's ok, our CI system will run those tests for you after you create a pull request. 
Refer to the [contribution guide](/CONTRIBUTING.md) for details. 

When running IT tests locally, by default, local Chrome is used to run tests, make sure it's installed.

Building a package
=====
The distribution package is built and installed into the local Maven repository by doing

1. mvn install

Running SuperDevMode
=====
Some flow internals use GWT in the client code. superDevMode allows to reload GWT changes on the fly, but it requires some setup first.

To start superDevMode do the following:

1. Get flow source code
1. If you are planning to launch the mode for the external application based on flow, first make sure that flow source code is of the same version as the application uses.
If it's not true, either update the application dependencies or check out the corresponding flow tag and rebuild both flow and the application.
1. Navigate to flow-client package in flow project
1. Run `mvn -Psdm clean install gwt:compile gwt:run-codeserver -DskipTests`
1. Start the application server
1. Open the application page and use the bookmarks to control dev mode
If you have no bookmarks, navigate to http://localhost:9876 to setup them.

In eclipse run .launch files from flow-client/eclipse in the order:

1. Compile ClientEngine.launch
2. Super Dev Mode.launch

> NOTE! SuperDevMode should be compiled before the application server is launched,
> also, flow version should match with the application one
> as else the application won't be able to run SDM and you will receive the
> exception `Can't find any GWT Modules on this page.`

More info about SuperDevMode: http://www.gwtproject.org/articles/superdevmode.html


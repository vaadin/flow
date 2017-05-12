Flow
======
*[Flow](https://vaadin.com) is a Java framework for building modern web sites that look great, perform well and make you and your users happy.*

For instructions about _using_ Flow to develop applications, please refer to
https://vaadin.com/

To contribute, first refer to https://vaadin.com/wiki/-/wiki/Main/Contributing+Code
for general instructions and requirements for contributing code to Flow.

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

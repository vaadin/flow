Hummingbird
======
*[Hummingbird](https://vaadin.com) is a Java framework for building modern web sites that look great, perform well and make you and your users happy.*

For instructions about _using_ Hummingbird to develop applications, please refer to
https://vaadin.com/

To contribute, first refer to https://vaadin.com/wiki/-/wiki/Main/Contributing+Code
for general instructions and requirements for contributing code to Hummingbird.

Instructions on how to set up a working environment for developing the Hummingbird project follow below.

Quick Setup
======
1. <code>git clone https://github.com/vaadin/hummingbird.git</code>
1. install phantomjs (version 2.1 or newer is required, make sure it's in the PATH)
1. make sure you have a TestBench license (get it otherwise on https://vaadin.com/pro/licenses) 
1. <code>mvn install</code>

For more details, see below

Setting up Eclipse to Develop Hummingbird
=========

Import the Project into the Workspace
------------
1. Do *File* -> *Import* -> *General* -> *Existing Maven Project*
1. Select the *hummingbird* folder (where you cloned the project)
1. Ensure all projects are checked
1. Click “finish” to complete the import

Note that the first compilation takes a while to finish as Maven downloads dependencies used in the projects.

Compiling the Client Engine
--------
Compile the client engine by executing the eclipse build configuration *Compile ClientEngine* in *hummingbird-client/eclipse*

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

# Vaadin Flow WebComponent API Analyzer

Vaadin Flow WebComponent API Analyzer is a tool that analyzes Web Components as input and outputs JSON data that can be used by a Java code generation tool that produces Flow Component APIs.

Currently the Analyzer supports only Web Components that the [Polymer Analyzer 2.0]( https://github.com/Polymer/polymer-analyzer) understands.

## Installation and Usage

### Maven Usage

Clone this repository, add your _bower.json_ file to the root folder and run
```shell
mvn install -P generator
```

This will download the needed dependencies to /bower-components/ and then generate the JSON files to `/generated-json/` folder.
Given the target folder or WCs to analyze as parameters is not currently supported.

### Local Node Installation

Install node and npm, clone this repository and in the root folder to install the packages locally run:
```shell
$ npm link
```
> If you've installed node and npm using `sudo`, installing packages globally will also require you to use `sudo`. See [http://givan.se/do-not-sudo-npm/](http://givan.se/do-not-sudo-npm/) how to remedy the situation.

Generating java code for bower packages installed in your bower_components folder

  ```shell
  $ bower install my-web-component
  $ flow-webcomponent-api-analyzer
  ```
Generating java code for a complete library

  ```shell
  $ flow-webcomponent-api-analyzer --package=PolymerElements/paper-elements
  ```

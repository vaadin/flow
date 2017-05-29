# Vaadin gwt-api-generator

Vaadin gwt-api-generator is a tool that produces GWT Java APIs for JavaScript libraries provided as input, assuming their APIs are decorated with JSDoc annotations.

Currently the generator only supports Web Components written in Polymer 1.0 syntax. Support for other type of JavaScript sources might be added in the future.

Original motivation behind the project was to provide GWT community an easy access to the elements in [Vaadin Elements](https://github.com/vaadin/vaadin-elements) library.

## Installation and Usage

- Installation
```shell
$ npm install -g vaadin/gwt-api-generator
```
> If you've installed node and npm using `sudo`, installing packages globally will also require you to use `sudo`. See [http://givan.se/do-not-sudo-npm/](http://givan.se/do-not-sudo-npm/) how to remedy the situation.

- Generating java code for bower packages installed in your bower_components folder

  ```shell
  $ bower install my-web-component
  $ gwt-api-generator
  ```
- Generating java code for a complete library

  ```shell
  $ gwt-api-generator --package=PolymerElements/paper-elements
  ```
- Generating resources with a custom groupId and artifactId

  ```shell
  $ gwt-api-generator --package=PolymerElements/paper-elements \
                    --groupId=com.foo --artifactId=bar
  ```
- Generating resources for a non-maven structure

  ```shell
  $ gwt-api-generator --package=PolymerElements/paper-elements
                      --javaDir=src --resourcesDir=src
  ```
- Generating maven `pom.xml` file and packaging the result as a ready-to-use `.jar` file

  ```shell
  $ gwt-api-generator --package=PolymerElements/paper-elements --pom
  $ mvn package
  ```

## Pre-built packages

### Paper, Iron and Vaadin-Core elements

Vaadin officially maintains and supports a pre-built package deployed at Maven Central repository containing all the resources needed for using Polymer
[paper-elements](https://elements.polymer-project.org/browse?package=paper-elements),
[iron-elements](https://elements.polymer-project.org/browse?package=iron-elements) and
[vaadin-core-elements](https://vaadin.com/elements)
in a GWT application.

Build script, demo and usage instructions for the project are available [here](https://github.com/vaadin/gwt-polymer-elements).

You also might see all these components in action using the [Show Case](http://vaadin.github.io/gwt-polymer-elements/demo/) application


## About GWT 2.7/2.8 compatibility

Vaadin gwt-api-generator produces `@JsType` interfaces for JS Element level access from Java Objects.

Generated classes are written using Java 1.7 syntax, and rely on GWT JSInterop available as an experimental feature from GWT 2.7.0, and will be stable in GWT 2.8.0.

But, starting with gwt-api-generator 1.2.1, GWT 2.7.0 is not supported anymore since the experimental version of jsInterop in 2.7.0 does not support JsFunctions.

Additionally JsInterop has suffered a complete API change in 2.8.0, so old syntax will be deprecated and remove.

So, will encourage to make your project depend on GWT 2.8.0-SNAPSHOT and use new JsInterop syntax.

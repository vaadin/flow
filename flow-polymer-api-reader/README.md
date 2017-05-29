# Vaadin Flow Polymer API Reader

Vaadin Flow Polymer API Reader is a tool that reads Web Components as input and outputs data that can be used by a Java code generation tool (WIP) that produces Flow Component APIs.
Currently the reader also generates the components into the source folder `src`.

Currently the reader supports only Web Components written in the Polymer 1.0 syntax and reads them using Hydrolysis, soon to be switched to use Analyzer and Polymer 2.0. Support for other type of JavaScript sources might be added in the future.

## Installation and Usage

- Local Installation

Clone this repository and run the flow-polymer-api-reader folder run:
```shell
$ npm link
```
> If you've installed node and npm using `sudo`, installing packages globally will also require you to use `sudo`. See [http://givan.se/do-not-sudo-npm/](http://givan.se/do-not-sudo-npm/) how to remedy the situation.

- Generating java code for bower packages installed in your bower_components folder

  ```shell
  $ bower install my-web-component
  $ flow-polymer-api-reader
  ```
- Generating java code for a complete library

  ```shell
  $ flow-polymer-api-reader --package=PolymerElements/paper-elements
  ```
- Generating resources with a custom groupId and artifactId

  ```shell
  $ flow-polymer-api-reader --package=PolymerElements/paper-elements \
                    --groupId=com.foo --artifactId=bar
  ```
- Generating resources for a non-maven structure

  ```shell
  $ flow-polymer-api-reader --package=PolymerElements/paper-elements
                      --javaDir=src --resourcesDir=src
  ```
- Generating maven `pom.xml` file and packaging the result as a ready-to-use `.jar` file

  ```shell
  $ flow-polymer-api-reader --package=PolymerElements/paper-elements --pom
  $ mvn package
  ```

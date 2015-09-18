
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin/components?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

# vaadin-grid

`vaadin-grid` is a Web Component for showing large amounts of tabular data.

**Features:**
 - Lazy-loading
 - Virtual scrolling
 - Frozen/fixed columns
 - Customizable headers and footers
 - Custom cell renderers
 - Touch support
 - Keyboard navigation
 - Sorting
 - Accessibility

## Getting started

[Getting started instructions](http://vaadin.github.io/components-examples/)

## Overview of this repository

- **demo**:
  A set of examples using the component

- **test**:
  Unit and visual tests

- **java**:
  The internal GWT implementation of the component
  exported to JavaScript which is used by the Polymer-based implementations.


## Developing

### Setting up the project for the first time

First, make sure you've installed all the necessary tooling:
- [Node.js](http://nodejs.org).
- [JDK8](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
- [Maven](http://maven.apache.org/download.cgi).

> If you encounter permission issues when running `npm` see [thread in StackOverFlow.com](http://stackoverflow.com/questions/16151018/npm-throws-error-without-sudo)

Then do the following:

1. Install [bower](https://www.npmjs.com/package/bower), [gulp](https://www.npmjs.com/package/gulp), [web-component-tester](https://www.npmjs.com/package/web-component-tester) and [polyserve](https://www.npmjs.com/package/polyserve) globally:
 ```shell
 $ npm install -g bower gulp web-component-tester polyserve
 ```
 
 _*Note: you might prepend `sudo` if you are in a unix like system (linux, mac)_

2. Clone the project:
 ```shell
 $ git clone https://github.com/vaadin/vaadin-grid.git
 $ cd vaadin-grid
 ```

3. Install the project dependencies:
 ```shell
 $ npm install
 ```

### Serving the components

- Spin up a web server:
```shell
$ polyserve
```
- Check that the components demo works visiting `http://localhost:8080/components/vaadin-grid/demo/`
- Access the components in your app through `http://localhost:8080/components/vaadin-grid/vaadin-grid.html`
- To use vaadin components, remember to manually install [Polymer](https://github.com/Polymer/polymer) as a dependency.
```shell
$ cd your-application
$ bower install polymer --save
```

### Running automated tests

- Run all tests locally:
```shell
$ gulp test
```
- Run a single browser locally:
```shell
$ wct --local=chrome/firefox/safari
```
- Run and debug tests manually:
```shell
$ polyserve
```
- Open http://localhost:8080/components/vaadin-grid/test/

### Development Protips

- Compiling GWT module:
```shell
$ gulp gwt
``` 
- Compiling GWT using "pretty" output:
```shell
$ gulp gwt --gwt-pretty
```
- Adding file watcher for GWT compilation:
```shell
$ gulp watch:gwt
```
- Running and debugging in GWT SuperDevMode:
```shell
$ mvn -f java/pom.xml gwt:run
```
- Update your dependencies once in a while:
```shell
$ npm install
```

## Demos / examples

Start a server in the root folder,
and access one of the demo files inside the component folders, e.g:
```shell
$ polyserve
```
- Open http://localhost:8080/components/vaadin-grid/demo/


## License

Vaadin Components is licensed under the Apache License 2.0.

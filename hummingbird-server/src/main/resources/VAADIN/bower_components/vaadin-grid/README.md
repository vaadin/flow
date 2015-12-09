
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/vaadin/vaadin-elements?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

[![Build Status](https://travis-ci.org/vaadin/vaadin-grid.svg?branch=master)](https://travis-ci.org/vaadin/vaadin-grid)

# vaadin-grid

`vaadin-grid` is a Web Component for showing large amounts of tabular data, part of the [vaadin-elements](https://github.com/vaadin/vaadin-elements) element bundle.

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
 - Declarative and imperative APIs

## Getting started

 - [Demos](https://cdn.vaadin.com/vaadin-elements/latest/vaadin-grid/demo/)
 - [Getting started instructions](https://cdn.vaadin.com/vaadin-elements/latest/vaadin-elements/demo/)

## Overview of this repository

- **demo**:
  A set of examples using the element

- **test**:
  Unit and visual tests

- **java**:
  The internal GWT implementation of the element
  exported to JavaScript which is used by the Polymer-based implementation.


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
 
 > Note: you might need to prepend `sudo` if you are in a unix like system (linux, mac)

2. Clone the project:
 ```shell
 $ git clone https://github.com/vaadin/vaadin-grid.git
 $ cd vaadin-grid
 ```

3. Install the project dependencies:
 ```shell
 $ npm install
 ```

## Demos / examples

Start a server in the root folder, and access one of the demo files inside 
the elements folders, e.g:
```shell
$ polyserve
```
- Open http://localhost:8080/components/vaadin-grid/demo/


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

Compiled module is in the repository, so you don't need to compile it unless
you modify GWT .java files.

```shell
$ gulp gwt
```
- Compiling GWT using "pretty" output:
```shell
$ gulp gwt --gwt-pretty
```


- Running and debugging in GWT SuperDevMode:
```shell
$ gulp gwt:sdm
```
- Update your dependencies once in a while:
```shell
$ npm install
```


## License

`vaadin-grid` is licensed under the Apache License 2.0.

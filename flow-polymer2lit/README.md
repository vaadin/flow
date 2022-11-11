# Polymer to Lit converter

An automatic tool that facilitates migration from Polymer to Lit by automatically converting basic Polymer constructions into their Lit equivalents in Java and JavaScript source files.

## Motivation

Polymer support has been deprecated since Vaadin 18 (released in November 2020), in favor of faster and simpler Lit templates. In Vaadin 24, the built-in support for Polymer templates is removed. Polymer support is still available in Vaadin 24 for Prime and Enterprise customers.

## Limitations

The converter only supports a limited set of transformations intended to reduce the effort for basic cases while everything else should be still converted by hand.

An example of what the converter cannot handle is complex Polymer observers:

```js
static get properties() {
  return {
    userList: {
      type: String,
      observer: 'userListChanged(users.*, filter)"
    },
  }
}
```

TypeScript source files are not supported.

## Usage

Run the converter in your project's root folder as follows:

```bash
mvn vaadin:convert-polymer
```
```bash
./gradlew vaadinConvertPolymer
```

To convert a project that is based on Vaadin < 24, use the full Maven goal:

```bash
mvn com.vaadin:vaadin-maven-plugin:24.0-SNAPSHOT:convert-polymer
```

Or, in the case of using Gradle, add the following to `build.gradle`:

```gradle
buildscript {
  repositories {
    classpath 'com.vaadin:flow-gradle-plugin:24.0-SNAPSHOT'
  }
}
```

## Configuring

The converter accepts the following arguments:

### -Dvaadin.path=path/to/your/file

By default, the converter scans all the files that match `**/*.js` and `**/*.java` and tries to convert them to Lit.

To limit conversion to a specific file or directory, you can use the `vaadin.path` argument:

```bash
mvn vaadin:convert-polymer -Dvaadin.path=path/to/your/file
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.path=path/to/your/file
```

The path is always relative to your project's root folder.

### -Dvaadin.useLit1

By default, the converter transforms Polymer imports into their Lit 2 equivalents.

If your project is using Lit 1 (Vaadin < 23), you can use the `vaadin.useLit1` argument to enforce Lit 1 compatible imports:

```bash
mvn vaadin:convert-polymer -Dvaadin.useLit1
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.useLit1
```

### -Dvaadin.disableOptionalChaining**

By default, the converter transforms `[[prop.sub.something]]` expressions into `${this.prop?.sub?.something}`.

If your project is using the Vaadin Webpack config, which doesn't support the JavaScript optional chaining operator (?.), you can use the `vaadin.disableOptionalChaining` argument:

```bash
mvn vaadin:convert-polymer -Dvaadin.disableOptionalChaining
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.disableOptionalChaining
```

## Supported transformations

This is a basic overview of the transformations that can be performed automatically by the converter:

### JavaScript

**Imports**

Example:
```diff
-import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
+import { html, LitElement, css } from 'lit';
```

**Lifecycle callbacks**

Example:
```diff
-ready() { ... }
+firstUpdated() { ... }
```

**Templates and bindings**

Example:
```diff
-static get template() {
-  return html`
-    <div>[[person.name]]</div>
-  `
-}
+render() {
+  return html`
+    <div>${this.person?.name}</div>
+  `
+}
```

Nested properties are accessed through the optional chaining operator to keep that in line with how Polymer interprets expressions.

More complex expressions, such as containing method invocations, are also supported.

**Default property values**

Initializing default property values is moved to the constructor.

Example:
```diff
static get properties() {
  return {
    fruits: {
      type: String,
-      value: () => ['apple', 'banana']
    }
  }
}

+constructor() {
+  super();
+  this.value = ['apple', 'banana'];
+}
```

**Computed properties**

Computed properties are replaced with getters.

Example:
```diff
static get properties() {
  return {
    firstName: String,
    lastName: String,
-    fullName: {
-      type: String,
-      computed: 'computeFullName(firstName, lastName)',
-    },
  };
}

+get fullName() {
+  return this.computeFullName(this.firstName, this.lastName);
+}

computeFullName(firstName, lastName) {
  return `${firstName} ${lastName}`;
}
```

**Event handlers**

Example:
```diff
-<div on-click="onClick"></div>
+<div @click="${this.onClick}></div>
```

**Two-way binding**

Two-way binding is replaced with a pair of one-way binding and event handler.

Example:

```diff
-<input value="{{value}}" />
+<input
+  .value="${this.value}"
+  @value-changed="${(e) => (this.value = e.detail.value)}" />
```


**Observers**

Observers are replaced with a pair of getters and setters.

Example:

```diff
static get properties() {
  return {
    firstName: {
      type: String,
-      observer: '_firstNameChanged'
    }
  };
}

+set firstName() { ... }
+get firstName() { ... }

_firstNameChanged(newValue, oldValue) { ... }
```

**`<dom-if>`**

Example:
```diff
-<dom-if if="{{condition}}">...</dom-if>
+${condition && html`...`}
```

**`<dom-repeat>`**

Example:
```diff
-<template is="dom-repeat" items="{{items}}">
-  <div>[[item]] [[index]]</div>
-</template>
+${items.map((item, index) =>
+  html`<div>${item} ${index}</div>`
+)}
```

**`<style>`**

Example:
```diff
-<style>
-  :host {
-    color: black;
-  }
-</style>
+static get styles() {
+  return css`
+    :host {
+      color: black;
+    }
+  `
+}
```

**Static node map**

Example:
```diff
-this.$.container.textContent = 'Content';
+this.shadowRoot.querySelector('#container').textContent = 'Content';
```

### Java


# Polymer to Lit converter

An automatic tool that facilitates migration from Polymer to Lit by automatically converting basic Polymer constructions into their Lit equivalents in Java and JavaScript source files.

## Motivation

Polymer support has been deprecated since Vaadin 18 (released in November 2020), in favor of faster and simpler Lit templates. In Vaadin 24, the built-in support for Polymer templates is removed. Polymer support is still available in Vaadin 24 for Prime and Enterprise customers.

## Usage

Run the converter in your project's root folder as follows:

```bash
mvn vaadin:convert-polymer
```
```bash
./gradlew vaadinConvertPolymer
```

If your project is using an older Flow version < 24.0, use the full Maven goal instead:

```bash
mvn com.vaadin:vaadin-maven-plugin:24.0-SNAPSHOT:convert-polymer
```

Or, in the case of Gradle, add the following to `build.gradle`:

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

If your project is using Lit 1, you can use the `vaadin.useLit1` argument to enforce Lit 1 compatible imports:

```bash
mvn vaadin:convert-polymer -Dvaadin.useLit1
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.useLit1
```

### -Dvaadin.disableOptionalChaining**

By default, the converter transforms `[[prop.sub.something]]` expressions into `${prop?.sub?.something}`.

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

**Lifecycle callbacks**

```diff
-ready() {
-  ...
-}
+firstUpdated() {
+  ...
+}
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

More complex expressions, including method invocations, are also supported.

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

```diff
-<div on-click="onClick"></div>
+<div @click="${this.onClick}></div>
```

**Two-way binding**



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

_firstNameChanged(newValue, oldValue) {
}
```

**`<dom-if>`**

Example:
```diff
-<dom-if if="{{condition}}">...</dom-if>
+${condition && ...}
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

### Java


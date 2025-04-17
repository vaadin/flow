# Polymer to Lit converter

An automatic tool that facilitates migration from Polymer to Lit by automatically converting basic Polymer constructions into their Lit equivalents in Java and JavaScript source files.

## Motivation

Polymer support has been deprecated since Vaadin 18 (released in November 2020), in favor of faster and simpler Lit templates. In Vaadin 24, the built-in support for Polymer templates is removed. Polymer support is still available in Vaadin 24 for Prime and Enterprise customers.

## Limitations

The converter only supports a limited set of transformations intended to reduce the effort for basic cases while everything else should be still converted by hand.

Here are a few examples of what the converter cannot handle:

- A Java Model implementation can be only generated for internal models i.e that are declared within the View class.
- A Java Model implementation can be only generated for String and Boolean fields.
- TypeScript source files are not supported because proper type conversion almost always requires knowing project specifics.
- Complex Polymer observers are not supported.
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
mvn com.vaadin:vaadin-maven-plugin:24.8-SNAPSHOT:convert-polymer
```

Or, in the case of using Gradle, add the following to `build.gradle`:

```gradle
buildscript {
  repositories {
    classpath 'com.vaadin:flow-gradle-plugin:24.8-SNAPSHOT'
  }
}
```

## Configuring

The converter accepts the following properties:

### -Dvaadin.path=path/to/your/file

By default, the converter scans all the files that match `**/*.js` and `**/*.java` and tries to convert them to Lit.

To limit conversion to a specific file or directory, you can use the `vaadin.path` property:

```bash
mvn vaadin:convert-polymer -Dvaadin.path=path/to/your/file
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.path=path/to/your/file
```

The path is always relative to your project's root folder.

### -Dvaadin.useLit1

By default, the converter transforms Polymer imports into their Lit 2 equivalents.

If your project is using Lit 1 (Vaadin < 21), you can use the `vaadin.useLit1` flag to enforce Lit 1 compatible imports:

```bash
mvn vaadin:convert-polymer -Dvaadin.useLit1
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.useLit1
```

### -Dvaadin.disableOptionalChaining**

By default, the converter transforms `[[prop.sub.something]]` expressions into `${this.prop?.sub?.something}`.

If your project is using the Vaadin Webpack config, which doesn't support the JavaScript optional chaining operator (?.), you can use the `vaadin.disableOptionalChaining` flag:

```bash
mvn vaadin:convert-polymer -Dvaadin.disableOptionalChaining
```
```bash
./gradlew vaadinConvertPolymer -Dvaadin.disableOptionalChaining
```

## Supported transformations

This is an overview of the transformations that can be performed automatically by the converter:

### JavaScript

**Imports**

```diff
-import { html, PolymerElement } from '@polymer/polymer/polymer-element.js';
+import { html, LitElement, css } from 'lit';
```

**Lifecycle callbacks**

```diff
-ready() { ... }
+firstUpdated() { ... }
```

**Templates and bindings**

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

Two-way binding is replaced with a pair of one-way binding and event handler.

```diff
-<input value="{{value}}" />
+<input
+  .value="${this.value}"
+  @value-changed="${(e) => (this.value = e.detail.value)}" />
```

**Observers**

Observers are replaced with a pair of getters and setters.

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

**\<dom-if>**

```diff
-<dom-if if="{{condition}}">...</dom-if>
+${condition && html`...`}
```

**\<dom-repeat>**

```diff
-<template is="dom-repeat" items="{{items}}">
-  <div>[[item]] [[index]]</div>
-</template>
+${items.map((item, index) =>
+  html`<div>${item} ${index}</div>`
+)}
```

**<style>**

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

```diff
-this.$.container.textContent = 'Content';
+this.shadowRoot.querySelector('#container').textContent = 'Content';
```

### Java

**Imports**

```diff
-import com.vaadin.flow.component.polymertemplate.Id;
+import com.vaadin.flow.component.template.Id;
-import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
+import com.vaadin.flow.component.littemplate.LitTemplate;
-import com.vaadin.flow.templatemodel.TemplateModel;
```

**Views extending PolymerTemplate**

PolymerTemplate extend is replaced with LitTemplate extend.

```diff
-public class UserEditView extends PolymerTemplate {
+public class UserEditView extends LitTemplate {
```

**Models extending TemplateModel**

TemplateModel extend is removed.

```diff
public class UserEditView extends LitTemplate {
-  public interface Model extend TemplateModel { ... }
+  public interface Model { ... }
}
```

**Model implementation**

As `Model` no longer extends `TemplateModel`, the `getModel()` method is added with a basic implementation of setters and getters.

The converter is only able to generate an implementation for String and Boolean fields. For others, it generates empty methods.

```diff
public class UserEditView extends LitTemplate {
  public interface Model {
    String getFirstName();

    void setFirstName(String value);
  }

+  private Model getModel() {
+    return new Model() {
+      @Override
+      public void setFirstName(String firstName) {
+        getElement().setProperty("firstName", firstName);
+      }
+
+      @Override
+      public String getFirstName() {
+        return getElement().getProperty("firstName", "");
+      }
+    }
+  }
}
```


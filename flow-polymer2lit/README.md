# Polymer to Lit converter

An automatic tool that facilitates migration from Polymer to Lit by automatically converting basic Polymer constructions into their Lit equivalents in Java and JavaScript source files.

## Limitations

The converter only targets basic cases. More advanced cases such as TypeScript source files or usage of internal Polymer API should be still converted manually.

## Usage

Run the converter in your project's root folder as follows:

```bash
mvn vaadin:convert-polymer
```

If your project is using an older Flow version < 24.0, use the full Maven goal instead:

```bash
mvn com.vaadin:vaadin-maven-plugin:24.0-SNAPSHOT:convert-polymer
```

## Configuring

The converter accepts the following arguments:

**-Dvaadin.path=path/to/your/file**

By default, the converter scans all the files that match `**/*.js` and `**/*.java` and tries to convert them into Lit.

To limit conversion to a specific file or directory, you can use the `path` argument:

```bash
mvn vaadin:convert-polymer -Dpath=path/to/your/file
```

The path is always relative to your project's root folder.

**-Dvaadin.useLit1**

By default, the converter transforms Polymer imports into their Lit 2 equivalents.

If your project is using Lit 1, you can use the `useLit1` argument to enforce Lit 1 compatible imports:

```bash
mvn vaadin:convert-polymer -DuseLit1
```

**-Dvaadin.disableOptionalChaining**

By default, the converter transforms `[[prop.sub.something]]` expressions into `${prop?.sub?.something}`.

If your project is using the Vaadin Webpack config, which doesn't support the optional chaining operator, you can use the `disableOptionalChaining` argument:

```bash
mvn vaadin:convert-polymer -DdisableOptionalChaining
```



# Frontend build: selected topics

`flow-build-tools` holds the frontend build of a Flow application: the
`Task*` commands that `NodeTasks` runs, the scanners, and the Vite plugins in
`src/main/resources/plugins/`. This note covers the parts whose design is not
obvious from the code. The Vite plugins have notes of their own, next to
their sources:

- [`vite-plugin-service-worker`](src/main/resources/plugins/vite-plugin-service-worker/ARCHITECTURE.md): `@PWA` and the offline service worker
- [`rollup-plugin-vaadin-i18n`](src/main/resources/plugins/rollup-plugin-vaadin-i18n/ARCHITECTURE.md): translations for Hilla views

## npm dependency pinning

### Overview

The browser part of a Vaadin application is a set of npm packages: the web
components, React, Lit, Vite and their dependencies. A Vaadin version is
tested with one exact version of each. The build makes sure the application
gets those versions, whether the package is a direct dependency or arrives
transitively, and whether the application installs with npm, pnpm or bun.

On every build, `TaskUpdatePackages`:

1. adds the npm packages the application needs (from `@NpmPackage`, the
   default dependency sets, the React or Lit router, Hilla) to the
   `dependencies` and `devDependencies` of `package.json`;
2. computes the version to pin each known package to;
3. writes **overrides**, which make npm and pnpm use the pinned version
   everywhere in the dependency tree: the `overrides` object of `package.json`
   for npm and bun, and the `overrides` map of `pnpm-workspace.yaml` for pnpm;
4. updates the hash in `package.json`, so that `TaskRunNpmInstall` knows when
   to install again.

### Design goals

- **What Vaadin tested is what gets installed**, including transitive
  dependencies. A component that pulls in another one through its own
  `package.json` gets the version of the same release.
- **Every jar can pin its own packages.** The platform, the commercial
  components and add-ons each ship their versions. No single file has to know
  everything.
- **The application has the last word.** A version the developer sets by hand
  is kept, with a warning when it is older or in a different minor than
  Vaadin's.
- **Upgrading is a version bump.** Changing `vaadin.version` updates the
  dependencies and the overrides it wrote, without manual cleanup and
  without `EOVERRIDE` conflicts.
- **Vaadin's entries are recognisable**, so that they can be updated and
  removed without touching what the developer wrote.

### The pieces

| Piece | Role |
| ----- | ---- |
| `META-INF/VAADIN/versions/*.json` | Versions files. Any jar may ship them. Each entry has an `npmName`, an `npmVersion` (or `jsVersion`), and optionally a `mode` (`react`, `lit`, `all`). A file may also list `exclusions`. The platform's files also carry a `platform` version. |
| `PinnedNpmVersions` | Reads and merges all versions files on the classpath. On a conflict the newer version wins, with a warning. Answers the Vaadin version, but only from the platform's own files. |
| `VersionsJsonConverter` | Turns one versions file into `name → version`, for the current mode (React or Lit), dropping excluded packages and, with `npmExcludeWebComponents`, the web components. |
| `VersionsJsonFilter` | Drops SNAPSHOT and unparsable versions, keeps URL versions, and warns when the developer pinned an older version in `package.json`. |
| `NodeUpdater` | Base of the package tasks. Reads the default dependency sets from `flow-server/src/main/resources/com/vaadin/flow/server/frontend/dependencies/*/package.json`, and keeps the `vaadin` section of `package.json`. |
| `TaskUpdatePackages` | Adds the dependencies, computes and writes the overrides, warns about version mismatches, and writes the hash. |
| `PnpmWorkspaceFile` | Reads and writes the `overrides` map of `pnpm-workspace.yaml`, where pnpm 10 and later read them. |

### The `vaadin` section of `package.json`

`package.json` has a `vaadin` object next to the npm sections. It records the
`dependencies` and `devDependencies` *as Vaadin last wrote them*, and the
hash. This record is how the build tells its own entries from the
developer's:

- If the version in `dependencies` is the same as in `vaadin.dependencies`,
  Vaadin wrote it, and a newer Vaadin may change it, up or down
  (`NodeUpdater.addDependency`).
- If they differ, the developer changed it. The build keeps the developer's
  version, and only warns when it is in another minor or major than the one
  Vaadin pins (`warnOnVersionRangeMismatch`, #24712).

### The overrides

`computeVaadinOverrides` builds the overrides Vaadin wants, for each package
of the versions files (and, where there are no versions files, of
`package.json` itself):

- **A package that is not a direct dependency** is overridden with the
  pinned version, so that every transitive use gets that version.
- **A package that is also a direct dependency** is overridden with a
  reference, `"$<name>"`. npm and pnpm resolve it to the version in
  `dependencies`, which is the developer's version if they changed it. This is
  what avoids `EOVERRIDE`, the conflict npm reports when an override and a
  direct dependency disagree (#24213), and it is how a developer opts out of
  the pinned version (#24123).
- **SNAPSHOT and unparsable versions, and packages linked to a folder**, are
  not pinned. URL versions, such as the tarball of a preview build, are
  pinned as they are (#26319).
- **Default overrides** come on top, from the `overrides` of the default
  dependency sets: at the moment, workbox's transitive dependencies when
  offline `@PWA` is enabled (#24008).

`pinVersionsForNpm` then loads the existing overrides as a flat map, with `>`
between nested keys, the format pnpm uses. It removes every key Vaadin
manages and every `$` reference to a package that is no longer a dependency,
adds the computed ones, and saves. Overrides of packages Vaadin doesn't
manage are kept as they are. The managed keys are recomputed on every build,
rather than remembered in `package.json`, so a leftover from an older Vaadin
version can't block an upgrade (#24137, #24718).

### Decisions and gotchas

- **One versions folder per jar** (#25308). Before, only the platform's
  `vaadin-core-versions.json` and `vaadin-versions.json` were read. Now every
  jar's `META-INF/VAADIN/versions/` is merged, so an add-on can pin its own
  npm packages.
- **Only the platform says what the Vaadin version is.** A versions file
  counts as the platform's only if it declares `@vaadin/vaadin-core` or
  `@vaadin/react-components`, so an add-on can't change the Vaadin version
  the build reports.
- **Mode matters.** React and Lit builds use different packages for the same
  component. `VersionsJsonConverter` keeps only the entries of the current
  mode. React entries are only kept when `flow-react` is on the classpath.
- **pnpm reads overrides from `pnpm-workspace.yaml`**, not from
  `package.json` (#24918). Overrides left in `package.json` by an older Flow
  (`pnpm.overrides`) are moved there, and the obsolete `vaadin.overrides`
  section is removed.
- **A broken versions file doesn't stop the build.** A file that can't be
  read is skipped with a warning, and the packages of the other files are
  still pinned. A folder that can't be listed is skipped as a whole, because
  it isn't known which of its files were missed.
- **Changing `package.json` reinstalls.** The hash covers the dependencies
  and the overrides. When it changes, `TaskRunNpmInstall` runs the package
  manager again.

### Working on it

- Unit tests: `PinnedNpmVersionsTest`, `VersionsJsonConverterTest`,
  `VersionsJsonFilterTest`, `TaskUpdatePackagesNpmTest`,
  `NodeUpdatePackagesNpmVersionPinningTest` and `NodeUpdaterTest` in
  `flow-build-tools`.
- Most bugs here show up only in an upgrade from one Vaadin version to
  another, with an existing `package.json`. Test with the `package.json` of
  the previous version, not only with an empty project.

## Tailwind CSS

### Overview

With the `tailwindCss` feature flag on, an application can style its views
with Tailwind's utility classes, from Java and from client-side code alike:

```java
layout.addClassNames("flex", "gap-4", "p-6", "bg-gray-50");
```

Tailwind doesn't ship a stylesheet with every class. Its compiler scans the
sources for the class names that are used, and generates only those. Flow
puts that compiler into the frontend build:

1. The build adds `tailwindcss` and `@tailwindcss/vite` to `package.json` and
   the Tailwind plugin to `vite.generated.ts`.
2. It generates `frontend/generated/tailwind.css`, the Tailwind entry point.
   That file tells the compiler to scan the project's `src` folder, Java
   files included, and imports the application's own CSS, so that `@apply`
   and `@theme` work there too.
3. It generates `frontend/generated/tailwind.js`, which injects the compiled
   CSS into the page, and imports it from the app shell imports.
4. In development, a change to a scanned file makes Vite recompile the CSS,
   and `tailwind.js` swaps it in place through HMR, without a reload.

### Design goals

- **Utility classes from Java.** The class names in Java code are found by
  the Tailwind compiler itself, so no list of classes has to be kept.
- **No configuration to start**, and one optional file to customize: a
  `frontend/tailwind-custom.css` with `@theme` and other Tailwind directives
  is imported when it exists (#22969).
- **Play well with Lumo and the application's styles.** The entry point
  imports Tailwind's theme and utilities only, not Preflight, Tailwind's base
  reset, so the base styles of the application stay as they are. The
  utilities are injected after the `@CssImport` styles, so a utility class
  wins over them.
- **Fast feedback.** Editing a Java file that adds a class updates the styles
  through HMR (#22927).

### The pieces

| Piece | Role |
| ----- | ---- |
| `CoreFeatureFlagProvider.TAILWIND_CSS`, `FrontendBuildUtils.isTailwindCssEnabled` | The feature flag, and the check every other piece uses. |
| `NodeUpdater` | Adds the dependencies from `dependencies/tailwindcss/package.json` in `flow-server`. |
| `TaskUpdateVite.updateTailwindCssVitePlugin` | Replaces the `//#tailwindcssVitePlugin…#` placeholders in `vite.generated.ts`. |
| `TaskGenerateTailwindCss` + `tailwind.css` template | Writes the entry point: `@source` for the `src` folder, the import of `tailwind-custom.css`, and imports of the theme's `styles.css`, the `@CssImport` files and the local `@StyleSheet` files. |
| `TaskGenerateTailwindJs` + `tailwind.js` template | Writes the module that imports `tailwind.css?inline` and injects it with `injectGlobalCss`, before the `CSSImport end` marker. On HMR, it replaces the previous style tag. |
| `AbstractUpdateImports` | Adds the `tailwind.js` import to the app shell imports. |
| `ApplicationConfiguration.getMode` | Turns the development bundle mode into frontend hot deploy when the flag is on. |

### Decisions and gotchas

- **Development needs the Vite dev server.** A pre-built development bundle
  can't know which classes the application's Java code will use. So with the
  flag on, `ApplicationConfiguration.getMode()` returns
  `DEVELOPMENT_FRONTEND_LIVERELOAD` instead of `DEVELOPMENT_BUNDLE` (#22853).
  A production build compiles Tailwind as part of the normal Vite build.
- **Only class names written out in full are found.** Tailwind reads the
  sources as text. A class name built at run time, such as `"p-" + size`, is
  not in the CSS.
- **The application's CSS is imported for `@apply`.** Tailwind only resolves
  `@apply` in files it compiles, so `tailwind.css` imports the theme's
  `styles.css`, the `@CssImport` files and the local `@StyleSheet` files.
  Each is looked up in the frontend folder, the jar resources and
  `src/main/resources/META-INF/resources`, and imported only if found
  (#23303). External stylesheets are left out.
- **Both files are always regenerated**, on every build, so they follow
  changes in the project.
- **Imports go through the `Frontend/` alias** in the app shell imports, so
  the path doesn't depend on where the generated folder is (#23481).

### Working on it

- Integration test: `flow-tests/test-tailwindcss` (`TailwindCssIT`), with a
  `tailwind-custom.css` in its frontend folder.
- The `tailwind.css` and `tailwind.js` templates are in
  `flow-server/src/main/resources/com/vaadin/flow/server/frontend/`.

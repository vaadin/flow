## Embedding tests
This module contains most integration tests related to embedding flow. If 
some related tests are in other directories, they are there for some good 
reason.

#### Module structure:
There are currently six modules, four of which contain the generic embedding 
tests (embedding, updating properties, firing events, etc). The four modules are
- `test-embedding-generic`
- `test-embedding-generic-compatibility`
- `test-embedding-production`
- `test-embedding-production-compatibility`

These are essentially running the same tests; `-generic` runs in development 
mode while `-production` runs in production mode. Modules with 
`-compatibility` run in compatibility (or Bower) mode. These four modules all
 use the `embedding-test-assets` module as their dependency. The 
 `embedding-test-assets` module provides both the test classes and the 
 classes _under_ test. 
 
 The only clear distinctions between the different modules are their 
 configuration and `html` pages. Compatibility mode uses html imports while 
 the "normal" mode uses JavaScript modules. These pages are located under
  `test-embedding/webapp`.
 
 The two current outliers are 
 - `test-embedding-theme-variant`
 - `test-embedding-theme-variant-compatibility`
 
 From the names you've probably already guessed which module runs in which 
 mode. These are separated from the other modules (and from themselves) since
 they leverage theme variants and since a package containing embeddable 
 components can only have one theme, it is cleaner to keep these separate. 

### Notes for adding new tests

- `embedding-test-assets/WebComponentIT` is an abstract class which needs to 
be implemented. Its method `assertThatThemedComponentWasChosen` is used to 
validate that the embedded component being tested has the correct theme 
applied. See modules `test-embedding-generic` and 
`test-embedding-generic-compatibility` for examples.
 
### Note on static webapp html file usage

- DO NOT create a file named web-component.html in src/main/webapp/
- The file web-component.html is reserved and auto-generated in src/main/frontend/web-component.html by the Vaadin build
  process
- If you create a manual web-component.html in the webapp folder, it will be served directly by the servlet container,
  bypassing Vite's transformation pipeline
- This prevents the automatic injection of vaadin-web-component.ts, which initializes the Flow client and sets up
  required APIs like window.Vaadin.Flow.registerWidgetset
- Result: Your embedded web components will fail to connect to the server with errors like
  $wnd.Vaadin.Flow.registerWidgetset is not a function

Correct approach:
- Use index.html or any other name for your main HTML files in src/main/webapp/
- Let Vaadin generate src/main/frontend/web-component.html automatically during the build
- Access your embedded web components through the URLs served by the Vaadin servlet, which will properly transform the
  auto-generated web-component.html

File structure:
src/main/
├── frontend/
│   └── web-component.html        ← Auto-generated, includes proper initialization
├── webapp/
│   └── index.html                ← Your custom HTML (use any name except web-component.html)
└── java/
    └── com/example/
    └── MyComponentExporter.java

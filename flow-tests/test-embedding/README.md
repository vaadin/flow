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

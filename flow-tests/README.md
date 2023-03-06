## Test modules and their meanings

* servlet-containers
  * Tests functionality on different servlet containers using test-root-context sources and tests
    * Tomcat8.5 & Tomcat9: test servers in compatibility mode
    * felix-jetty: Test OSGi functionality in compatibility mode

* test-dev-mode
  * Root context tests run in compatibility + development mode
* test-embedding
  * Test for webcomponents (npm and compatibility mode) see module [README.md](test-embedding/README.md)
  * Sum test modules:
    * embedding-test-assets
    * test-embedding-generic
    * test-embedding-generic-compatibility
    * test-embedding-production-mode
    * test-embedding-production-mode-compatibility
    * test-embedding-theme-variant
    * test-embedding-theme-variant-compatibility
    * webapp
      * test module configuration and templates
* test-frontend-production-custom-context
  * Test running compatibility mode with custom contextPath
* test-memory-leaks
  * Verification test that no memory leaks happen during war redeployment.
* test-misc
  * Uncategorized tests
    * Contains custom theme tests
* test-mixed
  * Test maven builds in all 4 run modes
* test-multi-war
  * Tests that we can deploy multiple Flow war files to the same server
* test-no-root-context
  * Running tests with a non-root context mapping
    * `/custom/*` mapped istead of `/*`
  * !Not active at the moment!
* test-no-theme
  * Test functionality without any theme present
  * !Not active at the moment!
* test-npm-only-features
  * Tests that are only viable for NPM mode
    * test-default-theme
      * Test that Lumo default theme is loaded
    * test-npm-bytecode-scanning
      * Test byte code scanning modes
        * Full classpath scanning for development mode
        * Classpath scanning without fallback
        * Classpath scanning with fallback
    * test-npm-custom-frontend-directory
      * Test npm mode with non default frontend folder
    * test-npm-general
      * Main NPM mode tests
    * test-npm-no-buildmojo
      * Development mode tests
    * test-npm-performance-regression
      * Test to see that startup performance is at an acceptable level
* test-pwa
  * ProgressiveWebApp tests
* test-root-context
  * Main Flow test module. 
    * Contains tests for all main Flow features. These should work in all supported modes.
    * Compatibility mode and npm mode tested in development mode.
* test-root-ui-context
  * Main UI test module. Compatibility mode only. 
* test-router-custom-context
  * Test Router on custom contextPath
* test-scalability
  * Gattling scalability tests. Compatibility mode only. 
* test-servlet
  * Automatic servlet registration test
* test-themes
  * Test application theme features
* test-live-reload
  * Tests the live reload feature in development mode. Run sequentially in their own
    module as live reload affects all open UIs and would cause interference between 
    parallelly executing tests.

### Common test resource modules

* test-common
  * Contains common Servlets an base views used in test modules:
    * test-dev-mode
    * test-themes
    * test-root-context
    * test-lumo-theme
    * test-material-theme
    * test-no-theme
    * test-npm-only-features
      * test-default-theme
      * test-npm-general
      * test-npm-no-buildmojo
      * test-npm-custom-frontend-directory
* test-resources 
  * Public resources used in test modules: 
    * test-dev-mode
    * test-root-ui-context
    * test-scalability
    * test-servlet
    * test-pwa
    * test-frontend-production-custom-context
    * test-router-custom-context
    * test-misc
    * test-mixed
    * test-no-root-context

gemini.suite('vaadin-ordered-layout', function(rootSuite) {
  function wait(actions, find) {
    actions.wait(5000);
  }

  function goToAboutBlank(actions, find) {
    // Firefox stops responding on socket after a test, workaround:
    return actions.executeJS(function(window) {
      window.location.href = 'about:blank'; // just go away, please!
    });
  }

  rootSuite
    .before(wait)
    .after(goToAboutBlank);

  gemini.suite('horizontal-layout', function(suite) {
    suite
      .setUrl('/default.html')
      .setCaptureElements('#horizontal-layout')
      .capture('horizontal-layout');
  });

  gemini.suite('vertical-layout', function(suite) {
    suite
      .setUrl('/default.html')
      .setCaptureElements('#vertical-layout')
      .capture('vertical-layout');
  });
});

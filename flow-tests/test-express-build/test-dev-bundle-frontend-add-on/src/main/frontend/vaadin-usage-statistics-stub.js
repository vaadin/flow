import { runIfDevelopmentMode } from '@vaadin/vaadin-development-mode-detector/vaadin-development-mode-detector.js';

// Stands in for @vaadin/vaadin-usage-statistics, which a Flow only
// application does not pull in. The published package has the comment below
// in the rewritten form already, this file has the plain form that the
// vaadin:preserve-usage-stats build plugin has to turn into /*! for a
// minifier to keep it. See DevBundleSourceMapsIT.
function maybeGatherAndSendStats() {
  /** vaadin-dev-mode:start
  document.documentElement.setAttribute('usage-statistics-stub-ran', 'true');
  vaadin-dev-mode:end **/
}

// The code inside the comment above is the code that is meant to run: the
// development mode detector reads it out of the source of this function and
// runs it, which only works as long as the build keeps the comment in the
// bundle. Development mode is forced on so that the test covers the build and
// not the heuristics the detector uses to recognize development mode.
window.Vaadin = window.Vaadin || {};
window.Vaadin.developmentMode = true;
runIfDevelopmentMode(maybeGatherAndSendStats);

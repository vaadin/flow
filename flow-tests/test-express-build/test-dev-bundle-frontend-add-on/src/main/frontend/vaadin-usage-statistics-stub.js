// Stands in for @vaadin/vaadin-usage-statistics, which a Flow only
// application does not pull in. The published package has the comment below
// in the rewritten form already, this file has the plain form that the
// vaadin:preserve-usage-stats build plugin turns into /*! so that a minifier
// keeps it. See DevBundleSourceMapsIT.
function maybeGatherAndSendStats() {
  /** vaadin-dev-mode:start
  console.log('vaadin-usage-statistics-stub');
  vaadin-dev-mode:end **/
}

// Keeps the module, and with it the comment, out of reach of tree shaking
window.vaadinUsageStatisticsStub = maybeGatherAndSendStats;

// Stands in for the @vaadin/vaadin-usage-statistics package, which a
// Flow-only application does not pull in. The build plugin that keeps the
// comment below in the bundle matches on the file name and on the marker, so
// this file is enough to run the build through that plugin.
/** vaadin-dev-mode:start
(function () {
  window.Vaadin = window.Vaadin || {};
  window.Vaadin.registrations = window.Vaadin.registrations || [];
})();
vaadin-dev-mode:end **/

// Keeps the module, and with it the comment, out of reach of tree shaking
window.Vaadin = window.Vaadin || {};
window.Vaadin.usageStatisticsStandIn = true;

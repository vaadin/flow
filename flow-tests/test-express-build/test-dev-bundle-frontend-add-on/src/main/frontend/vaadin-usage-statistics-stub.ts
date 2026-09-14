/** vaadin-dev-mode:start
console.log('Usage statistics stub');
vaadin-dev-mode:end **/

// Stands in for @vaadin/vaadin-usage-statistics, so that the build sees a
// module whose name and dev mode comment the vaadin:preserve-usage-stats
// plugin rewrites. The published package already has the rewritten comment,
// this file has the plain form that the plugin has to turn into /*! for a
// minifier to keep it. See DevBundleSourceMapsIT.
document.documentElement.setAttribute('usage-statistics-stub', 'true');

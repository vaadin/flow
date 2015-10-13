/* Pre-rendering timing */
if ({{preTiming}}) {
	var doneAfterNext = false;
	var raf = function() {
		if (doneAfterNext) {
			window.alert("Pre-render done in "+window.performance.now()+"ms");
			return;
		}
		
		if (document.querySelectorAll("vaadin-internals").length > 0) {
			doneAfterNext = true;
		} 
	
		if (window.performance.now() < 5000)
			window.requestAnimationFrame(raf);
		
	};

	window.requestAnimationFrame(raf);
}
/* Pre-rendering timing end */

var attempts = 1000;
loadPoller = function (){
	window.console.log("Checking for window.vaadin.framework...");
	if (!window.vaadin || !window.vaadin.framework) {
		// Bootstrap.js not yet loaded, wait for it
		if (attempts-- >= 0) {
			setTimeout(loadPoller,0);
		} else {
			window.alert("Failed to start application");
		}	
		return;
	}
	
	if (typeof window.__gwtStatsEvent != 'function') {
		vaadin.gwtStatsEvents = [];
		window.__gwtStatsEvent = function(event) {
			vaadin.gwtStatsEvents.push(event); 
			return true;
		};
	}
    var uidl = {{initialUIDL}};
    var config = {{configJSON}};
    config.uidl = uidl;
    window.vaadin.framework.initApplication("{{appId}}", config);
};

setTimeout(loadPoller,0);


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
	
	window.vaadin.framework.initApplication("{{appId}}", {{configJSON}});
};

setTimeout(loadPoller,0);


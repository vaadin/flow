if (!window.vaadin) {
	alert("Failed to load the bootstrap javascript");
}

if (typeof window.__gwtStatsEvent != 'function') {
	vaadin.gwtStatsEvents = [];
	window.__gwtStatsEvent = function(event) {
		vaadin.gwtStatsEvents.push(event); 
		return true;
	};
}

vaadin.initApplication("{{appId}}", {{configJSON}});

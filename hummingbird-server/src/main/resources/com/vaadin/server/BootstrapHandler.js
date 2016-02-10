if (!window.vaadin) {
	alert("Failed to load the bootstrap javascript");
}
{{GWT_STAT_EVENTS}}
vaadin.initApplication("{{appId}}", {{configJSON}});

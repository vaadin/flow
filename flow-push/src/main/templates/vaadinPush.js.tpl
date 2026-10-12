window.vaadinPush = window.vaadinPush|| {};
(function() {
// The atmosphere.js global build also assigns its entry points to window.
// Restore whatever was there before so only window.vaadinPush is added.
var globals = ['atmosphere', 'Atmosphere', 'AtmosphereRooms', 'subscribeStreaming'];
var previous = globals.map(function(name) {
	return Object.getOwnPropertyDescriptor(window, name);
});
@atmosphere.js@
globals.forEach(function(name, i) {
	if (previous[i]) {
		Object.defineProperty(window, name, previous[i]);
	} else {
		delete window[name];
	}
});
this.atmosphere = AtmosphereJS;
if (window.console) {
	window.console.debug("Vaadin push loaded");
}
}).call(window.vaadinPush);

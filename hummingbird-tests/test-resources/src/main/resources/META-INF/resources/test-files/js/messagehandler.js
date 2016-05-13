var logMessage = function(msg) {
	var d = document.createElement("div");
	d.innerText = msg;
	d.classList.add("message");
	document.body.appendChild(d);
}
window.addEventListener("message", function(m) {
	logMessage("Message received: "+m.data);

}, false);
logMessage("Messagehandler initialized");


var log = document.createElement("div");
log.id = "read-global-var-text";
log.textContent = "Second script loaded. Global variable (window.globalVar) is: '" + window.globalVar+"'";
document.body.insertBefore(log, null);

window.logMessage = function(msg) {
    var d = document.createElement("div");
    d.innerText = msg;
    d.classList.add("message");
    document.body.appendChild(d);
};

logMessage("HTML import 4 companion JS loaded");

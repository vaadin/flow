function attachTestDiv(textContent) {
    const div = document.createElement("div");
    div.className = "dependenciesTest";
    div.textContent = textContent;
    document.body.appendChild(div);
}

console.log("eager.js")

// Either attach message from HTML import or set handler that will attach it
if (window.messages) {
	window.messages.forEach(attachTestDiv);
} else {
	window.messages = {
	  push: attachTestDiv
	}
}

attachTestDiv("eager.js");
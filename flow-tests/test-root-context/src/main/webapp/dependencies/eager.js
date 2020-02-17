function attachTestDiv(textContent) {
  const div = document.createElement("div");
  div.className = "dependenciesTestWebRes";
  div.textContent = textContent;
  const outlet = document.getElementById('outlet') || document.body;
  outlet.appendChild(div);
}

// Attach any existing message to the DOM
if (window.webResourcesMessages && window.webResourcesMessages.forEach) {
  window.webResourcesMessages.forEach(attachTestDiv);
}

// Swap out implementation with one that directly attaches to the DOM
window.webResourcesMessages = {
  push: attachTestDiv
};

attachTestDiv("eager.js");

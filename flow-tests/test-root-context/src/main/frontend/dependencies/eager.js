function attachTestDiv(textContent) {
  const div = document.createElement("div");
  div.className = "dependenciesTest";
  div.textContent = textContent;
  const outlet = document.getElementById('outlet') || document.body;
  outlet.appendChild(div);
}

// Attach any existing message to the DOM
if (window.messages && window.messages.forEach) {
  window.messages.forEach(attachTestDiv);
}

// Swap out implementation with one that directly attaches to the DOM
window.messages = {
  push: attachTestDiv
};

attachTestDiv("eager.js");

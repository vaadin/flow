function attachTestDiv(textContent) {
  const div = document.createElement("div");
  div.className = "dependenciesTest";
  div.textContent = textContent;
  document.body.appendChild(div);
}

// Attach any existing message to the DOM
if (window.messages) {
  window.messages.forEach(attachTestDiv);
}

// Swap out implementation with one that directly attaches to the DOM
window.messages = {
  push: attachTestDiv
};

attachTestDiv("eager.js");

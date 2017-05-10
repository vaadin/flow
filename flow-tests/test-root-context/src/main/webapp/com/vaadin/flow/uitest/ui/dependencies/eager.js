function attachTestDiv(textContent) {
    const div = document.createElement("div");
    div.className = "dependenciesTest";
    div.textContent = textContent;
    document.body.appendChild(div);
}
attachTestDiv("eager.js");

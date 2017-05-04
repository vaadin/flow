function nonBlockingJs() {
    const div = document.createElement("div");
    div.className = "dependenciesTest";
    div.textContent = "non-blocking.js";
    document.body.appendChild(div);
}
nonBlockingJs();

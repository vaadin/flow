function blockingJs() {
    const div = document.createElement("div");
    div.className = "dependenciesTest";
    div.textContent = "blocking.js";
    document.body.appendChild(div);
}
blockingJs();

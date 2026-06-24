document.body.addEventListener("click", function(e) {
  if (e.target != document.body) {
    // Ignore clicks on other elements
    return;
  }
  var d = document.createElement("div");
  d.innerText = "Click on body, reported by JavaScript click handler";
  d.classList.add("body-click-added");
  document.body.appendChild(d);
});

window.Vaadin.Flow.connector = {
  initLazy: function() {
    let htmlDivElement = document.createElement("div");
    htmlDivElement.setAttribute("id", "lazy-element");
    htmlDivElement.innerHTML = "I is the Lazy Element!";
    document.body.appendChild(htmlDivElement);
  }
};

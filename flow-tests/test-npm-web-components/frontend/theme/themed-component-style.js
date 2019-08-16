const style = document.createElement("style");
style.type = "text/css";
style.appendChild(document.createTextNode(
  `:root {--after-content-var: rgba(255, 0, 0, 1);}`
));
document.head.appendChild(style);

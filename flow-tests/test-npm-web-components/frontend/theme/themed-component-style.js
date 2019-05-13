const style = document.createElement("style");
style.type = "text/css";
style.appendChild(document.createTextNode(
    `:root {--after-content-var: "a theme!";}`
));
document.head.appendChild(style);

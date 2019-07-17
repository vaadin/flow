import '@polymer/polymer/lib/elements/custom-style';
const documentContainer = document.createElement('template');

documentContainer.innerHTML = `
<custom-style>
    <style>
        html {
            --lumo-primary-color: red;
        }
    </style>
</custom-style>
`;

document.head.appendChild(documentContainer.content);

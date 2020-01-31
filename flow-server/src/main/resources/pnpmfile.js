const fs = require('fs');

const packageJson = JSON.parse(fs.readFileSync('./package.json', 'utf-8'));

const versionsFile = '[to-be-generated-by-flow]';

if (!fs.existsSync(versionsFile)) {
    return;
}
const versions = JSON.parse(fs.readFileSync(versionsFile, 'utf-8'));

let vaadinDeps;

module.exports = {
  hooks: {
    readPackage
  }
};

function getExactVersion(string) {
  return string.replace('^', '');
}


function readPackage(pkg) {
  const deps = versions;

  const { dependencies } = pkg;

  if (dependencies) {
    for (let k in deps) {
      if (dependencies[k] && dependencies[k] !== deps[k]) {
        console.log(
          `Pinned ${k} required by ${pkg.name} from ${dependencies[k]} to ${deps[k]}`
        );
        pkg.dependencies[k] = deps[k];
      }
    }
  }

  return pkg;
}
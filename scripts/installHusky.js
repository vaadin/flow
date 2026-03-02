#!/usr/bin/env node
const fs = require("fs");
const { execSync } = require("child_process");

const lockDir = ".husky/_lock";
try {
  fs.mkdirSync(lockDir);
} catch (e) {
  return; // another process or already set up
}
try {
  execSync("npx husky@9.1.7", { stdio: "inherit" });
} finally {
  fs.rmSync(lockDir, { recursive: true });
}

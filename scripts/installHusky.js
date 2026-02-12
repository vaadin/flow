#!/usr/bin/env node
const fs = require("fs");
const { execSync } = require("child_process");

try {
  fs.mkdirSync(".husky/_lock");
  execSync("npx husky@9.1.7", { stdio: "inherit" });
  fs.rmSync(".husky/_lock", { recursive: true });
} catch (e) {
  // Already exists, another process won the race or husky is already set up
}

#!/usr/bin/env python3
"""Generate flow-tests/MIGRATION_LIST.md.

Maps every current ``*IT.java`` under ``flow-tests`` to its target module and
feature package, following the decision procedure in ``flow-tests/README.md`` and
the permutation policy in ``flow-tests/MIGRATION.md``.

- Per-module and sub-package mappings are firm.
- Feature packages for the flat ``test-root-context`` ``com.vaadin.flow.uitest.ui``
  package are keyword-derived (suggested) and confirmed per migration PR. Adjust
  the ``FEAT`` heuristic / ``SUBMAP`` / ``target()`` rules here as decisions are
  finalised, then re-run.

Usage (no dependencies, stdlib only):
    python3 flow-tests/scripts/generate-migration-list.py

Runs from anywhere; locates ``flow-tests`` relative to this script and overwrites
``flow-tests/MIGRATION_LIST.md``.
"""
import os
import re
import collections

# flow-tests/ is the parent of the directory holding this script
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "MIGRATION_LIST.md")

its = []
for dirpath, dirs, files in os.walk(ROOT):
    if "/target/" in dirpath or dirpath.endswith("/target"):
        continue
    for f in files:
        if f.endswith("IT.java"):
            its.append(os.path.join(dirpath, f))


def parts(path):
    """(module, submodule, package, class, ui-subpackage) for an IT path."""
    rel = os.path.relpath(path, ROOT)
    seg = rel.split(os.sep)
    module = seg[0]
    submodule = seg[1] if len(seg) > 2 and seg[1] != "src" else ""
    m = re.search(r"src/test/java/(.+)\.java$", rel.replace(os.sep, "/"))
    pkg = ""
    cls = os.path.basename(path)[:-5]
    if m:
        pkg = "/".join(m.group(1).split("/")[:-1]).replace("/", ".")
    sub = ""
    if ".uitest.ui" in pkg:
        sub = pkg.split(".uitest.ui", 1)[1].lstrip(".")
    return module, submodule, pkg, cls, sub


# feature heuristic for the flat test-root-context ui package (first match wins)
FEAT = [
    ("templates.polymer", ["polymer", "domrepeat", "modellist", "twoway", "oneway",
                           "subproperty", "listbinding", "beaninlist", "basictypeinlist"]),
    ("templates", ["template"]),
    ("push", ["push", "poll"]),
    ("scroll", ["scroll"]),
    ("signals", ["signal"]),
    ("i18n", ["locale", "translation", "i18n"]),
    ("theming", ["theme", "css", "style", "lumo", "direction"]),
    ("errorhandling", ["error", "exception", "fault", "internalerror"]),
    ("routing", ["route", "router", "navigat", "forward", "reroute", "redirect",
                 "param", "location", "viewtitle", "popstate", "basehref",
                 "fragment", "backbutton", "historyback"]),
    ("dependencies", ["executejs", "javascript", "jsfunction", "jsinit", "jsapi",
                      "history", "webstorage", "scriptinject", "depend", "inline",
                      "streamresource", "callfunction"]),
    ("lifecycle", ["preserveonrefresh", "attach", "detach", "valuechange",
                   "synchroniz", "debounce", "beforeenter", "reattach", "resync",
                   "serializ", "uiscollected", "invalidate", "trigger",
                   "waitforvaadin", "vaadinpush"]),
    ("components", ["dnd", "drag", "dialog", "upload", "download", "image",
                    "iframe", "composite", "component", "loadingindicator",
                    "enabled", "shortcut", "modal", "findcomponent", "inmemory"]),
    ("dom", ["element", "dom", "event", "listener", "property", "shadow", "focus",
             "blur", "keyboard", "visib", "slot", "inert", "classlist",
             "namespaced", "page", "uielement"]),
    ("bootstrap", []),
]


def feat_flat(name):
    n = name.lower()
    for feat, kws in FEAT:
        if any(kw in n for kw in kws):
            return feat
    return "bootstrap"


SUBMAP = {
    "push": "push", "scroll": "scroll", "signal": "signals",
    "dependencies": "dependencies", "routing": "routing", "routerstate": "routing",
    "frontend": "dependencies", "littemplate": "templates.lit",
    "template": "templates.polymer", "template.collections": "templates.polymer",
    "template.imports": "templates.polymer", "webcomponent": "embedding",
    "faulttolerance": "__faulttol__",
}

SERVLET_NAMES = ("SyncError", "LogoutWithNotification")


def target(module, submodule, pkg, cls, sub):
    """(target_module, feature, type) where type is move|reuse|keep|done."""
    if module == "test-root-context":
        if any(s in cls for s in SERVLET_NAMES):
            return ("test-plain-servlet", "servlet", "move")
        if "ProdMode" in cls:
            return ("test-production", "routing", "move")
        if pkg == "com.vaadin.flow":
            return ("test-default", "bootstrap", "move")
        if sub == "":  # flat ui package
            return ("test-default", feat_flat(cls), "move")
        feat = SUBMAP.get(sub)
        if feat == "__faulttol__":
            return ("test-fault-tolerance(keep)", "fault-tolerance", "keep")
        return ("test-default", feat or "bootstrap", "move")
    if module == "test-default":
        return ("test-default", "(already migrated)", "done")

    M = module
    if M == "test-dev-mode":
        return ("test-default", "devmode", "move")
    if M == "test-react-router":
        return ("test-default", "routing", "move")
    if M == "test-react-adapter":
        return ("test-default", "react", "move")
    if M == "test-vaadin-router":
        return ("test-vaadin-router", "routing", "reuse")
    if M in ("test-ccdm", "test-ccdm-flow-navigation"):
        return ("test-vaadin-router", "routing", "move")
    if M in ("test-router-custom-context", "test-router-custom-context-encoded",
             "test-router-custom-context-encoded-prod"):
        return ("test-contextpath", "routing", "move")
    if M in ("test-themes", "test-application-theme", "test-theme-no-polymer"):
        return ("test-themes", "theming", "move")
    if M == "test-no-theme":
        return ("test-no-theme", "theming", "move")
    if M == "test-tailwindcss":
        return ("test-tailwind", "theming", "move")
    if M in ("test-pwa", "test-pwa-disabled-offline", "test-webpush"):
        return ("test-pwa", "pwa", "move")
    if M == "test-embedding":
        return ("test-embedding", "embedding", "move")
    if M in ("test-custom-frontend-directory", "test-legacy-frontend"):
        return ("test-custom-frontend-directory", "frontend", "move")
    if M in ("test-live-reload", "test-live-reload-multimodule",
             "test-live-reload-multimodule-devbundle"):
        return ("test-livereload", "devmode", "move")
    if M in ("test-redeployment", "test-redeployment-no-cache"):
        return ("test-redeployment", "devmode", "move")
    if M == "test-eager-bootstrap":
        return ("test-eager-bootstrap", "bootstrap", "move")
    if M == "test-servlet":
        return ("test-plain-servlet", "servlet", "move")
    if M == "test-custom-route-registry":
        return ("test-plain-servlet", "routing", "move")
    if M == "test-client-queue":
        return ("test-plain-servlet", "devmode", "move")
    if M == "test-misc":
        return ("test-default", "misc", "move")
    if M in ("test-multi-war", "servlet-containers", "test-commercial-banner"):
        return (M + "(keep)", "infra", "keep")
    if M == "test-express-build":
        s = (submodule or "").lower()
        if "prod" in s:
            return ("test-production", "frontend", "move")
        if "embedding" in s:
            return ("test-embedding", "embedding", "move")
        if "theme" in s:
            return ("test-themes", "theming", "move")
        return ("test-dev-bundle", "frontend", "move")
    if M == "test-frontend":
        s = (submodule or "").lower()
        if "pnpm" in s:
            return ("test-pnpm", "frontend", "move")
        if "bun" in s:
            return ("test-bun", "frontend", "move")
        if "pwa" in s:
            return ("test-pwa", "pwa", "move")
        if "embedd" in s:
            return ("test-embedding", "embedding", "move")
        if "context" in s:
            return ("test-contextpath", "routing", "move")
        if "prod" in s:
            return ("test-production", "frontend", "move")
        return ("test-default", "frontend", "move")
    if M == "test-npm-only-features":
        s = (submodule or "").lower()
        if "performance" in s:
            return ("test-performance-regression(keep)", "infra", "keep")
        if "custom-frontend" in s:
            return ("test-custom-frontend-directory", "frontend", "move")
        if "bytecode" in s or "prod" in s:
            return ("test-production", "frontend", "move")
        return ("test-default", "frontend", "move")
    if M == "vaadin-spring-tests":
        s = (submodule or "").lower()
        if "security" in s or "webicons" in s:
            return ("test-spring-security", "security", "move")
        if "mvc" in s:
            return ("test-plain-spring", "di", "move")
        if "reload-time" in s:
            return ("(benchmark, keep)", "benchmark", "keep")
        if "filter-packages" in s or "white-list" in s:
            return ("test-default", "di", "move")
        if "contextpath" in s:
            return ("test-contextpath", "routing", "reuse")
        return ("test-default", "di", "move")
    return ("?UNMAPPED?", "?", "?")


rows = []
for p in its:
    module, submodule, pkg, cls, sub = parts(p)
    tm, feat, typ = target(module, submodule, pkg, cls, sub)
    src = module + (("/" + submodule) if submodule else "")
    rows.append((tm, feat, src, cls, typ))

bytarget = collections.Counter(r[0] for r in rows)
print("TOTAL ITs:", len(rows))
for k, v in sorted(bytarget.items(), key=lambda x: -x[1]):
    print(f"{v:4d}  {k}")
unmapped = [r for r in rows if r[0] == "?UNMAPPED?"]
if unmapped:
    print("\nUNMAPPED:", len(unmapped))
    for r in unmapped:
        print("   ", r[2], r[3])

out = [
    "# flow-tests migration list (current IT -> target)",
    "",
    "Generated by `scripts/generate-migration-list.py`. Maps every current",
    "`*IT.java` to its target module and feature package, per the decision",
    "procedure in [README.md](README.md) and the permutation policy in",
    "[MIGRATION.md](MIGRATION.md).",
    "",
    "- **type**: `move` = source home moves here; `reuse` = runs here as a",
    "  permutation by reusing another module's test-jar (no source copy); `keep` =",
    "  stays in its own special-infra module.",
    "- Feature packages for the flat `test-root-context` `ui` package are",
    "  **suggested** (keyword-derived) and confirmed per migration PR; sub-packaged",
    "  and per-module mappings are firm.",
    "",
    "## Summary (ITs per target)",
    "", "| Target | ITs |", "|---|---|",
]
for k, v in sorted(bytarget.items(), key=lambda x: (-x[1], x[0])):
    out.append(f"| {k} | {v} |")
out.append(f"| **TOTAL** | **{len(rows)}** |")
out.append("")
out.append("## Mapping (grouped by target module, then feature)")
rows.sort(key=lambda r: (r[0], r[1], r[2], r[3]))
cur = curf = None
for tm, feat, src, cls, typ in rows:
    if tm != cur:
        out.append(f"\n### → {tm}\n")
        cur, curf = tm, None
    if feat != curf:
        out.append(f"\n**`{feat}`**\n")
        out.append("| IT | from | type |")
        out.append("|---|---|---|")
        curf = feat
    out.append(f"| {cls} | {src} | {typ} |")

with open(OUT, "w") as fh:
    fh.write("\n".join(out) + "\n")
print("\nwrote", os.path.relpath(OUT))

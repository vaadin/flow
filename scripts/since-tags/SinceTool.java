///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21+
//DEPS com.github.javaparser:javaparser-core:3.26.4

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.type.Type;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Reconciles Javadoc {@code @since} tags against the actual release history of a
 * library, using the published *source* artifacts as ground truth.
 *
 * Two modes:
 *
 *   index <srcRoot> <outTsv>
 *       srcRoot contains one subdir per full version (e.g. 2.1.4/, 25.2.0-rc2/).
 *       Emits "key\tv1,v2,..." -- the full set of versions each public/protected
 *       API element appears in.
 *
 *   apply <currentSrcRoot> <indexDir> <devMinor> <dry|write> <reportFile> [all|add|update]
 *       indexDir holds one or more *.tsv index files (merged). For every
 *       public/protected API element, @since is the start of the contiguous run of
 *       presence (over all releases with minor <= devMinor) reaching the latest
 *       release; elements absent from the latest release are assigned devMinor.
 *       Maintenance-release intros get the full patch (25.1.4); .0 GAs and
 *       pre-releases get the bare minor. The optional filter splits a run into
 *       additions (add) and corrections+removals (update).
 *
 * The same signature-key normalization is used on both sides, so historic and
 * current source compare apples-to-apples without symbol resolution.
 */
public class SinceTool {

    static JavaParser parser() {
        ParserConfiguration cfg = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                .setAttributeComments(true);
        return new JavaParser(cfg);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("modes: index <srcRoot> <outTsv> | apply <srcRoot> <indexDir> <devMinor> <dry|write> <report> [all|add|update]");
            System.exit(2);
        }
        switch (args[0]) {
            case "index" -> index(args[1], args[2]);
            case "apply" -> apply(args[1], args[2], args[3], args[4].equals("write"), args[5],
                    args.length > 6 ? args[6] : "all");
            default -> throw new IllegalArgumentException("unknown mode: " + args[0]);
        }
    }

    // ---------- signature keys ----------

    static String stripGenerics(String s) {
        while (s.contains("<")) {
            String n = s.replaceAll("<[^<>]*>", "");
            if (n.equals(s)) break;
            s = n;
        }
        return s;
    }

    static String simpleName(String s) {
        int arr = s.indexOf('[');
        String base = arr < 0 ? s : s.substring(0, arr);
        String suffix = arr < 0 ? "" : s.substring(arr).replaceAll("\\s", "");
        int dot = base.lastIndexOf('.');
        if (dot >= 0) base = base.substring(dot + 1);
        return base + suffix;
    }

    /** A parameter type, generics-stripped and reduced to its simple name so it
     * stays stable across version-to-version package moves and qualification. */
    static String normType(Type type, boolean varargs) {
        String s = stripGenerics(type.toString()).replaceAll("\\s+", "");
        if (varargs) s = s + "[]";
        return simpleName(s);
    }

    static String paramsOf(List<Parameter> ps) {
        return ps.stream()
                .map(p -> normType(p.getType(), p.isVarArgs()))
                .collect(Collectors.joining(","));
    }

    // ---------- visibility ----------

    static boolean typeIsApi(TypeDeclaration<?> t) {
        TypeDeclaration<?> cur = t;
        while (cur != null) {
            boolean topLevel = cur.getParentNode()
                    .map(p -> !(p instanceof TypeDeclaration)).orElse(true);
            boolean ok = topLevel ? cur.isPublic() : (cur.isPublic() || cur.isProtected());
            if (!ok) return false;
            cur = cur.findAncestor(TypeDeclaration.class).orElse(null);
        }
        return true;
    }

    static boolean memberIsApi(BodyDeclaration<?> m, TypeDeclaration<?> owner) {
        boolean interfaceLike = (owner instanceof ClassOrInterfaceDeclaration c && c.isInterface())
                || owner instanceof AnnotationDeclaration;
        if (m instanceof MethodDeclaration md)
            return interfaceLike ? !md.isPrivate() : (md.isPublic() || md.isProtected());
        if (m instanceof ConstructorDeclaration cd)
            return cd.isPublic() || cd.isProtected();
        if (m instanceof FieldDeclaration fd)
            return interfaceLike ? !fd.isPrivate() : (fd.isPublic() || fd.isProtected());
        if (m instanceof EnumConstantDeclaration) return true;
        if (m instanceof AnnotationMemberDeclaration) return true;
        return false;
    }

    /** enclosingTypeKey is the "T:fqn" of the directly enclosing type (a member's
     * declaring type, or a nested type's outer type), or null for a top-level
     * type. Used to suppress @since that merely repeats the enclosing type's. */
    record NodeKey(BodyDeclaration<?> node, String key, String enclosingTypeKey) {}

    /** All public/protected API elements of a compilation unit, paired with the
     * node that carries (or should carry) the @since tag.
     *
     * synthImplicitCtor (index side only): when a class declares no explicit
     * constructor it has a compiler-generated implicit default constructor with
     * the class's own visibility, which the parser never sees. Record its
     * presence as M:fqn#<init>() so the no-arg constructor's presence set spans
     * from the class's first release. Without this, a later *explicit* no-arg
     * constructor (often added just to attach logic/Javadoc) looks new, and the
     * tool dates it to that late version instead of the class's version. Off on
     * the apply side: there is no source node to attach a tag to, and a no-arg
     * constructor is never given a freshly-created @since. */
    static List<NodeKey> elements(CompilationUnit cu, boolean synthImplicitCtor) {
        List<NodeKey> out = new ArrayList<>();
        for (TypeDeclaration<?> t : cu.findAll(TypeDeclaration.class)) {
            if (!typeIsApi(t)) continue;
            String fqn = t.getFullyQualifiedName().orElse(t.getNameAsString());
            String selfTypeKey = "T:" + fqn;
            String outerTypeKey = t.findAncestor(TypeDeclaration.class)
                    .map(a -> "T:" + a.getFullyQualifiedName().orElse(a.getNameAsString()))
                    .orElse(null);
            out.add(new NodeKey(t, selfTypeKey, outerTypeKey));
            if (synthImplicitCtor && t instanceof ClassOrInterfaceDeclaration c
                    && !c.isInterface() && c.getConstructors().isEmpty())
                out.add(new NodeKey(t, "M:" + fqn + "#<init>()", selfTypeKey));
            for (BodyDeclaration<?> m : t.getMembers()) {
                if (!memberIsApi(m, t)) continue;
                if (m instanceof MethodDeclaration md)
                    out.add(new NodeKey(md, "M:" + fqn + "#" + md.getNameAsString() + "(" + paramsOf(md.getParameters()) + ")", selfTypeKey));
                else if (m instanceof ConstructorDeclaration cd)
                    out.add(new NodeKey(cd, "M:" + fqn + "#<init>(" + paramsOf(cd.getParameters()) + ")", selfTypeKey));
                else if (m instanceof FieldDeclaration fd)
                    for (VariableDeclarator v : fd.getVariables())
                        out.add(new NodeKey(fd, "F:" + fqn + "#" + v.getNameAsString(), selfTypeKey));
                else if (m instanceof AnnotationMemberDeclaration am)
                    out.add(new NodeKey(am, "M:" + fqn + "#" + am.getNameAsString() + "()", selfTypeKey));
            }
            // Enum constants live in getEntries(), NOT getMembers() -- handle separately
            // (they are implicitly public, so always part of the API).
            if (t instanceof EnumDeclaration ed)
                for (EnumConstantDeclaration ec : ed.getEntries())
                    out.add(new NodeKey(ec, "F:" + fqn + "#" + ec.getNameAsString(), selfTypeKey));
        }
        return out;
    }

    // ---------- index ----------

    static List<Path> javaFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.getFileName().toString().equals("module-info.java"))
                    .filter(f -> !f.getFileName().toString().equals("package-info.java"))
                    .sorted().collect(Collectors.toList());
        }
    }

    /** Index every release: srcRoot has one subdir per full version (e.g. 2.1.4/,
     * 25.2.0-rc2/). Emits "key\tv1,v2,..." -- the full set of versions each
     * public/protected API element appears in, sorted by version. */
    static void index(String moduleSrcRoot, String outTsv) throws Exception {
        Map<String, Set<String>> present = new HashMap<>();
        int parsed = 0, failed = 0;
        List<Path> verDirs;
        try (Stream<Path> s = Files.list(Path.of(moduleSrcRoot))) {
            verDirs = s.filter(Files::isDirectory).sorted().collect(Collectors.toList());
        }
        for (Path dir : verDirs) {
            String ver = dir.getFileName().toString();
            for (Path f : javaFiles(dir)) {
                ParseResult<CompilationUnit> r;
                try { r = parser().parse(f); } catch (Exception e) { failed++; continue; }
                if (r.getResult().isEmpty()) { failed++; continue; }
                parsed++;
                for (NodeKey nk : elements(r.getResult().get(), true))
                    present.computeIfAbsent(nk.key(), k -> new HashSet<>()).add(ver);
            }
        }
        try (var w = Files.newBufferedWriter(Path.of(outTsv), StandardCharsets.UTF_8)) {
            for (var e : new TreeMap<>(present).entrySet())
                w.write(e.getKey() + "\t" + e.getValue().stream().sorted(SinceTool::cmpFullVersion).collect(Collectors.joining(",")) + "\n");
        }
        System.err.println("INDEX done: " + present.size() + " keys, parsed=" + parsed + " failed=" + failed + " -> " + outTsv);
    }

    // ---------- apply ----------

    static int cmpMinor(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        int c = Integer.compare(Integer.parseInt(x[0]), Integer.parseInt(y[0]));
        return c != 0 ? c : Integer.compare(Integer.parseInt(x[1]), Integer.parseInt(y[1]));
    }

    static String minorOf(String v) { String[] p = v.split("\\."); return p[0] + "." + p[1]; }

    /** Normalize an ancient dot-style pre-release (2.2.0.alpha11) to dash-style
     * (2.2.0-alpha11) so it sorts before, and stays distinct from, its GA. */
    static String norm(String v) { return v.replaceFirst("\\.(alpha|beta|rc|m)", "-$1"); }

    static int patchNum(String v) {
        String[] p = norm(v).split("[.-]");
        return p.length > 2 ? Integer.parseInt(p[2].replaceAll("\\D.*$", "")) : 0;
    }

    /** @since value for a release: a maintenance patch -> X.Y.Z; a .0 GA or any
     * pre-release -> X.Y. */
    static String fmt(String v) {
        v = norm(v);
        String[] p = v.split("[.-]");
        return (v.contains("-") || patchNum(v) == 0) ? p[0] + "." + p[1] : p[0] + "." + p[1] + "." + patchNum(v);
    }

    /** Full version order: major.minor.patch numerically; a pre-release sorts
     * before its GA (25.2.0-rc2 < 25.2.0), pre-releases by suffix. */
    static int cmpFullVersion(String a, String b) {
        a = norm(a); b = norm(b);
        String[] ap = a.split("-", 2), bp = b.split("-", 2);
        String[] an = ap[0].split("\\."), bn = bp[0].split("\\.");
        for (int i = 0; i < 3; i++) {
            int x = i < an.length ? Integer.parseInt(an[i]) : 0, y = i < bn.length ? Integer.parseInt(bn[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        boolean aPre = ap.length > 1, bPre = bp.length > 1;
        if (aPre != bPre) return aPre ? -1 : 1;
        return aPre ? ap[1].compareTo(bp[1]) : 0;
    }

    static Map<String, Set<String>> loadIndex(String indexDir) throws IOException {
        Map<String, Set<String>> idx = new HashMap<>();
        try (Stream<Path> s = Files.walk(Path.of(indexDir))) {
            for (Path f : s.filter(p -> p.toString().endsWith(".tsv")).collect(Collectors.toList()))
                for (String line : Files.readAllLines(f)) {
                    int t = line.indexOf('\t');
                    if (t < 0) continue;
                    Set<String> set = idx.computeIfAbsent(line.substring(0, t), x -> new HashSet<>());
                    for (String v : line.substring(t + 1).trim().split(","))
                        if (!v.isEmpty()) set.add(v);
                }
        }
        return idx;
    }

    /** @since = start of the contiguous run of presence over the release axis (all
     * releases at patch granularity) that reaches the latest release. Patch-level
     * contiguity excludes backports with no dates: a backport into 25.0.10 is
     * dropped because 25.1.0/25.1.1 (later in version order) lack the API, breaking
     * the streak before it reaches 25.0.10. Returns devMinor when the element isn't
     * in the latest release (new on this branch). */
    static String computeSince(String key, Map<String, Set<String>> idx, List<String> axis, String latest, String devMinor) {
        Set<String> present = idx.get(key);
        if (latest == null || present == null || !present.contains(latest)) return devMinor;
        int i = axis.size() - 1;
        while (i >= 0 && present.contains(axis.get(i))) i--;
        return fmt(axis.get(i + 1));
    }

    // kind: 0=replace line, 1=insert before line, 2=delete lines [line0..delTo]
    record Op(int line0, int kind, String text, int delTo) {}

    static boolean isBlankStar(String line) { return line.matches("\\s*\\*\\s*"); }

    static void apply(String currentSrcRoot, String indexDir, String devMinor, boolean write, String reportFile,
            String filter) throws Exception {
        // filter: "all" (default) | "add" (only insert missing tags) | "update"
        // (only correct wrong tags and remove redundant ones). Lets a combined run
        // be split into an additions commit and an updates commit.
        boolean doAdd = filter.equals("all") || filter.equals("add");
        boolean doUpd = filter.equals("all") || filter.equals("update");
        Map<String, Set<String>> idx = loadIndex(indexDir);
        // Release axis: every released version whose minor is <= devMinor (so the
        // result is relative to this branch's lineage), sorted by full version.
        TreeSet<String> axisSet = new TreeSet<>(SinceTool::cmpFullVersion);
        for (Set<String> vs : idx.values())
            for (String v : vs)
                if (cmpMinor(minorOf(v), devMinor) <= 0) axisSet.add(v);
        List<String> axis = new ArrayList<>(axisSet);
        String latest = axis.isEmpty() ? null : axis.get(axis.size() - 1);
        int correct = 0, fixed = 0, filled = 0, newDev = 0, removed = 0, created = 0, noJavadoc = 0, singleLine = 0;
        List<String> fixedL = new ArrayList<>(), filledL = new ArrayList<>(), newDevL = new ArrayList<>(),
                removedL = new ArrayList<>(), createdL = new ArrayList<>(), noDocL = new ArrayList<>();

        for (Path f : javaFiles(Path.of(currentSrcRoot))) {
            List<String> raw = new ArrayList<>(Files.readAllLines(f, StandardCharsets.UTF_8));
            ParseResult<CompilationUnit> r = parser().parse(f);
            if (r.getResult().isEmpty()) { System.err.println("parse fail " + f); continue; }
            CompilationUnit cu = r.getResult().get();

            List<Op> ops = new ArrayList<>();

            for (NodeKey nk : elements(cu, false)) {
                String desired = computeSince(nk.key(), idx, axis, latest, devMinor);
                boolean isNew = desired.equals(devMinor);
                String enclVer = nk.enclosingTypeKey() == null ? null
                        : computeSince(nk.enclosingTypeKey(), idx, axis, latest, devMinor);
                boolean redundant = enclVer != null && desired.equals(enclVer);
                String loc = rel(currentSrcRoot, f) + "  " + nk.key() + " -> " + desired;

                Optional<Comment> oc = nk.node().getComment().filter(Comment::isJavadocComment);

                // locate an existing @since line within the comment, if any
                int begin = -1, end = -1, sinceLine = -1;
                if (oc.isPresent()) {
                    begin = oc.get().getRange().get().begin.line;
                    end = oc.get().getRange().get().end.line;
                    for (int ln = begin; ln <= end; ln++)
                        if (raw.get(ln - 1).matches(".*@since\\b.*")) { sinceLine = ln; break; }
                }

                if (redundant) {
                    // A member/nested-type @since equal to its enclosing type's is
                    // noise: never add one, and strip any that already exists (update).
                    if (sinceLine >= 0 && doUpd) {
                        int from = sinceLine, to = sinceLine; // 1-based
                        if (sinceLine == end - 1 && isBlankStar(raw.get(sinceLine - 2)))
                            from = sinceLine - 1; // also drop the now-orphaned blank ' *' line
                        ops.add(new Op(from - 1, 2, null, to - 1));
                        removed++; removedL.add(loc + "   (removed, == enclosing type @since " + enclVer + ")");
                    }
                    continue;
                }

                if (oc.isEmpty()) {
                    // A type with no javadoc would lose its @since entirely (members at
                    // the same version are suppressed as redundant). Create a minimal
                    // javadoc carrying just the @since rather than tagging every member.
                    // Members without javadoc stay report-only.
                    if (doAdd && nk.node() instanceof TypeDeclaration<?> td) {
                        int declLine = td.getBegin().get().line; // declaration (or its first annotation)
                        for (var ann : td.getAnnotations())
                            declLine = Math.min(declLine, ann.getBegin().get().line);
                        // Don't create if a javadoc already sits just above, detached from
                        // the declaration only by annotations / line comments / blanks --
                        // JavaParser won't attach it, but it IS the type's doc, so a second
                        // block would double it.
                        boolean detachedDoc = false;
                        for (int ln = declLine - 1; ln >= 1; ln--) {
                            String s = raw.get(ln - 1).trim();
                            if (s.isEmpty() || s.startsWith("@") || s.startsWith("//")) continue;
                            detachedDoc = s.endsWith("*/");
                            break;
                        }
                        if (detachedDoc) {
                            noJavadoc++; noDocL.add(loc + "  [type doc detached by comments; not created]");
                            continue;
                        }
                        String indent = raw.get(declLine - 1).replaceAll("\\S.*$", "");
                        ops.add(new Op(declLine - 1, 1,
                                indent + "/**\n" + indent + " * @since " + desired + "\n" + indent + " */", -1));
                        created++; createdL.add(loc);
                    } else {
                        noJavadoc++; noDocL.add(loc + (isNew ? "  [new]" : ""));
                    }
                    continue;
                }

                if (sinceLine >= 0) {
                    String cur = raw.get(sinceLine - 1);
                    String existing = cur.replaceAll(".*@since\\b\\s*", "").replaceAll("\\s.*$", "").replaceAll("\\.$", "");
                    if (existing.equals(desired)) { correct++; continue; }
                    if (doUpd) { // correcting an existing tag is an update
                        ops.add(new Op(sinceLine - 1, 0, cur.replaceAll("@since\\b.*$", "@since " + desired), -1));
                        fixed++; fixedL.add(loc + "   (was " + existing + ")");
                    }
                } else {
                    if (begin == end) { singleLine++; noDocL.add(loc + "  [single-line javadoc, no @since]"); continue; }
                    if (doAdd) { // inserting a missing tag is an addition
                        String indent = raw.get(end - 1).replaceAll("\\S.*$", ""); // align '*' with closing '*/'
                        ops.add(new Op(end - 1, 1, indent + "* @since " + desired, -1));
                        if (isNew) { newDev++; newDevL.add(loc); } else { filled++; filledL.add(loc); }
                    }
                }
            }

            if (ops.isEmpty()) continue;
            ops.sort((a, b) -> Integer.compare(b.line0(), a.line0())); // bottom-up keeps indices valid
            for (Op op : ops) switch (op.kind()) {
                case 0 -> raw.set(op.line0(), op.text());
                case 1 -> raw.addAll(op.line0(), Arrays.asList(op.text().split("\n", -1)));
                case 2 -> raw.subList(op.line0(), op.delTo() + 1).clear();
            }
            if (write) Files.write(f, (String.join("\n", raw) + "\n").getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder rep = new StringBuilder("# @since reconciliation report\n\n");
        rep.append("source root: ").append(currentSrcRoot).append("  (filter: ").append(filter).append(")\n");
        rep.append("correct (already right):     ").append(correct).append("\n");
        rep.append("fixed (wrong -> right):      ").append(fixed).append("\n");
        rep.append("filled (missing tag):        ").append(filled).append("\n");
        rep.append("new in dev (").append(devMinor).append("):        ").append(newDev).append("\n");
        rep.append("removed (redundant):         ").append(removed).append("\n");
        rep.append("created javadoc (type):      ").append(created).append("\n");
        rep.append("no javadoc (reported):       ").append(noJavadoc).append("\n");
        rep.append("single-line javadoc:         ").append(singleLine).append("\n");
        section(rep, "FIXED (wrong version corrected)", fixedL);
        section(rep, "FILLED (missing @since added; member existed in a release)", filledL);
        section(rep, "NEW IN DEV (not found in any release -> " + devMinor + "; REVIEW for false positives)", newDevL);
        section(rep, "REMOVED (redundant: same @since as enclosing type)", removedL);
        section(rep, "CREATED JAVADOC (type had none; minimal /** @since X */ added)", createdL);
        section(rep, "NO JAVADOC (public/protected member without javadoc; not touched)", noDocL);
        Files.writeString(Path.of(reportFile), rep.toString());
        System.err.println("APPLY " + (write ? "(write)" : "(dry)") + ": correct=" + correct + " fixed=" + fixed
                + " filled=" + filled + " newDev=" + newDev + " removed=" + removed + " created=" + created
                + " noJavadoc=" + noJavadoc + " -> " + reportFile);
    }

    static String rel(String root, Path f) {
        try { return Path.of(root).toAbsolutePath().relativize(f.toAbsolutePath()).toString(); }
        catch (Exception e) { return f.toString(); }
    }

    static void section(StringBuilder sb, String title, List<String> lines) {
        sb.append("\n## ").append(title).append(" (").append(lines.size()).append(")\n");
        Collections.sort(lines);
        for (String l : lines) sb.append(l).append("\n");
    }
}

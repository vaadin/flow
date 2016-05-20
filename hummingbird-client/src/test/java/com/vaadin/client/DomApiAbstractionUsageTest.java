package com.vaadin.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.client.communication.DefaultReconnectDialog;
import com.vaadin.client.hummingbird.dom.DomApi;
import com.vaadin.client.hummingbird.dom.DomElement;
import com.vaadin.client.hummingbird.dom.DomNode;

public class DomApiAbstractionUsageTest {
    private static final Path location = new File(".").toPath();
    private static final Path javaLocation = location.resolve("src/main/java");

    private static final List<String> blackListedMethodNames = new ArrayList<>();
    private static final List<String> whiteListedFiles = Arrays.asList(
            new String[] { DomElement.class.getName(), DomNode.class.getName(),
                    ResourceLoader.class.getName(), BrowserInfo.class.getName(),
                    DefaultReconnectDialog.class.getName(),
                    SystemErrorHandler.class.getName(),
                    LoadingIndicator.class.getName(),
                    Profiler.class.getName() });

    // checks for line starting with DomApi.wrap(variableName).
    //@formatter:off
    private static final String methodPrecedesWrapPatternString =
                    "DomApi" // DomApi
                    + "\\.wrap\\(" // .wrap(
                    + "\\w+" // variable name
                    + "[\\.1\\w*]*" // optionally more variables separated with .
                    + "\\)\\.%s{1}\\("; // ).
    // same as above, but matches end of line with $,
    //@formatter:on

    // checks for DomApi.wrap(element) call from previous line because of line
    // wrapping, e.g. next line must start with .methodCall()
    private static final Pattern endsWithPattern = Pattern
            .compile("DomApi\\.wrap\\(\\w+[\\.1\\w*]*\\)$");

    private static final Map<String, Pattern> methodToPatternMap = new HashMap<>();

    private static final Map<String, Path> classNameToPathMap = new HashMap<>();

    @BeforeClass
    public static void setup() throws IOException {
        // read files
        Files.walk(javaLocation).filter(DomApiAbstractionUsageTest::filter)
                .forEach(DomApiAbstractionUsageTest::addFile);

        addBlackListedMethods(DomElement.class);
    }

    private static void addBlackListedMethods(Class<?> clazz) {
        Stream.of(clazz.getMethods()).forEach(
                method -> blackListedMethodNames.add(method.getName()));
    }

    /**
     * This tests that no API from {@link DomElement} or {@link DomNode} is used
     * without wrapping it with a {@link DomApi#wrap(elemental.dom.Node)} call.
     * <p>
     * Needs to be updated to take nested calls also into account, e.g.
     * <code>DomApi.wrap(element).getFirstElemenetChild().appendChild(anotherElement);</code>
     * , which should have the returned element also wrapped.
     */
    @Test
    public void testDomApiCodeNotUsed() throws IOException {
        StringBuilder sb = new StringBuilder();
        AtomicInteger fails = new AtomicInteger();
        for (Entry<String, Path> entry : classNameToPathMap.entrySet()) {
            Stream<String> lines = Files.lines(entry.getValue())
                    .filter(DomApiAbstractionUsageTest::filterComments);
            String previousLine = "";

            for (String line : lines.toArray(String[]::new)) {
                final String trimmedLine = line.trim();

                for (String blackListed : blackListedMethodNames) {
                    if (trimmedLine.contains(blackListed + "(")) {
                        // if someone is chaining the calls, just fail (for now)
                        int matches = StringUtils.countMatches(trimmedLine,
                                blackListed);
                        if (matches > 1) {
                            sb.append("\n[").append(blackListed)
                                    .append("] is used ").append(matches)
                                    .append(" times in [").append(trimmedLine)
                                    .append("] @ ").append(entry.getKey());
                            fails.incrementAndGet();
                        } else

                        if (!doesPreviousLineEndWithWrap(previousLine,
                                trimmedLine, blackListed)
                                && !doesCallPrecideWrap(trimmedLine,
                                        blackListed)) {

                            sb.append("\n[").append(blackListed)
                                    .append("] used in [").append(trimmedLine)
                                    .append("] @ ").append(entry.getKey());
                            fails.incrementAndGet();
                        }
                    }
                }
                previousLine = trimmedLine;
            }
        }

        if (sb.length() > 0)

        {
            Assert.fail("Number of fails: " + fails.toString() + sb.toString());
        }
    }

    private boolean doesPreviousLineEndWithWrap(String previousLine,
            String trimmedLine, String blackListedMethod) {
        return endsWithPattern.matcher(previousLine).find()
                && trimmedLine.startsWith("." + blackListedMethod);
    }

    private boolean doesCallPrecideWrap(String trimmedLine,
            String blackListedMethod) {
        Pattern pattern = methodToPatternMap.get(blackListedMethod);
        if (pattern == null) {
            String format = String.format(methodPrecedesWrapPatternString,
                    blackListedMethod);
            pattern = Pattern.compile(format);
            methodToPatternMap.put(blackListedMethod, pattern);
        }
        // case where there are multiple matches been failed earlier
        return pattern.matcher(trimmedLine).find();
    }

    private static boolean filterComments(String line) {
        String trimmed = line.trim();
        return !(trimmed.startsWith("/*") || trimmed.startsWith("*")
                || trimmed.startsWith("// "));
    }

    private static boolean filter(Path path) {
        String fileName = path.toFile().getName();
        return fileName.endsWith(".java");
    }

    private static void addFile(Path path) {
        String className = javaLocation.relativize(path).toString()
                .replace('/', '.').replaceAll("\\.java$", "");

        if (!whiteListedFiles.contains(className)) {
            classNameToPathMap.put(className, path);
        }
    }
}

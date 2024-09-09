/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.base.devserver.stats;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.pro.licensechecker.MachineId;

import elemental.json.Json;
import elemental.json.JsonObject;

@NotThreadSafe
public class DevModeUsageStatisticsTest extends AbstractStatisticsTest {

    @Before
    @After
    public void clearUsageStatistic() throws Exception {
        UsageStatistics.resetEntries();
    }

    @Test
    public void clientData() throws Exception {
        // Init using test project
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        String data = IOUtils.toString(
                TestUtils.getTestResource("stats-data/client-data-1.txt"),
                StandardCharsets.UTF_8);
        DevModeUsageStatistics.handleBrowserData(wrapStats(data));
    }

    @Test
    public void projectId() throws Exception {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        Assert.assertEquals(
                "pom" + ProjectHelpers.createHash("com.exampledemo"),
                storage.getProjectId());
    }

    @Test
    public void sourceIdMaven1() throws Exception {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        ObjectNode json = storage.readProject();
        Assert.assertEquals("https://start.vaadin.com/test/1",
                json.get(StatisticsConstants.FIELD_SOURCE_ID).asText());
    }

    @Test
    public void sourceIdMaven2() throws Exception {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder2");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        ObjectNode json = storage.readProject();
        Assert.assertEquals("https://start.vaadin.com/test/2",
                json.get(StatisticsConstants.FIELD_SOURCE_ID).asText());
    }

    @Test
    public void sourceIdGradle1() throws Exception {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/gradle-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        ObjectNode json = storage.readProject();
        Assert.assertEquals("https://start.vaadin.com/test/3",
                json.get(StatisticsConstants.FIELD_SOURCE_ID).asText());
    }

    @Test
    public void sourceIdGradle2() throws Exception {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/gradle-project-folder2");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        ObjectNode json = storage.readProject();
        Assert.assertEquals("https://start.vaadin.com/test/4",
                json.get(StatisticsConstants.FIELD_SOURCE_ID).asText());
    }

    @Test
    public void aggregates() throws Exception {
        // Init using test project
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        // Averate events
        DevModeUsageStatistics.collectEvent("aggregate", 1);

        StatisticsContainer projectData = new StatisticsContainer(
                storage.readProject());

        Assert.assertEquals("Min does not match", 1,
                projectData.getValueAsDouble("aggregate_min"), 0);
        Assert.assertEquals("Max does not match", 1,
                projectData.getValueAsDouble("aggregate_max"), 0);
        Assert.assertEquals("Average does not match", 1,
                projectData.getValueAsDouble("aggregate_avg"), 0);
        Assert.assertEquals("Count does not match", 1,
                projectData.getValueAsInt("aggregate_count"));

        DevModeUsageStatistics.collectEvent("aggregate", 2);
        projectData = new StatisticsContainer(storage.readProject());
        Assert.assertEquals("Min does not match", 1,
                projectData.getValueAsDouble("aggregate_min"), 0);
        Assert.assertEquals("Max does not match", 2,
                projectData.getValueAsDouble("aggregate_max"), 0);
        Assert.assertEquals("Average does not match", 1.5,
                projectData.getValueAsDouble("aggregate_avg"), 0);
        Assert.assertEquals("Count does not match", 2,
                projectData.getValueAsInt("aggregate_count"));

        DevModeUsageStatistics.collectEvent("aggregate", 3);
        projectData = new StatisticsContainer(storage.readProject());

        Assert.assertEquals("Min does not match", 1,
                projectData.getValueAsDouble("aggregate_min"), 0);
        Assert.assertEquals("Max does not match", 3,
                projectData.getValueAsDouble("aggregate_max"), 0);
        Assert.assertEquals("Average does not match", 2,
                projectData.getValueAsDouble("aggregate_avg"), 0);
        Assert.assertEquals("Count does not match", 3,
                projectData.getValueAsInt("aggregate_count"));

        // Test count events
        DevModeUsageStatistics.collectEvent("count");
        projectData = new StatisticsContainer(storage.readProject());

        Assert.assertEquals("Increment does not match", 1,
                projectData.getValueAsInt("count"));
        DevModeUsageStatistics.collectEvent("count");
        projectData = new StatisticsContainer(storage.readProject());
        Assert.assertEquals("Increment does not match", 2,
                projectData.getValueAsInt("count"));
        DevModeUsageStatistics.collectEvent("count");
        projectData = new StatisticsContainer(storage.readProject());
        Assert.assertEquals("Increment does not match", 3,
                projectData.getValueAsInt("count"));

    }

    @Test
    public void multipleProjects() throws Exception {
        // Init using test project
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);
        StatisticsContainer projectData = new StatisticsContainer(
                storage.readProject());
        // Data contains 5 previous starts for this project
        Assert.assertEquals("Expected to have 6 restarts", 6,
                projectData.getValueAsInt("devModeStarts"));

        // Switch project to track
        File mavenProjectFolder2 = TestUtils
                .getTestFolder("stats-data/maven-project-folder2");
        DevModeUsageStatistics.init(mavenProjectFolder2, storage, sender);
        projectData = new StatisticsContainer(storage.readProject());
        Assert.assertEquals("Expected to have one restarts", 1,
                projectData.getValueAsInt("devModeStarts"));

        // Switch project to track
        File gradleProjectFolder1 = TestUtils
                .getTestFolder("stats-data/gradle-project-folder1");
        DevModeUsageStatistics.init(gradleProjectFolder1, storage, sender);
        projectData = new StatisticsContainer(storage.readProject());
        Assert.assertEquals("Expected to have one restarts", 1,
                projectData.getValueAsInt("devModeStarts"));

        // Switch project to track
        File gradleProjectFolder2 = TestUtils
                .getTestFolder("stats-data/gradle-project-folder2");

        // Double init to check restart count
        DevModeUsageStatistics.init(gradleProjectFolder2, storage, sender);
        DevModeUsageStatistics.init(gradleProjectFolder2, storage, sender);
        projectData = new StatisticsContainer(storage.readProject());
        Assert.assertEquals("Expected to have 2 restarts", 2,
                projectData.getValueAsInt("devModeStarts"));

        // Check that all project are stored correctly
        ObjectNode allData = storage.read();
        Assert.assertEquals("Expected to have 4 projects", 4,
                getNumberOfProjects(allData));

    }

    @Test
    public void mavenProjectProjectId() {
        File mavenProjectFolder1 = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        File mavenProjectFolder2 = TestUtils
                .getTestFolder("stats-data/maven-project-folder2");
        String id1 = ProjectHelpers.generateProjectId(mavenProjectFolder1);
        String id2 = ProjectHelpers.generateProjectId(mavenProjectFolder2);
        Assert.assertNotNull(id1);
        Assert.assertNotNull(id2);
        Assert.assertNotEquals(id1, id2); // Should differ
    }

    @Test
    public void mavenProjectSource() {
        File mavenProjectFolder1 = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        File mavenProjectFolder2 = TestUtils
                .getTestFolder("stats-data/maven-project-folder2");
        String source1 = ProjectHelpers.getProjectSource(mavenProjectFolder1);
        String source2 = ProjectHelpers.getProjectSource(mavenProjectFolder2);
        Assert.assertEquals("https://start.vaadin.com/test/1", source1);
        Assert.assertEquals("https://start.vaadin.com/test/2", source2);
    }

    @Test
    public void gradleProjectProjectId() {
        File gradleProjectFolder1 = TestUtils
                .getTestFolder("stats-data/gradle-project-folder1");
        File gradleProjectFolder2 = TestUtils
                .getTestFolder("stats-data/gradle-project-folder2");
        String id1 = ProjectHelpers.generateProjectId(gradleProjectFolder1);
        String id2 = ProjectHelpers.generateProjectId(gradleProjectFolder2);
        Assert.assertNotNull(id1);
        Assert.assertNotNull(id2);
        Assert.assertNotEquals(id1, id2); // Should differ
    }

    @Test
    public void gradleProjectSource() {
        File gradleProjectFolder1 = TestUtils
                .getTestFolder("stats-data/gradle-project-folder1");
        File gradleProjectFolder2 = TestUtils
                .getTestFolder("stats-data/gradle-project-folder2");
        String source1 = ProjectHelpers.getProjectSource(gradleProjectFolder1);
        String source2 = ProjectHelpers.getProjectSource(gradleProjectFolder2);
        Assert.assertEquals("https://start.vaadin.com/test/3", source1);
        Assert.assertEquals("https://start.vaadin.com/test/4", source2);
    }

    @Test
    public void missingProject() {
        File mavenProjectFolder1 = TestUtils.getTestFolder("java");
        File mavenProjectFolder2 = TestUtils.getTestFolder("stats-data/empty");
        String id1 = ProjectHelpers.generateProjectId(mavenProjectFolder1);
        String id2 = ProjectHelpers.generateProjectId(mavenProjectFolder2);
        Assert.assertEquals(DEFAULT_PROJECT_ID, id1);
        Assert.assertEquals(DEFAULT_PROJECT_ID, id2); // Should be the
                                                      // default
                                                      // id in both
                                                      // cases
    }

    @Test
    public void machineId() throws Exception {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        final ObjectNode project = storage.read();
        Assert.assertEquals(MachineId.get(),
                project.get(StatisticsConstants.FIELD_MACHINE_ID).asText());
    }

    @Test
    public void handleServerData_createEntriesForServerSideStatistic() {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        long before = System.currentTimeMillis();
        UsageStatistics.markAsUsed("test-has-flow-feature", "99.99.99");
        DevModeUsageStatistics.handleServerData();
        long after = System.currentTimeMillis();

        final ObjectNode project = storage.readProject();
        JsonNode featureNode = assertThatProjectHasFeature(project,
                "test-has-flow-feature", "99.99.99");
        Assert.assertNotNull("firstUsed", featureNode.get("firstUsed"));
        long firstUsed = featureNode.get("firstUsed").longValue();
        Assert.assertTrue("firstUsed ",
                firstUsed >= before && firstUsed <= after);
        Assert.assertNull("lastUsed", featureNode.get("lastUsed"));
    }

    @Test
    public void handleServerData_updatesServerSideStatistic() {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);
        ObjectNode existingFeatureNode = JsonHelpers.getJsonMapper()
                .createObjectNode();
        existingFeatureNode.put("version", "99.99.99");
        long firstUsed = System.currentTimeMillis() - 10000;
        existingFeatureNode.put("firstUsed", firstUsed);

        storage.update((global, project) -> {
            ObjectNode elements = JsonHelpers.getJsonMapper()
                    .createObjectNode();
            elements.set("test-has-flow-feature", existingFeatureNode);
            project.setValue("elements", elements);
        });

        long before = System.currentTimeMillis();
        UsageStatistics.markAsUsed("test-has-flow-feature", "99.99.99");
        DevModeUsageStatistics.handleServerData();
        long after = System.currentTimeMillis();

        final ObjectNode project = storage.readProject();
        JsonNode featureNode = assertThatProjectHasFeature(project,
                "test-has-flow-feature", "99.99.99");
        Assert.assertEquals("firstUsed", firstUsed,
                featureNode.get("firstUsed").longValue());
        long lastUsed = featureNode.get("lastUsed").longValue();
        Assert.assertTrue("lastUsed ", lastUsed >= before && lastUsed <= after);
    }

    @Test
    public void handleServerData_preserveExistingFields() {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        storage.update((global, project) -> {
            ObjectNode elements = JsonHelpers.getJsonMapper()
                    .createObjectNode();
            elements.set("test-existing-feature",
                    JsonHelpers.getJsonMapper().createObjectNode());
            project.setValue("elements", elements);
        });

        UsageStatistics.markAsUsed("test-has-flow-feature", "99.99.99");
        DevModeUsageStatistics.handleServerData();

        final ObjectNode project = storage.readProject();
        JsonNode elements = project.get("elements");
        Assert.assertNotNull("elements key missing", elements);
        Assert.assertTrue("existing feature missing",
                elements.has("test-existing-feature"));
        Assert.assertTrue("existing feature missing",
                elements.has("test-has-flow-feature"));
    }

    @Test
    public void handleBrowserData_createEntriesForBrowserStatistic() {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        JsonObject browserData = createBrowserData();
        DevModeUsageStatistics.handleBrowserData(browserData);

        final ObjectNode project = storage.readProject();
        assertThatProjectHasFeature(project, "@vaadin/router", "1.7.4");
        assertThatProjectHasFeature(project, "@vaadin/common-frontend",
                "0.0.18");
        assertThatProjectHasFeature(project, "vaadin-iconset", "99.99.99");
        Assert.assertEquals("Unexpected number of elements entries", 3,
                project.get("elements").size());

        assertThatProjectHasFramework(project, "React", "unknown");
        assertThatProjectHasFramework(project, "Flow", "99.99.99");
        Assert.assertEquals("Unexpected number of frameworks entries", 2,
                project.get("frameworks").size());

        assertThatProjectHasTheme(project, "Lumo", "99.99.99");
        Assert.assertEquals("Unexpected number of themes entries", 1,
                project.get("themes").size());
    }

    @Test
    public void handleBrowserData_overwritesBrowseStatistic()
            throws JsonProcessingException {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        JsonObject browserData = createBrowserData();

        ObjectNode existingFeatureNode = JsonHelpers.getJsonMapper().readValue(
                browserData.getObject("browserData").getObject("elements")
                        .getObject("@vaadin/router").toJson(),
                ObjectNode.class);
        long firstUsedFromBrowserData = existingFeatureNode.get("firstUsed")
                .longValue();
        long firstUsed = firstUsedFromBrowserData - 10000;
        existingFeatureNode.put("version", "1.0.0");
        existingFeatureNode.put("firstUsed", firstUsed);

        storage.update((global, project) -> {
            ObjectNode elements = JsonHelpers.getJsonMapper()
                    .createObjectNode();
            elements.set("@vaadin/router", existingFeatureNode);
            project.setValue("elements", elements);
        });
        ObjectNode project = storage.readProject();
        assertThatProjectHasFeature(project, "@vaadin/router",
                existingFeatureNode.get("version").asText());

        DevModeUsageStatistics.handleBrowserData(browserData);

        project = storage.readProject();
        JsonNode featureNode = assertThatProjectHasFeature(project,
                "@vaadin/router", "1.7.4");
        Assert.assertEquals("firstUsed", firstUsedFromBrowserData,
                featureNode.get("firstUsed").longValue());
    }

    @Test
    public void handleBrowserData_preserveExistingFields() {
        File mavenProjectFolder = TestUtils
                .getTestFolder("stats-data/maven-project-folder1");
        DevModeUsageStatistics.init(mavenProjectFolder, storage, sender);

        ObjectNode featureNode = JsonHelpers.getJsonMapper().createObjectNode();
        featureNode.put("version", "99.99.99");
        featureNode.put("firstUsed", System.currentTimeMillis() - 90000);
        featureNode.put("lastUsed", System.currentTimeMillis() - 10000);
        storage.update((global, project) -> {
            ObjectNode elements = JsonHelpers.getJsonMapper()
                    .createObjectNode();
            elements.set("test-existing-feature", featureNode);
            project.setValue("elements", elements);
        });

        JsonObject browserData = createBrowserData();
        DevModeUsageStatistics.handleBrowserData(browserData);

        final ObjectNode project = storage.readProject();
        assertThatProjectHasFeature(project, "test-existing-feature",
                "99.99.99");
        assertThatProjectHasFeature(project, "@vaadin/router", "1.7.4");
    }

    private static JsonObject wrapStats(String data) {
        JsonObject wrapped = Json.createObject();
        wrapped.put("browserData", data);
        return wrapped;
    }

    private static int getNumberOfProjects(ObjectNode allData) {
        if (allData.has(StatisticsConstants.FIELD_PROJECTS)) {
            return allData.get(StatisticsConstants.FIELD_PROJECTS).size();
        }
        return 0;
    }

    private JsonNode assertProjectEntry(ObjectNode project, String container,
            String name, String version) {
        Assert.assertTrue(container + " key missing", project.has(container));
        JsonNode featureNode = project.get(container).get(name);
        Assert.assertNotNull("entry " + name + " not found in " + container,
                featureNode);
        Assert.assertEquals("version", version,
                featureNode.get("version").asText());
        Assert.assertNotNull("firstUsed", featureNode.get("firstUsed"));
        return featureNode;
    }

    private JsonNode assertThatProjectHasFramework(ObjectNode project,
            String name, String version) {
        return assertProjectEntry(project, "frameworks", name, version);
    }

    private JsonNode assertThatProjectHasTheme(ObjectNode project, String name,
            String version) {
        return assertProjectEntry(project, "themes", name, version);
    }

    private JsonNode assertThatProjectHasFeature(ObjectNode project,
            String name, String version) {
        return assertProjectEntry(project, "elements", name, version);
    }

    private static JsonObject createBrowserData() {
        return Json.parse("""
                {
                    "browserData": {
                        "elements": {
                            "@vaadin/router": {
                                "firstUsed": 1655485266038,
                                "version": "1.7.4",
                                "lastUsed": 1717309299243
                            },
                            "@vaadin/common-frontend": {
                                "firstUsed": 1655485266038,
                                "version": "0.0.18",
                                "lastUsed": 1725870127515
                            },
                            "vaadin-iconset": {
                                "firstUsed": 1655485266038,
                                "version": "99.99.99",
                                "lastUsed": 1725870127515
                            }
                        },
                        "frameworks": {
                            "React": {
                                "firstUsed": 1655485266038,
                                "version": "unknown",
                                "lastUsed": 1703682895162
                            },
                            "Flow": {
                                "firstUsed": 1655485266038,
                                "version": "99.99.99",
                                "lastUsed": 1725870127515
                            }
                        },
                        "themes": {
                            "Lumo": {
                                "firstUsed": 1655485266038,
                                "version": "99.99.99",
                                "lastUsed": 1725870127515
                            }
                        }
                    }
                }""");
    }

}

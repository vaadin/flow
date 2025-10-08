/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.testutil.TestUtils;
import com.vaadin.pro.licensechecker.MachineId;

@NotThreadSafe
public class DevModeUsageStatisticsTest extends AbstractStatisticsTest {

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

    private static JsonNode wrapStats(String data) {
        ObjectNode wrapped = JacksonUtils.createObjectNode();
        wrapped.put("browserData", data);
        return wrapped;
    }

    private static int getNumberOfProjects(ObjectNode allData) {
        if (allData.has(StatisticsConstants.FIELD_PROJECTS)) {
            return allData.get(StatisticsConstants.FIELD_PROJECTS).size();
        }
        return 0;
    }

}

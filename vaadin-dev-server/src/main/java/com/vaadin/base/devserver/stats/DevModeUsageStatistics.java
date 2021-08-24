package com.vaadin.base.devserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Singleton for collecting development time usage metrics
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 */
public class VaadinUsageStatistics {


    /*
     * Client-side telemetry parameter.
     */
    public static final String TELEMETRY_PARAMETER = "vaadin_telemetry_data";


    /*
     * Name of the JSON file containing all the statistics.
     */
    private static final String STATISTICS_FILE_NAME = "usage-statistics.json";


    /*
     *  Reporting remote URL.
     */
    private static final String USAGE_REPORT_URL = "https://tools.vaadin.com/usage-stats/v2/submit";

    // Default data values and limits
    private static final String MISSING_DATA = "[NA]";
    private static final String DEFAULT_PROJECT_ID = "default-project-id";
    private static final String GENERATED_USERNAME = "GENERATED";
    private static final long TIME_SEC_12H = 43200L;
    private static final long TIME_SEC_24H = 86400L;
    private static final long TIME_SEC_30D = 2592000L;
    private static final int MAX_TELEMETRY_LENGTH = 1024*100; // 100k
    private static final String INVALID_SERVER_RESPONSE = "Invalid server response.";

    // External parameters and file names
    private static final String PARAMETER_PROJECT_SOURCE_ID = "project.source.id";
    private static final String PROPERTY_USER_HOME = "user.home";
    private static final String VAADIN_FOLDER_NAME = ".vaadin";
    private static final String PRO_KEY_FILE_NAME = "proKey";
    private static final String USER_KEY_FILE_NAME = "userKey";

    // Meta fields for reporting and scheduling
    private static final String FIELD_LAST_SENT = "lastSent";
    private static final String FIELD_LAST_STATUS = "lastSendStatus";
    private static final String FIELD_SEND_INTERVAL = "reportInterval";
    private static final String FIELD_SERVER_MESSAGE = "serverMessage";

    // Data fields
    private static final String FIELD_PROJECT_ID = "id";
    private static final String FIELD_PROJECT_DEVMODE_STARTS = "devModeStarts";
    private static final String FIELD_PROJECT_DEVMODE_RELOADS = "devModeReloads";
    private static final String FIELD_OPEARATING_SYSTEM = "os";
    private static final String FIELD_JVM = "jvm";
    private static final String FIELD_FLOW_VERSION = "flowVersion";
    private static final String FIELD_SOURCE_ID = "sourceId";
    private static final String FIELD_PROKEY = "proKey";
    private static final String FIELD_USER_KEY = "userKey";
    private static final String FIELD_PROJECTS = "projects";
    private static final String VAADIN_PROJECT_SOURCE_TEXT = "Vaadin project from";

    private String projectId;
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private ObjectNode json;
    private ObjectNode projectJson;
    private boolean usageStatisticsEnabled;
    private String reportingUrl;
    private File usageStatisticsFile;

    /** Singleton pattern */
    private static final AtomicReference<VaadinUsageStatistics> instance = new AtomicReference<>();

    /**
     * Get the instantiated VaadinUsageStatistics.
     *
     * @return devModeHandler or {@code null} if {@link #init(ApplicationConfiguration, String)} has not been called.
     */
    public static VaadinUsageStatistics get() {
        return instance.get();
    }

    private VaadinUsageStatistics() {
        // Only static methods here, no need to create an instance
    }

    /**
     * Initialize statistics module. This should be done on devmode startup.
     * First check if statistics collection is enabled.
     *
     *
     * @param config Application configuration parameters.
     * @param projectFolder Folder of the working project.
     * @return VaadinUsageStatistics instance in case everything is ok, null otherwise
     */
    public static VaadinUsageStatistics init(ApplicationConfiguration config, String projectFolder) {

        if (instance.get() == null) {
            VaadinUsageStatistics newStats = new VaadinUsageStatistics();
            instance.compareAndSet(null, newStats);
        }

        final VaadinUsageStatistics stats = get();
        stats.setStatisticsEnabled(config != null
                && !config.isProductionMode()
                && config.isUsageStatisticsEnabled());
        if (stats.isStatisticsEnabled()) {
            getLogger().debug("VaadinUsageStatistics enabled");
        } else {
            getLogger().debug("VaadinUsageStatistics disabled");
            return stats; // Do not go any further
        }

        stats.projectId = stats.generateProjectId(projectFolder);

        // Read the current statistics data
        stats.json = stats.readJson();

        // Update the machine / user / source level data
        stats.json.put(FIELD_OPEARATING_SYSTEM, stats.getOperatingSystem());
        stats.json.put(FIELD_JVM, stats.getJVMVersion());
        stats.json.put(FIELD_PROKEY, stats.getProKey());
        stats.json.put(FIELD_USER_KEY, stats.getUserKey());

        // Find the project we are working on
        if (!stats.json.has(FIELD_PROJECTS)) {
            stats.json.set(FIELD_PROJECTS, jsonMapper.createArrayNode());
        }
        stats.projectJson = findById(stats.projectId, stats.json.get(FIELD_PROJECTS), true);

        // Update basic project statistics and save
        stats.projectJson.put(FIELD_FLOW_VERSION, Version.getFullVersion());
        stats.projectJson.put(FIELD_SOURCE_ID, getProjectSource(projectFolder));
        incrementJsonValue(stats.projectJson, FIELD_PROJECT_DEVMODE_STARTS);
        stats.writeJson(stats.json);

        // Send usage statistics asynchronously, if enough time has passed
        if (stats.isIntervalElapsed()) {
            CompletableFuture.runAsync(()-> stats.sendCurrentStatistics());
        }

        return stats;
    }

    /**
     *  Get the pseudonymized project id.
     *
     * @return
     */
    public String getProjectId() {
        return projectId;
    }


    /**
     *  Get the remote reporting URL.
     **
     * @return Returns {@link #USAGE_REPORT_URL} by default.
     */
    public String getUsageReportingUrl() {
        return reportingUrl == null? USAGE_REPORT_URL : reportingUrl;
    }

    /**
     *  Get the remote reporting URL.
     *
     * @return By default return {@link #USAGE_REPORT_URL}.
     */
    void setUsageReportingUrl(String reportingUrl) {
        this.reportingUrl = reportingUrl;
    }

    /**
     *  Check if statistics are enabled for this project.
     *
     * @return true if statistics collection is enabled.
     */
    public boolean isStatisticsEnabled() {
        return this.usageStatisticsEnabled;
    }

    /** Enable or disable statistics collection and sending.
     *
     * @param enabled true if statistics should be collected, false otherwise.
     */
    public void setStatisticsEnabled(boolean enabled) {
        this.usageStatisticsEnabled = enabled;
    }

    /**
     * Get operating system identifier from system.
     *
     * @return os.name system property or MISSING_DATA
     */
    public String getOperatingSystem() {
        String os = System.getProperty("os.name");
        return os == null  ? MISSING_DATA: os;
    }

    /**
     * Get operating JVM version identifier from system.
     *
     * @return os.name system property or MISSING_DATA
     */
    public String getJVMVersion() {
        String os = System.getProperty("java.vm.name");
        os = (os == null ? MISSING_DATA : os);
        String ver = System.getProperty("java.specification.version");
        ver = (ver == null ? MISSING_DATA : ver);
        return os == null && ver == null? MISSING_DATA: os+ " / " +ver;
    }

    /** Handles a client-side request to receive component telemetry data.
     *
     * @return <code>true</code> if request was handled, <code>false</code> otherwise.
     */
    public boolean handleClientTelemetryData(HttpServletRequest request, HttpServletResponse response) {
        if (!usageStatisticsEnabled) {
            return false;
        }

        if (request.getParameter(TELEMETRY_PARAMETER) != null && request.getMethod().equals("POST")) {
            getLogger().debug("Received telemetry POST from browser");
            try {
                if (request.getContentType() == null || !request.getContentType().equals("application/json")) {
                    // Content type should be correct
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return true;
                }
                if (request.getContentLength() > MAX_TELEMETRY_LENGTH) {
                    // Do not store meaningless amount of telemetry data
                    ObjectNode telemetryData = jsonMapper.createObjectNode();
                    telemetryData.put("elements", "Too much telemetry data: "+request.getContentLength());
                    updateProjectTelemetryData(telemetryData);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return true;
                } else {
                    // Backward compatible parsing: The request contains an explanation,
                    // and the json starts with the first "{"
                    String data = IOUtils.toString(request.getReader());
                    if (!data.contains("{")) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return true;
                    }
                    String json = data.substring(data.indexOf("{"));

                    JsonNode telemetryData = jsonMapper.readTree(json);
                    updateProjectTelemetryData(telemetryData);
                }

            } catch (Exception e) {
                getLogger().debug("Failed to handle telemetry request", e);
            } finally {
                try {
                    response.getWriter().write("Thank you");
                } catch (IOException e) {
                    getLogger().debug("Failed to write telemetry response", e);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Update a single increment number value in current project data.
     *
     *  Stores the data to the disk automatically.
     *
     * @see #incrementJsonValue(ObjectNode, String)
     * @param fieldName name of the field to increment
     */
    public void incrementFieldValue(String fieldName) {
        incrementJsonValue(projectJson,fieldName);
        writeJson(json);
    }

    /**
     * Get a value of number value in current project data.
     *
     * @see #incrementFieldValue(String)
     * @param fieldName name of the field to get
     * @return Value if this is integer field, -1 if missing
     */
    public int getFieldValue(String fieldName) {
        if (projectJson != null && projectJson.has(fieldName) && projectJson.get(fieldName).isInt()) {
            return projectJson.get(fieldName).asInt();
        }

        return -1;
    }

    /**
     * Check the Interval has elapsed.
     *
     * Uses <code>System.currentTimeMillis</code> as time source.
     *
     * @see #getLastSendTime()
     * @see #getInterval()
     * @return true if enough time has passed since the last send attempt.
     */
    public boolean isIntervalElapsed() {
        long now = System.currentTimeMillis();
        long lastSend = getLastSendTime();
        long interval = getInterval();
        return lastSend+interval*1000 < now;
    }

    /**
     * Reads the statistics update interval.
     *
     * @see #FIELD_SEND_INTERVAL
     * @return Time interval in seconds. {@link #TIME_SEC_24H} in minumun and {@link #TIME_SEC_30D} as maximum.
     */
    public long getInterval() {
        try {
            long interval = json.get(FIELD_SEND_INTERVAL).asLong();
            return normalizeInterval(interval);
        } catch (Exception e) {
            // Just return the default value
        }
        return TIME_SEC_24H;
    }

    /**
     *  Gets the last time the data was collected according to the statistics file.
     *
     * @see #FIELD_LAST_SENT
     * @return Unix timestamp or -1 if not present
     */
    public long getLastSendTime() {
        try {
            return json.get(FIELD_LAST_SENT).asLong();
        } catch (Exception e) {
            // Use default value in case of any problems
        }
        return -1; //
    }

    /**
     *  Gets the last time the data was collected according to the statistics file.
     *
     * @see #FIELD_LAST_STATUS
     * @return Unix timestamp or -1 if not present
     */
    public String getLastSendStatus() {
        try {
            return json.get(FIELD_LAST_STATUS).asText();
        } catch (Exception e) {
            // Use default value in case of any problems
        }
        return null; //
    }

    /**
     *  Helper to update client data in current project.
     *
     * @param clientData Json data received from client.
     */
    private synchronized void updateProjectTelemetryData(JsonNode clientData) {
        try {
            if (clientData != null && clientData.isObject()) {
                clientData.fields().forEachRemaining(e -> projectJson.set(e.getKey(), e.getValue()));
            }
        } catch (Exception e) {
            getLogger().debug("Failed to update client telemetry data", e);
        }
        writeJson(json);
    }

    /**
     * DOM helper to find the text content of the first direct child node by given name.
     *
     * @param parent
     * @param nodeName
     * @return Text content of the first mach or null if not found.
     */
    private static String getFirstElementTextByName(Element parent, String nodeName) {
        NodeList nodeList = parent.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeName().equals(nodeName))  {
                return nodeList.item(i).getTextContent();
            }
        }
        return null;
    }

    /**
     *  Helper to find a project node by id in the given array node.
     *
     *
     * @see #FIELD_PROJECT_ID
     * @param pid Project ID
     * @param projects Json array node containing list of projects
     * @param createNew true if a new {@link ObjectNode} should be created if not found.
     * @return Json {@link ObjectNode} if found or null. Always returns a node if <code>createNew</code> is <code>true</code> and <code>projects</code> is not null.
     */
    private static ObjectNode findById(String pid, JsonNode projects, boolean createNew) {
        if (projects== null || !projects.isArray()) {
            return null;
        }

        for (final JsonNode p : projects) {
            if (p!= null && p.has(FIELD_PROJECT_ID) && pid.equals(p.get(FIELD_PROJECT_ID).asText())) {
                return (ObjectNode)p;
            }
        }

        if (createNew) {
            ArrayNode arrayNode = (ArrayNode) projects;
            ObjectNode p = arrayNode.addObject();
            p.put(FIELD_PROJECT_ID, pid);
            return p;
        }

        return null;
    }


    /**
     *  Send current statistics to given reporting URL.
     *
     * Reads the current data and posts it to given URL. Updates or replaces
     * the local data according to the response.
     *
     * Updates <code>FIELD_LAST_SENT</code> and <code>FIELD_LAST_STATUS</code>.
     *
     * @see #postData(String, JsonNode)
     */
    void sendCurrentStatistics() {

        // Post copy of the current data
        String message = null;
        JsonNode response = postData(getUsageReportingUrl(),json.deepCopy());

        // Update the last sent time
        // If the last send was successful we clear the project data
        if (response != null && response.isObject() && response.has(FIELD_LAST_STATUS)) {
            json.put(FIELD_LAST_SENT,System.currentTimeMillis());
            json.put(FIELD_LAST_STATUS,response.get(FIELD_LAST_STATUS).asText());

            // Use different interval, if requested in response or default to 24H
            if (response.has(FIELD_SEND_INTERVAL)
                    && response.get(FIELD_SEND_INTERVAL).isNumber()) {
                json.put(FIELD_SEND_INTERVAL, normalizeInterval(response.get(FIELD_SEND_INTERVAL).asLong()));
            } else {
                json.put(FIELD_SEND_INTERVAL, TIME_SEC_24H);
            }

            // Update the server message
            if (response.has(FIELD_SERVER_MESSAGE)
                    && response.get(FIELD_SERVER_MESSAGE).isTextual()) {
                message = response.get(FIELD_SERVER_MESSAGE).asText();
                json.put(FIELD_SERVER_MESSAGE, message);
            }

            // If data was sent ok, clear the existing project data
            if (response.get(FIELD_LAST_STATUS).asText().startsWith("200:")) {
                json.set(FIELD_PROJECTS, jsonMapper.createArrayNode());
                projectJson = findById(projectId, json.get(FIELD_PROJECTS), true);
            }
        }

        writeJson(json);

        // Show message on console, if present
        if (message != null && !message.trim().isEmpty()) {
            getLogger().info(message);
        }
    }

    /**
     * Get interval that is between {@link #TIME_SEC_12H}and {@link #TIME_SEC_30D}
     *
     * @param intervalSec Interval to normalize
     * @return <code>interval</code> if inside valid range.
     */
    private static long normalizeInterval(long intervalSec) {
        if (intervalSec < TIME_SEC_12H) return TIME_SEC_12H;
        if (intervalSec > TIME_SEC_30D) return TIME_SEC_30D;
        return intervalSec;
    }

    /** Posts given Json data to a URL.
     *
     * Updates <code>FIELD_LAST_STATUS</code>.
     *
     * @param posrtUrl URL to post data to.
     * @param data Json data to send
     * @return Response or <code>data</code> if the data was not successfully sent.
     */
    private static ObjectNode postData(String posrtUrl, JsonNode data) {
        ObjectNode result;
        try {
            HttpPost post = new HttpPost(posrtUrl);
            post.addHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(jsonMapper.writeValueAsString(data)));

            HttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = client.execute(post);
            String responseStatus = response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase();
            JsonNode jsonResponse = null;
            if (response.getStatusLine().getStatusCode()== HttpStatus.SC_OK) {
                String responseString = EntityUtils.toString(response.getEntity());
                jsonResponse = jsonMapper.readTree(responseString);
            }

            if (jsonResponse != null && jsonResponse.isObject()) {
                result = (ObjectNode) jsonResponse;
            } else {
                // Default response in case of any problems
                result = jsonMapper.createObjectNode();
            }
            // Update the status and return the results
            result.put(FIELD_LAST_STATUS, responseStatus);
            return result;

        } catch (IOException e) {
            getLogger().debug("Failed to send statistics.",e);
        }

        // Fallback
        result = jsonMapper.createObjectNode();
        result.put(FIELD_LAST_STATUS, INVALID_SERVER_RESPONSE);
        return result;
    }


    /**
     *  Writes the given json data to local project statistics file.
     *
     * @see #getUsageStatisticsStore()
     * @param data Json data to be written.
     */
    private synchronized void writeJson(JsonNode data) {
        try {
            jsonMapper.writeValue(getUsageStatisticsStore(), data);
        } catch (IOException e) {
            getLogger().debug("Failed to write json", e);
        }
    }

    /**
     *  Read the data from local project statistics file.
     *
     * @see #getUsageStatisticsStore()
     * @return  Json data in the file or empty Json node.
     */
    private synchronized ObjectNode readJson() {
        try {
            File file = getUsageStatisticsStore();
            if (file.exists()) {
                return  (ObjectNode)jsonMapper.readTree(file);
            }
        } catch (JsonProcessingException e) {
            getLogger().debug("Failed to parse json", e);
        } catch (IOException e) {
            getLogger().debug("Failed to read json", e);
        }
        // Return empty node if nothing else is found
        return jsonMapper.createObjectNode();
    }

    /**
     *
     * Get usage statistics json file location.
     *
     * @return the location of statistics storage file.
     */
    private File getUsageStatisticsStore() {

        if (this.usageStatisticsFile == null) {
            File vaadinHome = null;
            try {
                vaadinHome = getVaadinHomeDirectory();
            } catch (Exception e) {
                getLogger().debug("Failed to find .vaadin directory ", e);
                vaadinHome = null;
            }

            if (vaadinHome == null) {
                try {
                    // Create a temp folder for data
                    vaadinHome = File.createTempFile(VAADIN_FOLDER_NAME,
                            UUID.randomUUID().toString());
                    FileUtils.forceMkdir(vaadinHome);
                } catch (IOException e) {
                    getLogger().debug("Failed to create temp directory ", e);
                    return null;
                }
            }
            this.usageStatisticsFile = new File(vaadinHome, STATISTICS_FILE_NAME);
        }
        return this.usageStatisticsFile;
    }

    /**
     *
     * Get usage statistics json file location.
     *
     * @return the location of statistics storage file.
     */
     void setUsageStatisticsStore(File usageStatistics) {
        this.usageStatisticsFile = usageStatistics;
     }
    /**
     * Get Vaadin Pro key if available in the system, or generated id.
     *
     * @return Vaadin Pro Key or null
     */
    String getProKey() {
        // Use the local proKey if present
        ProKey proKey = ProKey.get();
        return proKey.getProKey();
    }

    /**
     * Get generated user id.
     *
     * @return Generated user id, or null if unable to load or generate one.
     */
    String getUserKey() {
        File userKeyFile = getUserKeyLocation();
        if (userKeyFile != null && userKeyFile.exists()) {
            try {
                ProKey localKey = ProKey.fromFile(userKeyFile);
                if (localKey != null && localKey.getProKey() != null) {
                    return localKey.getProKey();
                }
            } catch (IOException e) {
                getLogger().debug("Failed to load userKey", e);
            }
        }

        try {
            // Generate a new one if missing and store it
            ProKey localKey = new ProKey(GENERATED_USERNAME, "user-"+UUID.randomUUID());
            localKey.toFile(userKeyFile);
            return localKey.getProKey();
        } catch (IOException e) {
            getLogger().debug("Failed to write generated userKey", e);
        }
        return null;
    }

    /**
     *  Get location for user key file.
     *
     * @return File containing the generated user id.
     */
    private static File getUserKeyLocation() {
        File vaadinHome = getVaadinHomeDirectory();
        return new File(vaadinHome, USER_KEY_FILE_NAME);
    }

    /**
     * Helper to update a single autoincrement value in current project data.
     *
     * @param node Json node which contains the field
     * @param fieldName name of the field to increment
     */
    static void incrementJsonValue(ObjectNode node, String fieldName) {
        if (node.has(fieldName)) {
            JsonNode f = node.get(fieldName);
            node.put(fieldName,f.asInt()+1);
        } else {
            node.put(fieldName,0);
        }
    }

    /** Generates a unique pseudonymisated hash string for the project in folder.
     * Uses either pom.xml or settings.gradle.
     *
     * @param projectFolder Project root folder. Should contain either pom.xml or settings.gradle.
     * @return Pseudonymised hash id of project or <code>DEFAULT_PROJECT_ID</code> if no valid project was found in the folder.
     */
    static String generateProjectId(String projectFolder) {
        Path projectPath = Paths.get(projectFolder);
        File pomFile = projectPath.resolve("pom.xml").toFile();

        // Maven project
        if (pomFile.exists()) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document pom = db.parse(pomFile);
                String groupId = getFirstElementTextByName(pom.getDocumentElement(),"groupId");
                String artifactId = getFirstElementTextByName(pom.getDocumentElement(),"artifactId");
                return "pom"+createHash(groupId+artifactId);
            } catch (SAXException | IOException | ParserConfigurationException e) {
                getLogger().debug("Failed to parse project id from "+pomFile.getPath(), e);
            }
        }

        // Gradle project
        Path gradleFile = projectPath.resolve("settings.gradle");
        if (gradleFile.toFile().exists()) {
            try (Stream<String> stream = Files.lines(gradleFile)) {
                String projectName =  stream
                        .filter(line -> line.contains("rootProject.name"))
                        .findFirst()
                        .orElse(DEFAULT_PROJECT_ID);
                if (projectName.contains("=")) {
                    projectName = projectName.substring(projectName.indexOf("=")+1)
                            .replaceAll("\'","").trim();
                }
                return "gradle"+createHash(projectName);
            } catch (IOException e) {
                getLogger().debug("Failed to parse project id from "+gradleFile.toFile().getPath(), e);
            }
        }
        return createHash(DEFAULT_PROJECT_ID);
    }

    /**
     *  Get the source URL for the project.
     *
     *  Looks for comment in either pom.xml or or settings.gradle that points back original source
     *  or repository of the project.
     *
     * @param projectFolder Project root folder. Should contain either pom.xml or settings.gradle.
     * @return URL of the project source or  <code>MISSING_DATA</code> if no valid URL was found.
     */
    static String getProjectSource(String projectFolder) {
        Path projectPath = Paths.get(projectFolder);
        File pomFile = projectPath.resolve("pom.xml").toFile();

        // Maven project
        if (pomFile.exists()) {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document pom = db.parse(pomFile);
                NodeList nodeList = pom.getDocumentElement().getChildNodes();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    if (nodeList.item(i).getNodeType() == Node.COMMENT_NODE)  {
                        String comment = nodeList.item(i).getTextContent();
                        if (comment.contains(VAADIN_PROJECT_SOURCE_TEXT)) {
                            return comment.substring(comment.indexOf(VAADIN_PROJECT_SOURCE_TEXT)
                                    + VAADIN_PROJECT_SOURCE_TEXT.length()).trim();
                        }
                    }
                }
            } catch (SAXException | IOException | ParserConfigurationException e) {
                getLogger().debug("Failed to parse project id from "+pomFile.getPath(), e);
            }
        }

        // Gradle project
        Path gradleFile = projectPath.resolve("settings.gradle");
        if (gradleFile.toFile().exists()) {
            try (Stream<String> stream = Files.lines(gradleFile)) {
                String projectName =  stream
                        .filter(line -> line.contains(VAADIN_PROJECT_SOURCE_TEXT))
                        .findFirst()
                        .orElse(null);
                if (projectName != null) {
                    return projectName.substring(projectName.indexOf(VAADIN_PROJECT_SOURCE_TEXT)
                            + VAADIN_PROJECT_SOURCE_TEXT.length()).trim();
                }
            } catch (IOException e) {
                getLogger().debug("Failed to parse project id from "+gradleFile.toFile().getPath(), e);
            }
        }
        return MISSING_DATA;
    }

    /** Creates a MD5 hash out from a string for pseudonymisation purposes.
     *
     * @param string String to hash
     * @return Hex encoded MD5 version of string or <code>MISSING_DATA</code>.
     */
    static String createHash(String string) {
        if (string != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(string.getBytes());
                byte[] digest = md.digest();
                return new String(Hex.encodeHex(digest));
            } catch (NoSuchAlgorithmException e) {
                getLogger().debug("Missing hash algorithm", e);
            }
        }
        return MISSING_DATA;
    }

    /** Get Vaadin home directory.
     *
     * @return File instance for Vaadin home folder. Does not check if the folder exists.
     */
    private static File getVaadinHomeDirectory() {
        String userHome = System.getProperty(PROPERTY_USER_HOME);
        return new File(userHome, VAADIN_FOLDER_NAME);
    }

    /** Get number of projects.
     *
     * @return Number of projects or zero.
     */
    public int getNumberOfProjects() {
        if (json != null && json.has(FIELD_PROJECTS)) {
            return json.get(FIELD_PROJECTS).size();
        }
        return 0;
    }

    /** An internal helper class representing a Vaadin Pro key.
     *
     *  This class is also used to load and save the generated User key.
     *
     */
    private static class ProKey {

        private final String username;
        private final String proKey;

        public ProKey(String username, String proKey) {
            super();
            this.username = username;
            this.proKey = proKey;
        }

        public String getUsername() {
            return username;
        }

        public String getProKey() {
            return proKey;
        }

        public static ProKey fromJson(String jsonData) {
            ProKey proKey = new ProKey(null,null);
            try {
                JsonNode json = jsonMapper.readTree(jsonData);
                proKey = new ProKey(json.get("username").asText(),
                        json.get("proKey").asText());
                return proKey;
            } catch (JsonProcessingException |NullPointerException e) {
                getLogger().debug("Failed to parse proKey from Json", e);
            }
            return proKey;
        }

        public static ProKey fromFile(File jsonFile) throws IOException {
            ProKey proKey = new ProKey(null,null);
            try {
                JsonNode json = jsonMapper.readTree(jsonFile);
                proKey = new ProKey(json.get("username").asText(),
                        json.get("proKey").asText());
                return proKey;
            } catch (JsonProcessingException |NullPointerException e) {
                getLogger().debug("Failed to parse proKey from Json", e);
            }
            return proKey;
        }

        public void toFile(File proKeyLocation) throws IOException {
            jsonMapper.writeValue(proKeyLocation,this);
        }

        public String toJson() {
            ObjectNode json = jsonMapper.createObjectNode();
            json.put("username", username);
            json.put("proKey", proKey);
            try {
                return jsonMapper.writeValueAsString(json);
            } catch (JsonProcessingException e) {
                getLogger().debug("Unable to read proKey", e);
            }
            return null;
        }

        public static ProKey get() {
            ProKey proKey = getSystemProperty();
            if (proKey != null) {
                return proKey;
            }
            proKey = getEnvironmentVariable();
            if (proKey != null) {
                return proKey;
            }
            File proKeyLocation = getFileLocation();
            try {
                proKey = read(proKeyLocation);
                return proKey;
            } catch (IOException e) {
                getLogger().debug("Unable to read proKey", e);
                return null;
            }
        }

        private static ProKey getSystemProperty() {
            String value = System.getProperty("vaadin.proKey");
            if (value == null) {
                return null;
            }
            String[] parts = value.split("/");
            if (parts.length != 2) {
                getLogger().debug(
                        "Unable to read pro key from the vaadin.proKey system property. The property must be of type -Dvaadin.proKey=[vaadin.com login email]/[prokey]");
                return null;
            }

            return new ProKey(parts[0], parts[1]);
        }

        private static ProKey getEnvironmentVariable() {
            String value = System.getenv("VAADIN_PRO_KEY");
            if (value == null) {
                return null;
            }
            String[] parts = value.split("/");
            if (parts.length != 2) {
                getLogger().debug(
                        "Unable to read pro key from the VAADIN_PRO_KEY environment variable. The value must be of type VAADIN_PRO_KEY=[vaadin.com login email]/[prokey]");
                return null;
            }

            return new ProKey(parts[0], parts[1]);
        }

        public static File getFileLocation() {
            File vaadinHome = getVaadinHomeDirectory();
            return new File(vaadinHome, PRO_KEY_FILE_NAME);
        }

        private static ProKey read(File proKeyLocation) throws IOException {
            if (!proKeyLocation.exists()) {
                return null;
            }
            return ProKey.fromFile(proKeyLocation);
        }

        public static void write(ProKey proKey, File proKeyLocation) throws IOException {
            File proKeyDirectory = proKeyLocation.getParentFile();
            if (!proKeyDirectory.exists()) {
                proKeyDirectory.mkdirs();
            }
            proKey.toFile(proKeyLocation);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinUsageStatistics.class.getName());
    }



}

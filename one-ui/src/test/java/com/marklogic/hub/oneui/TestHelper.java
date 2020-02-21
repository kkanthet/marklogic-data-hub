package com.marklogic.hub.oneui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.hub.oneui.auth.MarkLogicAuthenticationManager;
import com.marklogic.hub.oneui.models.EnvironmentInfo;
import com.marklogic.hub.oneui.models.HubConfigSession;
import com.marklogic.hub.oneui.services.EnvironmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import com.marklogic.mgmt.ManageClient;
import com.marklogic.mgmt.ManageConfig;
import com.marklogic.mgmt.api.API;
import com.marklogic.mgmt.api.security.User;
import org.junit.jupiter.api.AfterAll;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@PropertySource("classpath:application-test.properties")
public class TestHelper {
    @Autowired
    MarkLogicAuthenticationManager markLogicAuthenticationManager;

    @Value("${test.mlHost:localhost}")
    public String mlHost;

    @Value("${test.dataHubDeveloperUsername:data-hub-developer-user}")
    public String dataHubDeveloperUsername;
    @Value("${test.dataHubDeveloperPassword:data-hub-developer-user}")
    public String dataHubDeveloperPassword;

    @Value("${test.adminUserName:admin}")
    public String adminUserName;
    @Value("${test.adminPassword:admin}")
    public String adminPassword;

    private API adminAPI;

    private ManageClient client;

    private User user;

   // @Value("${test.dataHubEnvironmentManagerUsername:data-hub-environment-manager-user}")
    @Value("data-hub-environment-manager-user")
    public String dataHubEnvironmentManagerUsername;
   // @Value("${test.dataHubEnvironmentManagerPassword:data-hub-environment-manager-user}")
    @Value("data-hub-environment-manager-user")
    public String dataHubEnvironmentManagerPassword;

    public Path tempProjectDirectory = Files.createTempDirectory("one-ui-hub-project");

    @Autowired
    private HubConfigSession hubConfig;

    @Autowired
    private EnvironmentService environmentService;

    public ObjectNode validLoadDataConfig = (ObjectNode) new ObjectMapper().readTree("{ \"name\": \"validArtifact\", \"sourceFormat\": \"xml\", \"targetFormat\": \"json\"}");

    public TestHelper() throws IOException {
    }

    public void authenticateSession() {
        createUser(dataHubDeveloperUsername,dataHubDeveloperPassword,"data-hub-developer");
       EnvironmentInfo environmentInfo = new EnvironmentInfo(mlHost, "DIGEST", 8000,"DIGEST", 8002,"DIGEST", 8010, "DIGEST", 8011);
       hubConfig.setCredentials(environmentInfo, dataHubDeveloperUsername, dataHubDeveloperPassword);
    }

    public void authenticateSessionAsEnvironmentManager() {
        createUser(dataHubEnvironmentManagerUsername,dataHubEnvironmentManagerPassword,"data-hub-environment-manager");
        EnvironmentInfo environmentInfo = new EnvironmentInfo(mlHost, "DIGEST", 8000,"DIGEST", 8002,"DIGEST", 8010, "DIGEST", 8011);
        hubConfig.setCredentials(environmentInfo, dataHubEnvironmentManagerUsername, dataHubEnvironmentManagerPassword);
    }

    public void setHubProjectDirectory() {
        environmentService.setProjectDirectory(tempProjectDirectory.toAbsolutePath().toString());
        if (!hubConfig.getHubProject().isInitialized()) {
            hubConfig.createProject(environmentService.getProjectDirectory());
            hubConfig.initHubProject();
        }
    }

    private void createUser(String username, String password, String role) {
        client = new ManageClient();
        client.setManageConfig(new ManageConfig(mlHost, 8002, adminUserName, adminPassword));
        adminAPI = new API(client);

        user = new User(adminAPI, username);
        user.setUserName(username);
        user.setPassword(password);
        user.setRole(Stream.of(role).collect(Collectors.toList()));
        user.save();
    }

    @AfterAll
    private void deleteUser() {
        user.delete();
    }
}

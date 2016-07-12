package com.marklogic.quickstart.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.hub.DataHub;
import com.marklogic.hub.HubConfig;

@Component
@Scope("session")
public class EnvironmentConfig {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EnvironmentConfig.class);
    private static final String GRADLE_PROPERTIES_FILENAME = "gradle.properties";

    public String projectDir;
    public String environment;
    public boolean installed = false;
    public boolean isInitialized = false;

    public HubConfig mlSettings;

    private Properties environmentProperties = new Properties();

    public void init(String projectDir, String environment) {
        init(projectDir, environment, null);
    }

    public void init(String projectDir, String environment, LoginInfo loginInfo) {
        this.projectDir = projectDir;
        this.environment = environment;
        mlSettings = new HubConfig(this.projectDir);
        if (loginInfo != null) {
            mlSettings.adminUsername = loginInfo.username;
            mlSettings.adminPassword = loginInfo.password;
        }
        loadConfigurationFromFiles();

        DataHub dh = new DataHub(mlSettings);
        installed = dh.isInstalled();
        isInitialized = true;
    }

    public void loadConfigurationFromFiles() {
        loadConfigurationFromFile(environmentProperties, GRADLE_PROPERTIES_FILENAME);
        String envPropertiesFile = "gradle-" + environment + ".properties";
        LOGGER.info("envPropertiesFile = " + envPropertiesFile);
        loadConfigurationFromFile(environmentProperties, envPropertiesFile);
        LOGGER.info(environmentProperties.toString());

        mlSettings.name = getEnvPropString("mlAppName", mlSettings.name);

        mlSettings.host = getEnvPropString("mlHost", mlSettings.host);

        mlSettings.stagingDbName = getEnvPropString("mlStagingDbName", mlSettings.stagingDbName);
        mlSettings.stagingHttpName = getEnvPropString("mlStagingAppserverName", mlSettings.stagingHttpName);
        mlSettings.stagingForestsPerHost = getEnvPropInteger("mlStagingForestsPerHost", mlSettings.stagingForestsPerHost);
        mlSettings.stagingPort = getEnvPropInteger("mlStagingPort", mlSettings.stagingPort);

        mlSettings.finalDbName = getEnvPropString("mlFinalDbName", mlSettings.finalDbName);
        mlSettings.finalHttpName = getEnvPropString("mlFinalAppserverName", mlSettings.finalHttpName);
        mlSettings.finalForestsPerHost = getEnvPropInteger("mlFinalForestsPerHost", mlSettings.finalForestsPerHost);
        mlSettings.finalPort = getEnvPropInteger("mlFinalPort", mlSettings.finalPort);

        mlSettings.traceDbName = getEnvPropString("mlTraceDbName", mlSettings.traceDbName);
        mlSettings.traceHttpName = getEnvPropString("mlTraceAppserverName", mlSettings.traceHttpName);
        mlSettings.traceForestsPerHost = getEnvPropInteger("mlTraceForestsPerHost", mlSettings.traceForestsPerHost);
        mlSettings.tracePort = getEnvPropInteger("mlTracePort", mlSettings.tracePort);

        mlSettings.modulesDbName = getEnvPropString("mlModulesDbName", mlSettings.modulesDbName);
        mlSettings.triggersDbName = getEnvPropString("mlTriggersDbName", mlSettings.triggersDbName);
        mlSettings.schemasDbName = getEnvPropString("mlSchemasDbName", mlSettings.schemasDbName);

        mlSettings.authMethod = getEnvPropString("mlAuth", mlSettings.authMethod);
    }

    private String getEnvPropString(String key, String fallback) {
        String value = this.environmentProperties.getProperty(key);
        if (value == null) {
            value = fallback;
        }
        return value;
    }

    private int getEnvPropInteger(String key, int fallback) {
        String value = this.environmentProperties.getProperty(key);
        int res;
        if (value != null) {
            res = Integer.parseInt(value);
        }
        else {
            res = fallback;
        }
        return res;
    }

    public void loadConfigurationFromFile(Properties configProperties, String fileName) {
        InputStream is = null;
        try {
            File file = new File(this.projectDir, fileName);
            if(file.exists()) {
                is = new FileInputStream( file );
                configProperties.load( is );
                is.close();
            }
        } catch ( Exception e ) {
            is = null;
        }
    }

    @JsonIgnore
    public DatabaseClient getStagingClient() {
        Authentication authMethod = Authentication
                .valueOf(mlSettings.authMethod.toUpperCase());

        DatabaseClient client = DatabaseClientFactory.newClient(
                mlSettings.host,
                mlSettings.stagingPort,
                mlSettings.adminUsername,
                mlSettings.adminPassword, authMethod);
        return client;
    }

    @JsonIgnore
    public DatabaseClient getFinalClient() {
        Authentication authMethod = Authentication
                .valueOf(mlSettings.authMethod.toUpperCase());

        DatabaseClient client = DatabaseClientFactory.newClient(
                mlSettings.host,
                mlSettings.finalPort,
                mlSettings.adminUsername,
                mlSettings.adminPassword, authMethod);
        return client;
    }
}

package com.bonitasoft.connectors.servicenow;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

/**
 * Abstract base connector for ServiceNow.
 */
@Slf4j
public abstract class AbstractServiceNowConnector extends AbstractConnector {

    // Connection input constants
    static final String INPUT_INSTANCE_URL = "instanceUrl";
    static final String INPUT_AUTH_MODE = "authMode";
    static final String INPUT_USERNAME = "username";
    static final String INPUT_PASSWORD = "password";
    static final String INPUT_CLIENT_ID = "clientId";
    static final String INPUT_CLIENT_SECRET = "clientSecret";
    static final String INPUT_API_KEY = "apiKey";
    static final String INPUT_CONNECT_TIMEOUT = "connectTimeout";
    static final String INPUT_READ_TIMEOUT = "readTimeout";

    // Output parameter constants
    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected ServiceNowConfiguration configuration;
    protected ServiceNowClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new ServiceNowClient(this.configuration);
            log.info("ServiceNow connector connected successfully");
        } catch (ServiceNowException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (ServiceNowException e) {
            log.error("ServiceNow connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in ServiceNow connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws ServiceNowException;

    protected abstract ServiceNowConfiguration buildConfiguration();

    protected void validateConfiguration(ServiceNowConfiguration config) {
        if (config.getInstanceUrl() == null || config.getInstanceUrl().isBlank()) {
            throw new IllegalArgumentException("instanceUrl is mandatory");
        }
        String authMode = config.getAuthMode();
        if (authMode == null || authMode.isBlank()) {
            throw new IllegalArgumentException("authMode is mandatory");
        }
        switch (authMode) {
            case "BASIC":
                if (config.getUsername() == null || config.getUsername().isBlank()) {
                    throw new IllegalArgumentException("username is mandatory for BASIC auth");
                }
                if (config.getPassword() == null || config.getPassword().isBlank()) {
                    throw new IllegalArgumentException("password is mandatory for BASIC auth");
                }
                break;
            case "OAUTH2":
                if (config.getClientId() == null || config.getClientId().isBlank()) {
                    throw new IllegalArgumentException("clientId is mandatory for OAUTH2 auth");
                }
                if (config.getClientSecret() == null || config.getClientSecret().isBlank()) {
                    throw new IllegalArgumentException("clientSecret is mandatory for OAUTH2 auth");
                }
                break;
            case "API_KEY":
                if (config.getApiKey() == null || config.getApiKey().isBlank()) {
                    throw new IllegalArgumentException("apiKey is mandatory for API_KEY auth");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid authMode: " + authMode + ". Must be BASIC, OAUTH2, or API_KEY");
        }
    }

    /** Helper: read a String input, returning null if not set. */
    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    /** Helper: read a String input with a default value. */
    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /** Helper: read a Boolean input with a default value. */
    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    /** Helper: read an Integer input with a default value. */
    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * Expose output parameters for testing (package-private).
     */
    Map<String, Object> getOutputs() {
        return getOutputParameters();
    }

    /**
     * Build the common connection configuration from shared input parameters.
     * Subclasses call this and then add operation-specific parameters.
     */
    protected ServiceNowConfiguration.ServiceNowConfigurationBuilder baseConfigBuilder() {
        return ServiceNowConfiguration.builder()
                .instanceUrl(readStringInput(INPUT_INSTANCE_URL))
                .authMode(readStringInput(INPUT_AUTH_MODE, "BASIC"))
                .username(readStringInput(INPUT_USERNAME))
                .password(readStringInput(INPUT_PASSWORD))
                .clientId(readStringInput(INPUT_CLIENT_ID))
                .clientSecret(readStringInput(INPUT_CLIENT_SECRET))
                .apiKey(readStringInput(INPUT_API_KEY))
                .connectTimeout(readIntegerInput(INPUT_CONNECT_TIMEOUT, 30000))
                .readTimeout(readIntegerInput(INPUT_READ_TIMEOUT, 60000));
    }
}

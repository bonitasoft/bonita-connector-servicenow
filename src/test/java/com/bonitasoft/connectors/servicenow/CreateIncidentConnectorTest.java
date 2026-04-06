package com.bonitasoft.connectors.servicenow;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateIncidentConnectorTest {

    private CreateIncidentConnector connector;

    @Mock
    private ServiceNowClient client;

    @BeforeEach
    void setUp() {
        connector = new CreateIncidentConnector();
    }

    static Map<String, Object> connectionInputs() {
        var inputs = new HashMap<String, Object>();
        inputs.put("instanceUrl", "https://test.service-now.com");
        inputs.put("authMode", "BASIC");
        inputs.put("username", "admin");
        inputs.put("password", "password");
        return inputs;
    }

    static void setConnectionInputs(AbstractServiceNowConnector connector) {
        connector.setInputParameters(connectionInputs());
    }

    @Test
    void shouldValidateWithValidInputs() throws Exception {
        var inputs = connectionInputs();
        inputs.put("shortDescription", "Test incident");
        connector.setInputParameters(inputs);

        connector.validateInputParameters();
    }

    @Test
    void shouldRejectMissingShortDescription() {
        connector.setInputParameters(connectionInputs());

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("shortDescription");
    }

    @Test
    void shouldRejectMissingInstanceUrl() {
        var inputs = new HashMap<String, Object>();
        inputs.put("authMode", "BASIC");
        inputs.put("username", "admin");
        inputs.put("password", "pass");
        inputs.put("shortDescription", "Test");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("instanceUrl");
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        var inputs = connectionInputs();
        inputs.put("shortDescription", "Test incident");
        inputs.put("description", "A test incident");
        inputs.put("urgency", "2");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        connector.client = client;

        Map<String, Object> result = new HashMap<>();
        result.put("sys_id", "abc123");
        result.put("number", "INC0010001");
        when(client.createIncident(anyMap())).thenReturn(result);

        connector.doExecute();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("sysId")).isEqualTo("abc123");
        assertThat(outputs.get("number")).isEqualTo("INC0010001");
        assertThat(outputs.get("statusCode")).isEqualTo(201);
    }
}

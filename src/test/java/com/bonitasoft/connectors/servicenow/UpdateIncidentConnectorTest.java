package com.bonitasoft.connectors.servicenow;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.bonitasoft.connectors.servicenow.CreateIncidentConnectorTest.connectionInputs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateIncidentConnectorTest {

    private UpdateIncidentConnector connector;

    @Mock
    private ServiceNowClient client;

    @BeforeEach
    void setUp() {
        connector = new UpdateIncidentConnector();
    }

    @Test
    void shouldRejectMissingSysId() {
        connector.setInputParameters(connectionInputs());

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("sysId");
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        var inputs = connectionInputs();
        inputs.put("sysId", "abc123");
        inputs.put("shortDescription", "Updated description");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        connector.client = client;

        Map<String, Object> result = new HashMap<>();
        result.put("sys_id", "abc123");
        result.put("number", "INC0010001");
        when(client.updateIncident(eq("abc123"), anyMap())).thenReturn(result);

        connector.doExecute();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("sysId")).isEqualTo("abc123");
        assertThat(outputs.get("statusCode")).isEqualTo(200);
    }
}

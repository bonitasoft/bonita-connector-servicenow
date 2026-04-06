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
class CreateRecordConnectorTest {

    private CreateRecordConnector connector;

    @Mock
    private ServiceNowClient client;

    @BeforeEach
    void setUp() {
        connector = new CreateRecordConnector();
    }

    @Test
    void shouldRejectMissingTableName() {
        var inputs = connectionInputs();
        inputs.put("additionalFieldsJson", "{\"field\":\"value\"}");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("tableName");
    }

    @Test
    void shouldRejectMissingFieldsJson() {
        var inputs = connectionInputs();
        inputs.put("tableName", "cmdb_ci");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("additionalFieldsJson");
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        var inputs = connectionInputs();
        inputs.put("tableName", "cmdb_ci");
        inputs.put("additionalFieldsJson", "{\"name\":\"Server01\"}");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        connector.client = client;

        Map<String, Object> result = new HashMap<>();
        result.put("sys_id", "xyz789");
        when(client.createRecord(eq("cmdb_ci"), anyMap())).thenReturn(result);

        connector.doExecute();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("sysId")).isEqualTo("xyz789");
        assertThat(outputs.get("statusCode")).isEqualTo(201);
    }
}

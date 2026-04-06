package com.bonitasoft.connectors.servicenow;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static com.bonitasoft.connectors.servicenow.CreateIncidentConnectorTest.connectionInputs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryTableConnectorTest {

    private QueryTableConnector connector;

    @Mock
    private ServiceNowClient client;

    @BeforeEach
    void setUp() {
        connector = new QueryTableConnector();
    }

    @Test
    void shouldRejectMissingTableName() {
        connector.setInputParameters(connectionInputs());

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("tableName");
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        var inputs = connectionInputs();
        inputs.put("tableName", "incident");
        inputs.put("sysparmQuery", "active=true");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        connector.client = client;

        List<Map<String, Object>> records = List.of(
                Map.of("sys_id", "r1", "number", "INC001"),
                Map.of("sys_id", "r2", "number", "INC002"));
        when(client.queryTable("incident", "active=true", null, 100, 0))
                .thenReturn(new ServiceNowClient.QueryResult(records, 42));

        connector.doExecute();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("recordCount")).isEqualTo(2);
        assertThat(outputs.get("totalCount")).isEqualTo(42);
        assertThat(outputs.get("statusCode")).isEqualTo(200);
    }
}

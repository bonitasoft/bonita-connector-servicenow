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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddJournalConnectorTest {

    private AddJournalConnector connector;

    @Mock
    private ServiceNowClient client;

    @BeforeEach
    void setUp() {
        connector = new AddJournalConnector();
    }

    @Test
    void shouldRejectMissingJournalValue() {
        var inputs = connectionInputs();
        inputs.put("tableName", "incident");
        inputs.put("sysId", "abc123");
        connector.setInputParameters(inputs);

        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class)
                .hasMessageContaining("journalValue");
    }

    @Test
    void shouldDefaultJournalFieldToWorkNotes() throws Exception {
        var inputs = connectionInputs();
        inputs.put("tableName", "incident");
        inputs.put("sysId", "abc123");
        inputs.put("journalValue", "Work in progress");
        connector.setInputParameters(inputs);
        connector.validateInputParameters();

        connector.client = client;

        Map<String, Object> result = new HashMap<>();
        result.put("sys_id", "abc123");
        when(client.addJournal("incident", "abc123", "work_notes", "Work in progress"))
                .thenReturn(result);

        connector.doExecute();

        var outputs = connector.getOutputs();
        assertThat(outputs.get("sysId")).isEqualTo("abc123");
        assertThat(outputs.get("statusCode")).isEqualTo(200);
    }
}

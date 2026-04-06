package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * [BETA] Adds a journal entry (work note or comment) to a ServiceNow record
 * via PATCH /api/now/table/{tableName}/{sys_id}.
 */
@Slf4j
public class AddJournalConnector extends AbstractServiceNowConnector {

    static final String INPUT_TABLE_NAME = "tableName";
    static final String INPUT_SYS_ID = "sysId";
    static final String INPUT_JOURNAL_FIELD = "journalField";
    static final String INPUT_JOURNAL_VALUE = "journalValue";

    static final String OUTPUT_SYS_ID = "sysId";
    static final String OUTPUT_RESPONSE_JSON = "responseJson";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .tableName(readStringInput(INPUT_TABLE_NAME))
                .sysId(readStringInput(INPUT_SYS_ID))
                .journalField(readStringInput(INPUT_JOURNAL_FIELD, "work_notes"))
                .journalValue(readStringInput(INPUT_JOURNAL_VALUE))
                .build();
    }

    @Override
    protected void validateConfiguration(ServiceNowConfiguration config) {
        super.validateConfiguration(config);
        if (config.getTableName() == null || config.getTableName().isBlank()) {
            throw new IllegalArgumentException("tableName is mandatory");
        }
        if (config.getSysId() == null || config.getSysId().isBlank()) {
            throw new IllegalArgumentException("sysId is mandatory");
        }
        if (config.getJournalValue() == null || config.getJournalValue().isBlank()) {
            throw new IllegalArgumentException("journalValue is mandatory");
        }
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing AddJournal connector for table={}, sysId={}, field={}",
                configuration.getTableName(), configuration.getSysId(), configuration.getJournalField());

        Map<String, Object> result = client.addJournal(
                configuration.getTableName(),
                configuration.getSysId(),
                configuration.getJournalField(),
                configuration.getJournalValue());

        setOutputParameter(OUTPUT_SYS_ID, getStringValue(result, "sys_id"));
        setOutputParameter(OUTPUT_RESPONSE_JSON, result.toString());
        setOutputParameter(OUTPUT_STATUS_CODE, 200);

        log.info("AddJournal connector executed successfully");
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}

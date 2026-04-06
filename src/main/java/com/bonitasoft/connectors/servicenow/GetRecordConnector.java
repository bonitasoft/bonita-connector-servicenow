package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * [BETA] Retrieves a single record from ServiceNow via GET /api/now/table/{tableName}/{sys_id}.
 */
@Slf4j
public class GetRecordConnector extends AbstractServiceNowConnector {

    static final String INPUT_TABLE_NAME = "tableName";
    static final String INPUT_SYS_ID = "sysId";

    static final String OUTPUT_SYS_ID = "sysId";
    static final String OUTPUT_RECORD_JSON = "recordJson";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .tableName(readStringInput(INPUT_TABLE_NAME))
                .sysId(readStringInput(INPUT_SYS_ID))
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
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing GetRecord connector for table={}, sysId={}", configuration.getTableName(), configuration.getSysId());

        Map<String, Object> result = client.getRecord(configuration.getTableName(), configuration.getSysId());

        setOutputParameter(OUTPUT_SYS_ID, getStringValue(result, "sys_id"));
        setOutputParameter(OUTPUT_RECORD_JSON, result.toString());
        setOutputParameter(OUTPUT_STATUS_CODE, 200);

        log.info("GetRecord connector executed successfully");
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}

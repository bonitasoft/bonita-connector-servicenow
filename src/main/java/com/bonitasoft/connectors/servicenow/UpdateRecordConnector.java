package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * [BETA] Updates a generic record in ServiceNow via PATCH /api/now/table/{tableName}/{sys_id}.
 */
@Slf4j
public class UpdateRecordConnector extends AbstractServiceNowConnector {

    static final String INPUT_TABLE_NAME = "tableName";
    static final String INPUT_SYS_ID = "sysId";
    static final String INPUT_ADDITIONAL_FIELDS_JSON = "additionalFieldsJson";

    static final String OUTPUT_SYS_ID = "sysId";
    static final String OUTPUT_RESPONSE_JSON = "responseJson";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .tableName(readStringInput(INPUT_TABLE_NAME))
                .sysId(readStringInput(INPUT_SYS_ID))
                .additionalFieldsJson(readStringInput(INPUT_ADDITIONAL_FIELDS_JSON))
                .build();
    }

    @Override
    protected void validateConfiguration(ServiceNowConfiguration config) {
        super.validateConfiguration(config);
        if (config.getTableName() == null || config.getTableName().isBlank()) {
            throw new IllegalArgumentException("tableName is mandatory for updating a record");
        }
        if (config.getSysId() == null || config.getSysId().isBlank()) {
            throw new IllegalArgumentException("sysId is mandatory for updating a record");
        }
        if (config.getAdditionalFieldsJson() == null || config.getAdditionalFieldsJson().isBlank()) {
            throw new IllegalArgumentException("additionalFieldsJson is mandatory -- must contain the fields to update as JSON");
        }
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing UpdateRecord connector for table={}, sysId={}", configuration.getTableName(), configuration.getSysId());

        Map<String, Object> fields = parseFieldsJson(configuration.getAdditionalFieldsJson());
        Map<String, Object> result = client.updateRecord(configuration.getTableName(), configuration.getSysId(), fields);

        setOutputParameter(OUTPUT_SYS_ID, getStringValue(result, "sys_id"));
        setOutputParameter(OUTPUT_RESPONSE_JSON, result.toString());
        setOutputParameter(OUTPUT_STATUS_CODE, 200);

        log.info("UpdateRecord connector executed successfully");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFieldsJson(String json) throws ServiceNowException {
        try {
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ServiceNowException("Invalid additionalFieldsJson: " + e.getMessage(), e);
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}

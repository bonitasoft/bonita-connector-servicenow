package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * [BETA] Creates a generic record in ServiceNow via POST /api/now/table/{tableName}.
 */
@Slf4j
public class CreateRecordConnector extends AbstractServiceNowConnector {

    static final String INPUT_TABLE_NAME = "tableName";
    static final String INPUT_ADDITIONAL_FIELDS_JSON = "additionalFieldsJson";

    static final String OUTPUT_SYS_ID = "sysId";
    static final String OUTPUT_RESPONSE_JSON = "responseJson";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .tableName(readStringInput(INPUT_TABLE_NAME))
                .additionalFieldsJson(readStringInput(INPUT_ADDITIONAL_FIELDS_JSON))
                .build();
    }

    @Override
    protected void validateConfiguration(ServiceNowConfiguration config) {
        super.validateConfiguration(config);
        if (config.getTableName() == null || config.getTableName().isBlank()) {
            throw new IllegalArgumentException("tableName is mandatory for creating a record");
        }
        if (config.getAdditionalFieldsJson() == null || config.getAdditionalFieldsJson().isBlank()) {
            throw new IllegalArgumentException("additionalFieldsJson is mandatory -- must contain the record fields as JSON");
        }
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing CreateRecord connector for table={}", configuration.getTableName());

        Map<String, Object> fields = parseFieldsJson(configuration.getAdditionalFieldsJson());
        Map<String, Object> result = client.createRecord(configuration.getTableName(), fields);

        setOutputParameter(OUTPUT_SYS_ID, getStringValue(result, "sys_id"));
        setOutputParameter(OUTPUT_RESPONSE_JSON, result.toString());
        setOutputParameter(OUTPUT_STATUS_CODE, 201);

        log.info("CreateRecord connector executed successfully");
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

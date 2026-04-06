package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * [BETA] Updates an existing incident in ServiceNow via PATCH /api/now/table/incident/{sys_id}.
 */
@Slf4j
public class UpdateIncidentConnector extends AbstractServiceNowConnector {

    static final String INPUT_SYS_ID = "sysId";
    static final String INPUT_SHORT_DESCRIPTION = "shortDescription";
    static final String INPUT_DESCRIPTION = "description";
    static final String INPUT_URGENCY = "urgency";
    static final String INPUT_IMPACT = "impact";
    static final String INPUT_PRIORITY = "priority";
    static final String INPUT_ASSIGNED_TO = "assignedTo";
    static final String INPUT_STATE = "state";
    static final String INPUT_ADDITIONAL_FIELDS_JSON = "additionalFieldsJson";

    static final String OUTPUT_SYS_ID = "sysId";
    static final String OUTPUT_NUMBER = "number";
    static final String OUTPUT_RESPONSE_JSON = "responseJson";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .sysId(readStringInput(INPUT_SYS_ID))
                .shortDescription(readStringInput(INPUT_SHORT_DESCRIPTION))
                .description(readStringInput(INPUT_DESCRIPTION))
                .urgency(readStringInput(INPUT_URGENCY))
                .impact(readStringInput(INPUT_IMPACT))
                .priority(readStringInput(INPUT_PRIORITY))
                .assignedTo(readStringInput(INPUT_ASSIGNED_TO))
                .additionalFieldsJson(readStringInput(INPUT_ADDITIONAL_FIELDS_JSON))
                .build();
    }

    @Override
    protected void validateConfiguration(ServiceNowConfiguration config) {
        super.validateConfiguration(config);
        if (config.getSysId() == null || config.getSysId().isBlank()) {
            throw new IllegalArgumentException("sysId is mandatory for updating an incident");
        }
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing UpdateIncident connector for sysId={}", configuration.getSysId());

        Map<String, Object> fields = new HashMap<>();
        putIfNotBlank(fields, "short_description", configuration.getShortDescription());
        putIfNotBlank(fields, "description", configuration.getDescription());
        putIfNotBlank(fields, "urgency", configuration.getUrgency());
        putIfNotBlank(fields, "impact", configuration.getImpact());
        putIfNotBlank(fields, "priority", configuration.getPriority());
        putIfNotBlank(fields, "assigned_to", configuration.getAssignedTo());
        String state = readStringInput(INPUT_STATE);
        if (state != null && !state.isBlank()) {
            fields.put("state", state);
        }
        mergeAdditionalFields(fields, configuration.getAdditionalFieldsJson());

        Map<String, Object> result = client.updateIncident(configuration.getSysId(), fields);

        setOutputParameter(OUTPUT_SYS_ID, getStringValue(result, "sys_id"));
        setOutputParameter(OUTPUT_NUMBER, getStringValue(result, "number"));
        setOutputParameter(OUTPUT_RESPONSE_JSON, result.toString());
        setOutputParameter(OUTPUT_STATUS_CODE, 200);

        log.info("UpdateIncident connector executed successfully");
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeAdditionalFields(Map<String, Object> fields, String json) throws ServiceNowException {
        if (json != null && !json.isBlank()) {
            try {
                var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> additional = objectMapper.readValue(json,
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
                fields.putAll(additional);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new ServiceNowException("Invalid additionalFieldsJson: " + e.getMessage(), e);
            }
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}

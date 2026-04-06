package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * [BETA] Creates an incident in ServiceNow via POST /api/now/table/incident.
 */
@Slf4j
public class CreateIncidentConnector extends AbstractServiceNowConnector {

    static final String INPUT_SHORT_DESCRIPTION = "shortDescription";
    static final String INPUT_DESCRIPTION = "description";
    static final String INPUT_URGENCY = "urgency";
    static final String INPUT_IMPACT = "impact";
    static final String INPUT_PRIORITY = "priority";
    static final String INPUT_ASSIGNED_TO = "assignedTo";
    static final String INPUT_CATEGORY = "category";
    static final String INPUT_SUBCATEGORY = "subcategory";
    static final String INPUT_CALLER_ID = "callerId";
    static final String INPUT_ADDITIONAL_FIELDS_JSON = "additionalFieldsJson";

    static final String OUTPUT_SYS_ID = "sysId";
    static final String OUTPUT_NUMBER = "number";
    static final String OUTPUT_RESPONSE_JSON = "responseJson";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .shortDescription(readStringInput(INPUT_SHORT_DESCRIPTION))
                .description(readStringInput(INPUT_DESCRIPTION))
                .urgency(readStringInput(INPUT_URGENCY))
                .impact(readStringInput(INPUT_IMPACT))
                .priority(readStringInput(INPUT_PRIORITY))
                .assignedTo(readStringInput(INPUT_ASSIGNED_TO))
                .category(readStringInput(INPUT_CATEGORY))
                .subcategory(readStringInput(INPUT_SUBCATEGORY))
                .callerIdValue(readStringInput(INPUT_CALLER_ID))
                .additionalFieldsJson(readStringInput(INPUT_ADDITIONAL_FIELDS_JSON))
                .build();
    }

    @Override
    protected void validateConfiguration(ServiceNowConfiguration config) {
        super.validateConfiguration(config);
        if (config.getShortDescription() == null || config.getShortDescription().isBlank()) {
            throw new IllegalArgumentException("shortDescription is mandatory for creating an incident");
        }
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing CreateIncident connector");

        Map<String, Object> fields = new HashMap<>();
        fields.put("short_description", configuration.getShortDescription());
        putIfNotBlank(fields, "description", configuration.getDescription());
        putIfNotBlank(fields, "urgency", configuration.getUrgency());
        putIfNotBlank(fields, "impact", configuration.getImpact());
        putIfNotBlank(fields, "priority", configuration.getPriority());
        putIfNotBlank(fields, "assigned_to", configuration.getAssignedTo());
        putIfNotBlank(fields, "category", configuration.getCategory());
        putIfNotBlank(fields, "subcategory", configuration.getSubcategory());
        putIfNotBlank(fields, "caller_id", configuration.getCallerIdValue());
        mergeAdditionalFields(fields, configuration.getAdditionalFieldsJson());

        Map<String, Object> result = client.createIncident(fields);

        setOutputParameter(OUTPUT_SYS_ID, getStringValue(result, "sys_id"));
        setOutputParameter(OUTPUT_NUMBER, getStringValue(result, "number"));
        setOutputParameter(OUTPUT_RESPONSE_JSON, result.toString());
        setOutputParameter(OUTPUT_STATUS_CODE, 201);

        log.info("CreateIncident connector executed successfully, number={}", getStringValue(result, "number"));
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

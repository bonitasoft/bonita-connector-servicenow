package com.bonitasoft.connectors.servicenow;

import lombok.extern.slf4j.Slf4j;

/**
 * [BETA] Queries a ServiceNow table via GET /api/now/table/{tableName}?sysparm_query=...
 */
@Slf4j
public class QueryTableConnector extends AbstractServiceNowConnector {

    static final String INPUT_TABLE_NAME = "tableName";
    static final String INPUT_SYSPARM_QUERY = "sysparmQuery";
    static final String INPUT_SYSPARM_FIELDS = "sysparmFields";
    static final String INPUT_SYSPARM_LIMIT = "sysparmLimit";
    static final String INPUT_SYSPARM_OFFSET = "sysparmOffset";

    static final String OUTPUT_RECORDS_JSON = "recordsJson";
    static final String OUTPUT_RECORD_COUNT = "recordCount";
    static final String OUTPUT_TOTAL_COUNT = "totalCount";
    static final String OUTPUT_STATUS_CODE = "statusCode";

    @Override
    protected ServiceNowConfiguration buildConfiguration() {
        return baseConfigBuilder()
                .tableName(readStringInput(INPUT_TABLE_NAME))
                .sysparmQuery(readStringInput(INPUT_SYSPARM_QUERY))
                .sysparmFields(readStringInput(INPUT_SYSPARM_FIELDS))
                .sysparmLimit(readIntegerInput(INPUT_SYSPARM_LIMIT, 100))
                .sysparmOffset(readIntegerInput(INPUT_SYSPARM_OFFSET, 0))
                .build();
    }

    @Override
    protected void validateConfiguration(ServiceNowConfiguration config) {
        super.validateConfiguration(config);
        if (config.getTableName() == null || config.getTableName().isBlank()) {
            throw new IllegalArgumentException("tableName is mandatory for querying a table");
        }
    }

    @Override
    protected void doExecute() throws ServiceNowException {
        log.info("Executing QueryTable connector for table={}", configuration.getTableName());

        ServiceNowClient.QueryResult queryResult = client.queryTable(
                configuration.getTableName(),
                configuration.getSysparmQuery(),
                configuration.getSysparmFields(),
                configuration.getSysparmLimit(),
                configuration.getSysparmOffset());

        setOutputParameter(OUTPUT_RECORDS_JSON, queryResult.records().toString());
        setOutputParameter(OUTPUT_RECORD_COUNT, queryResult.records().size());
        setOutputParameter(OUTPUT_TOTAL_COUNT, queryResult.totalCount());
        setOutputParameter(OUTPUT_STATUS_CODE, 200);

        log.info("QueryTable connector executed successfully, returned {} records", queryResult.records().size());
    }
}

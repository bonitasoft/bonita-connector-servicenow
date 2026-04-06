package com.bonitasoft.connectors.servicenow;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for ServiceNow connector -- holds all connection and operation parameters.
 */
@Data
@Builder
public class ServiceNowConfiguration {

    // === Connection / Auth parameters ===
    private String instanceUrl;
    @Builder.Default
    private String authMode = "BASIC";
    private String username;
    private String password;
    private String clientId;
    private String clientSecret;
    private String apiKey;

    @Builder.Default
    private int connectTimeout = 30000;
    @Builder.Default
    private int readTimeout = 60000;
    @Builder.Default
    private int maxRetries = 3;

    // === Operation parameters ===
    private String tableName;
    private String sysId;
    private String shortDescription;
    private String description;
    private String urgency;
    private String impact;
    private String priority;
    private String assignedTo;
    private String category;
    private String subcategory;
    private String callerIdValue;
    private String additionalFieldsJson;

    // Change request specific
    private String changeType;
    private String riskLevel;
    private String startDate;
    private String endDate;

    // Query
    private String sysparmQuery;
    private String sysparmFields;
    @Builder.Default
    private int sysparmLimit = 100;
    @Builder.Default
    private int sysparmOffset = 0;

    // Journal
    private String journalField;
    private String journalValue;
}

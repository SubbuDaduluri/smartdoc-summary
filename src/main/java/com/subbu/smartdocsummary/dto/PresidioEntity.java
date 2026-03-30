package com.subbu.smartdocsummary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresidioEntity {
    @JsonProperty("entity_type")
    private String entityType;
    private int start;
    private int end;
    private double score;
    @JsonProperty("analysis_explanation")
    private String analysisExplanation;
}

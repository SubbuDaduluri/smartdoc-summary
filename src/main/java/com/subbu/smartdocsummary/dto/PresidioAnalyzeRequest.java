package com.subbu.smartdocsummary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PresidioAnalyzeRequest {
    private String text;
    @JsonProperty("entity_types")
    private List<String> entities;
    private String language;
    @JsonProperty("score_threshold")
    private Double scoreThreshold;
}

package com.subbu.smartdocsummary.dto;

import lombok.Data;

import java.util.List;

@Data
public class PresidioAnalyzeResponse {
    private List<PresidioEntity> results;
}

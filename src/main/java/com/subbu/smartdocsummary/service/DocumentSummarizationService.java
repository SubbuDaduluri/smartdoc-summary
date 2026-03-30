package com.subbu.smartdocsummary.service;

import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.dto.ProcessResponse;

public interface DocumentSummarizationService {
    ProcessResponse process(ProcessRequest request);
}


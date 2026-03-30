package com.subbu.smartdocsummary.service;

import com.subbu.smartdocsummary.dto.PresidioEntity;

import java.util.List;
import java.util.Map;

public interface TokenizationService {
    String tokenizeAndReplace(String text, List<PresidioEntity> entities, Map<String, String> mapping);
}

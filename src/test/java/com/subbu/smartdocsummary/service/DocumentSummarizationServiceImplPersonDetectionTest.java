package com.subbu.smartdocsummary.service;

import com.subbu.smartdocsummary.client.PresidioClient;
import com.subbu.smartdocsummary.dto.PresidioEntity;
import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.service.impl.DocumentSummarizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentSummarizationServiceImplPersonDetectionTest {

    @Mock
    private PresidioClient presidioClient;

    @Mock
    private TokenizationService tokenizationService;

    @Mock
    private LlmService llmService;

    @Captor
    private ArgumentCaptor<List<PresidioEntity>> entitiesCaptor;

    private DocumentSummarizationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentSummarizationServiceImpl(presidioClient, tokenizationService, llmService);

        when(tokenizationService.tokenizeAndReplace(anyString(), anyList(), anyMap()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class));
        when(llmService.summarize(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    }

    @Test
    void shouldDetectSingleCapitalizedNameAsPerson() {
        List<PresidioEntity> entities = processAndCaptureEntities("Saikumar", List.of());

        assertThat(entities)
                .anyMatch(entity -> "PERSON".equals(entity.getEntityType()) && spanText("Saikumar", entity).equals("Saikumar"));
    }

    @Test
    void shouldIgnoreLowercaseSingleWordName() {
        List<PresidioEntity> entities = processAndCaptureEntities("saikumar", List.of());

        assertThat(entities).noneMatch(entity -> "PERSON".equals(entity.getEntityType()));
    }

    @Test
    void shouldDetectBrokenNameWhenContextExists() {
        String input = "Name: saikumar. k";
        List<PresidioEntity> entities = processAndCaptureEntities(input, List.of());

        assertThat(entities).anyMatch(entity -> "PERSON".equals(entity.getEntityType()));
    }

    @Test
    void shouldNotDetectUsernameValueAsPerson() {
        List<PresidioEntity> entities = processAndCaptureEntities("username: saikumar", List.of());

        assertThat(entities).noneMatch(entity -> "PERSON".equals(entity.getEntityType()));
    }

    @Test
    void shouldKeepPresidioPersonDetection() {
        PresidioEntity presidioPerson = PresidioEntity.builder()
                .entityType("PERSON")
                .start(0)
                .end(8)
                .score(0.95)
                .build();

        List<PresidioEntity> entities = processAndCaptureEntities("saikumar", List.of(presidioPerson));

        assertThat(entities)
                .anyMatch(entity -> "PERSON".equals(entity.getEntityType()) && entity.getStart() == 0 && entity.getEnd() == 8);
    }

    private List<PresidioEntity> processAndCaptureEntities(String input, List<PresidioEntity> presidioOutput) {
        when(presidioClient.analyze(anyString())).thenReturn(Mono.just(presidioOutput));

        service.process(new ProcessRequest(input));

        verify(tokenizationService).tokenizeAndReplace(anyString(), entitiesCaptor.capture(), anyMap());
        return entitiesCaptor.getValue();
    }

    private String spanText(String text, PresidioEntity entity) {
        return text.substring(entity.getStart(), Math.min(entity.getEnd(), text.length()));
    }
}


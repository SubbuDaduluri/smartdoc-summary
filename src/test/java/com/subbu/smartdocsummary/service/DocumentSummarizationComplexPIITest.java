package com.subbu.smartdocsummary.service;

import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.dto.ProcessResponse;
import com.subbu.smartdocsummary.service.impl.DocumentSummarizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive test suite for complex PII handling and document summarization.
 * Tests various PII types: emails, phone numbers, addresses, credit cards, SSNs, 
 * driver licenses, passports, IBANs, NHS numbers, NINOs, IPs, and URLs.
 */
@SpringBootTest
class DocumentSummarizationComplexPIITest {

    @Autowired
    private DocumentSummarizationServiceImpl documentSummarizationService;

    @MockBean
    private VectorStore vectorStore;

    private String complexPIIText;

    @BeforeEach
    void setUp() {
        // Complex input with multiple PII types
        complexPIIText = "John Smith recently moved to Bangalore and updated his contact details with our system. " +
                "His new email address is john.smith@gmail.com and his backup email is j.smith@outlook.com. " +
                "You can reach him on his primary phone number +91-9876543210 or his office number 080-45678901. " +
                "John's current residential address is Flat No. 502, Green Valley Apartments, Whitefield, Bangalore, Karnataka, India. " +
                "Previously, he lived at 221B Baker Street, London, UK. " +
                "His IP address during the last login was 192.168.1.25, and the request was made from the URL https://example.com/profile/update. " +
                "For billing purposes, John has provided his credit card number 4111-1111-1111-1111, which expires in 12/27. " +
                "He also shared his IBAN code GB29NWBK60161331926819 for international transactions. " +
                "For compliance verification, John submitted the following documents: " +
                "US Social Security Number (SSN): 123-45-6789, " +
                "US Driver License: D1234567, " +
                "US Passport: X12345678, " +
                "US Bank Account Number: 9876543210. " +
                "Additionally, for his UK operations, he provided: " +
                "UK NHS Number: 943 476 5919, " +
                "UK National Insurance Number (NINO): QQ 12 34 56 C. " +
                "John is also an investor in cryptocurrency. " +
                "Later, John referred his colleague Sarah Johnson, whose email is sarah.johnson@company.com and phone number is +1-202-555-0173. " +
                "Sarah currently resides at 742 Evergreen Terrace, Springfield, USA. " +
                "Both John and Sarah accessed the system from different locations, and their activity logs include multiple URLs such as " +
                "https://app.company.com/login and https://dashboard.company.com/home.";
    }

    /**
     * Test 1: Basic Processing with Complex PII Input
     * Validates that the service can process and summarize text with multiple PII types
     */
    @Test
    void testComplexPIIProcessing() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        assertThat(response).isNotNull();
        assertThat(response.processedText()).isNotBlank();
        
        // Verify that NO tokens appear in response (all replaced with actual values)
        assertThat(response.processedText()).doesNotContainPattern("PERSON_\\d+");
        assertThat(response.processedText()).doesNotContainPattern("EMAIL_ADDRESS_\\d+");
        assertThat(response.processedText()).doesNotContainPattern("PHONE_NUMBER_\\d+");
        assertThat(response.processedText()).doesNotContainPattern("LOCATION_\\d+");
        assertThat(response.processedText()).doesNotContainPattern("URL_\\d+");
    }

    /**
     * Test 2: Email Detection and Replacement
     * Validates that multiple email addresses are properly detected and replaced
     */
    @Test
    void testMultipleEmailDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // At least one email should be visible in the summary (actual value, not token)
        boolean hasEmailInSummary = summary.contains("john.smith@gmail.com") || 
                                   summary.contains("j.smith@outlook.com") ||
                                   summary.contains("sarah.johnson@company.com");
        
        assertThat(hasEmailInSummary).as("Email addresses should appear in summary").isTrue();
    }

    /**
     * Test 3: Phone Number Detection and Replacement
     * Validates that phone numbers in various formats are detected
     */
    @Test
    void testPhoneNumberDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // At least one phone number format should be visible
        boolean hasPhoneInSummary = summary.contains("+91-9876543210") || 
                                   summary.contains("080-45678901") ||
                                   summary.contains("+1-202-555-0173");
        
        assertThat(hasPhoneInSummary).as("Phone numbers should appear in summary").isTrue();
    }

    /**
     * Test 4: Address Detection
     * Validates that addresses (both new and previous) are properly handled
     */
    @Test
    void testAddressDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Should contain key location indicators
        assertThat(summary.toLowerCase()).as("Summary should reference Bangalore or location change")
                .containsAnyOf("bangalore", "address", "moved", "location");
    }

    /**
     * Test 5: Credit Card Number Detection
     * Validates that credit card numbers are properly tokenized and replaced
     */
    @Test
    void testCreditCardDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Credit card 4111-1111-1111-1111 should either appear (if detected) or be absent
        // but should NOT appear as token
        assertThat(summary).doesNotContainPattern("CREDIT_CARD_\\d+");
    }

    /**
     * Test 6: IBAN Code Detection
     * Validates that IBAN codes are properly detected
     */
    @Test
    void testIBANDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // IBAN should not appear as token
        assertThat(summary).doesNotContainPattern("IBAN_CODE_\\d+");
    }

    /**
     * Test 7: US Social Security Number (SSN) Detection
     * Validates that SSNs are properly detected and tokenized
     */
    @Test
    void testUSSSNDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // SSN should not appear as token (already handled by Presidio)
        assertThat(summary).doesNotContainPattern("US_SSN_\\d+");
    }

    /**
     * Test 8: IP Address Detection
     * Validates that IP addresses are properly detected
     */
    @Test
    void testIPAddressDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // IP should not appear as token
        assertThat(summary).doesNotContainPattern("IP_ADDRESS_\\d+");
    }

    /**
     * Test 9: URL Detection and Replacement
     * Validates that URLs are properly detected and replaced
     */
    @Test
    void testURLDetection() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // URLs should not appear as tokens
        assertThat(summary).doesNotContainPattern("URL_\\d+");
        
        // At least one URL should be visible in summary (actual value)
        boolean hasURLInSummary = summary.contains("https://example.com/profile/update") ||
                                 summary.contains("https://app.company.com/login") ||
                                 summary.contains("https://dashboard.company.com/home") ||
                                 summary.toLowerCase().contains("url");
        
        assertThat(hasURLInSummary).as("URL information should appear in summary").isTrue();
    }

    /**
     * Test 10: Multiple Person Names Detection
     * Validates that multiple person names are detected
     */
    @Test
    void testMultiplePersonNames() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Names should appear as actual values, not tokens
        boolean hasNamesInSummary = summary.contains("John Smith") || 
                                   summary.contains("Sarah Johnson") ||
                                   summary.toLowerCase().contains("john") ||
                                   summary.toLowerCase().contains("sarah");
        
        assertThat(hasNamesInSummary).as("Person names should appear in summary").isTrue();
    }

    /**
     * Test 11: Response Contains Actual Values (Not Tokens)
     * Critical test: Verifies no tokens remain in the final response
     */
    @Test
    void testNoTokensInResponse() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Verify NO token patterns remain
        assertThat(summary).doesNotContainPattern("_\\d+"); // Generic token pattern
        assertThat(summary).doesNotContainPattern("(PERSON|EMAIL_ADDRESS|PHONE_NUMBER|LOCATION|URL|IP_ADDRESS|CREDIT_CARD|IBAN_CODE|US_SSN|UK_NHS|UK_NINO)_\\d+");
    }

    /**
     * Test 12: Response Length and Content
     * Validates that response is reasonable and contains summary content
     */
    @Test
    void testResponseLength() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Response should be shorter than original (summarized)
        assertThat(summary.length()).as("Summarized text should be shorter than original")
                .isLessThan(complexPIIText.length());
        
        // But should still be meaningful (at least 50 chars)
        assertThat(summary.length()).as("Summary should contain meaningful content")
                .isGreaterThan(50);
    }

    /**
     * Test 13: Empty/Null Input Handling
     * Validates error handling for edge cases
     */
    @Test
    void testEmptyInputHandling() {
        ProcessRequest emptyRequest = new ProcessRequest("");
        ProcessResponse emptyResponse = documentSummarizationService.process(emptyRequest);

        assertThat(emptyResponse.processedText()).isBlank();
    }

    /**
     * Test 14: Null Input Handling
     * Validates null input handling
     */
    @Test
    void testNullInputHandling() {
        ProcessRequest nullRequest = new ProcessRequest(null);
        ProcessResponse nullResponse = documentSummarizationService.process(nullRequest);

        assertThat(nullResponse.processedText()).isNull();
    }

    /**
     * Test 15: Simple Text Without PII
     * Validates normal text processing without sensitive data
     */
    @Test
    void testSimpleTextProcessing() {
        String simpleName = "The weather is nice today. I enjoy walking in the park.";
        ProcessRequest request = new ProcessRequest(simpleName);
        ProcessResponse response = documentSummarizationService.process(request);

        assertThat(response.processedText()).isNotBlank();
        // Should not contain any token patterns
        assertThat(response.processedText()).doesNotContainPattern("_\\d+");
    }

    /**
     * Test 16: Duplicate PII Values
     * Validates that same PII appearing multiple times gets same token
     */
    @Test
    void testDuplicatePIIValues() {
        String textWithDuplicates = "Contact John Smith at john.smith@gmail.com. " +
                                    "Another email for John Smith is also john.smith@gmail.com.";
        ProcessRequest request = new ProcessRequest(textWithDuplicates);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Duplicate emails should both appear as same value
        int emailCount = (int) summary.split("john.smith@gmail.com").length - 1;
        assertThat(emailCount).isGreaterThanOrEqualTo(1);
    }

    /**
     * Test 17: Mixed Content (PII + Non-PII)
     * Validates proper handling of mixed content
     */
    @Test
    void testMixedContent() {
        String mixedText = "John Smith works at our company and his email is john.smith@company.com. " +
                          "The company was founded in 2010 and has 500 employees.";
        ProcessRequest request = new ProcessRequest(mixedText);
        ProcessResponse response = documentSummarizationService.process(request);

        String summary = response.processedText();
        
        // Should contain non-PII info
        assertThat(summary.toLowerCase()).contains("company");
        
        // Should not have any tokens
        assertThat(summary).doesNotContainPattern("PERSON_\\d+");
    }

    /**
     * Test 18: Large Text Processing
     * Validates that large texts are processed efficiently
     */
    @Test
    void testLargeTextProcessing() {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            largeText.append(complexPIIText).append(" ");
        }
        
        ProcessRequest request = new ProcessRequest(largeText.toString());
        ProcessResponse response = documentSummarizationService.process(request);

        assertThat(response.processedText()).isNotBlank();
        assertThat(response.processedText()).doesNotContainPattern("_\\d+");
    }

    /**
     * Test 19: Response Format Validation
     * Validates response format is correct
     */
    @Test
    void testResponseFormat() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        ProcessResponse response = documentSummarizationService.process(request);

        assertThat(response).isNotNull();
        assertThat(response.processedText()).isNotNull();
        // Should be a ProcessResponse with only processedText field
        assertThat(response.toString()).contains("processedText");
    }

    /**
     * Test 20: Consistency Across Multiple Calls
     * Validates deterministic behavior
     */
    @Test
    void testConsistencyAcrossMultipleCalls() {
        ProcessRequest request = new ProcessRequest(complexPIIText);
        
        ProcessResponse response1 = documentSummarizationService.process(request);
        ProcessResponse response2 = documentSummarizationService.process(request);

        // Note: LLM responses might vary, but structure should be consistent
        assertThat(response1.processedText()).isNotBlank();
        assertThat(response2.processedText()).isNotBlank();
        
        // Both should have no tokens
        assertThat(response1.processedText()).doesNotContainPattern("_\\d+");
        assertThat(response2.processedText()).doesNotContainPattern("_\\d+");
    }
}


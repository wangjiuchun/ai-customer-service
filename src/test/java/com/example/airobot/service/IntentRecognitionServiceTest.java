package com.example.airobot.service;

import com.example.airobot.entity.Intention;
import com.example.airobot.repository.IntentionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class IntentRecognitionServiceTest {

    @Autowired
    private IntentRecognitionService intentRecognitionService;

    @Autowired
    private IntentionRepository intentionRepository;

    @BeforeEach
    void setUp() {
        intentionRepository.deleteAll();
        intentionRepository.save(Intention.builder()
                .name("product_inquiry")
                .description("产品咨询")
                .keywords("产品,功能,特点")
                .responseTemplate("我们的产品具有以下特点...")
                .status(1)
                .build());
    }

    @Test
    void recognize_withMatchingKeyword_shouldReturnRecognized() {
        var result = intentRecognitionService.recognize("我想了解产品功能");

        assertEquals(IntentRecognitionService.IntentResult.RECOGNIZED, result.result);
        assertNotNull(result.intention);
        assertEquals("product_inquiry", result.intention.getName());
    }
}
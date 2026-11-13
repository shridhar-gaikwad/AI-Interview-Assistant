package io.interviewAssistant.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AssistantProcessor {

    private static final Logger log = LoggerFactory.getLogger(AssistantProcessor.class);

    public static void main(String[] args) {

        SpringApplication.run(AssistantProcessor.class, args);
        log.info("Application started");

    }

}

package com.jadenf7.jobscheduler.infrastructure.adapter.out.handler;

import com.jadenf7.jobscheduler.domain.exception.NonRetryableTaskException;
import com.jadenf7.jobscheduler.domain.model.TaskPayload;
import com.jadenf7.jobscheduler.domain.port.out.TaskHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailTaskHandler.class);
    public static final String TASK_TYPE = "SEND_EMAIL";

    @Override
    public String supportedType() {
        return TASK_TYPE;
    }

    @Override
    public void handle(TaskPayload payload) throws Exception {
        String to = payload.data().get("to");
        String subject = payload.data().get("subject");
        if (to == null || to.isBlank()) {
            throw new NonRetryableTaskException("SEND_EMAIL payload missing 'to' field");
        }

        log.info("Sending email to={} subject={}", to, subject);
    }
}

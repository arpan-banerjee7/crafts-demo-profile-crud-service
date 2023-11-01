package com.crafts.profileservice.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

@Slf4j
@NoArgsConstructor
public final class KafkaUtil {

    /**
     * Method to get the event value from kafka headers
     **/
    public static String getEventValue(Headers headers, String eventKey) {
        for (Header header : headers) {
            if ("EVENT_TYPE".equals(header.key())) {
                log.info("Event type is {}", new String(header.value()));
                return new String(header.value());
            }
        }
        return null;
    }

}

package com.crafts.profileservice.config;

import com.crafts.profileservice.config.props.KafkaPropsConfig;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Getter
public class KakfaConfig {

    @Autowired
    private KafkaPropsConfig kafkaPropsConfig;

    @Bean
    public Map<String, Object> getKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, kafkaPropsConfig.getProducerKeySerializer());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, kafkaPropsConfig.getProducerValueSerializer());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaPropsConfig.getConsumerKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaPropsConfig.getConsumerValueDeserializer());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaPropsConfig.getConsumerAutoOffsetReset());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaPropsConfig.getConsumerMaxPollRecords());
        return props;
    }

    @Bean(name = "userProfileSubmissionKafkaTemplate")
    public KafkaTemplate<String, String> userProfileSubmissionKafkaTemplate() {
        Map<String, Object> props = getKafkaTemplate();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaPropsConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaPropsConfig.getUpsConsumerGroupId());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }

    @Bean(name = "userProfileValidationResultKafkaTemplate")
    public KafkaTemplate<String, String> userProfileValidationResultKafkaTemplate() {
        Map<String, Object> props = getKafkaTemplate();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaPropsConfig.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaPropsConfig.getUpvConsumerGroupId());
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}

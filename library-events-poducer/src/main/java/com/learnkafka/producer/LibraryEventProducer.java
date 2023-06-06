package com.learnkafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.domain.LibraryEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class LibraryEventProducer {
    @Autowired
    KafkaTemplate<Integer, String> kafkaTemplate;
    @Autowired
    ObjectMapper objectMapper;
    String topic = "library-events";
    // This one has Asynchronous Behaviour
    public void sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.getLibraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);
        ListenableFuture<SendResult<Integer, String>> listenableFuture = kafkaTemplate.sendDefault(key, value);
        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(key, value, result);
            }
        });
    }
    // This one has Asynchronous Behaviour
    // Implementing using "ProducerRecord"
    public void sendLibraryEvent_Approach2(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.getLibraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);
        ProducerRecord<Integer, String> producerRecord = buildProducerRecord(key, value, topic);
        ListenableFuture<SendResult<Integer, String>> listenableFuture = kafkaTemplate.send(producerRecord);
        listenableFuture.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {
            @Override
            public void onFailure(Throwable ex) {
                handleFailure(key, value, ex);
            }

            @Override
            public void onSuccess(SendResult<Integer, String> result) {
                handleSuccess(key, value, result);
            }
        });
    }

    private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value, String topic) {
        // Adding RecordHeader
        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "scanner".getBytes()),
                new RecordHeader("developed-by", "KloudSpot".getBytes()));
        return new ProducerRecord<Integer, String>(topic, null, key, value, recordHeaders);
    }

    public SendResult<Integer, String> sendLibraryEventSynchronous(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        SendResult<Integer, String> sendResult = null;
        Integer key = libraryEvent.getLibraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);
        try {
            // So when you make a call on .get(), this is when you are going to wait until the 'ListenableFuture' returned
            // Successfully or Failure. Basically, it is going to wait until the ListenableFuture is resolved to 'OnSuccess'
            // or 'OnFailure'.
            sendResult = kafkaTemplate.sendDefault(key, value).get();
            // The below code will be wait for '1-Sec'
            // sendResult = kafkaTemplate.sendDefault(key, value).get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException e) {
            log.error("ExecutionException/InterruptedException sending the Message and the exception is {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception sending the Message and the exception is {}", e.getMessage());
            throw e;
        }
        return sendResult;
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error("Error sending the Message and the exception is {}", ex.getMessage());
        try {
            throw ex;
        } catch (Throwable throwable) {
            log.error("Error in onFailure : {}", throwable.getMessage());
        }
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
        log.info("Message Sent Successfully for the key : {} and the value is {} ,partition is {}", key, value, result.getRecordMetadata().partition());
    }
}

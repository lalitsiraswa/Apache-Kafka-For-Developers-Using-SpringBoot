package controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.LibraryEventsPoducerApplication;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// "webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT" this is going to take care of generating a random
// PORT every time you launch the application, if you don't provide this configuration, then every time when you
// launch the application, it is going to launch in the same PORT, which is by-default 8080, So this is going to
// avoid the conflicts with the PORT.
@SpringBootTest(classes = LibraryEventsPoducerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
// Here we override the 'spring.kafka.producer.bootstrap-servers' property of application.yml file to 'spring.embedded.kafka.brokers'
// we get this from 'EmbeddedKafkaBroker,java' file.
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
public class LibraryEventsControllerIntegrationTest {
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private ObjectMapper MAPPER;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;
    private Consumer<Integer, String> consumer;

//    Before Each Test-Case
    @BeforeEach
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }
//    After Each Test-Case
    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    @Timeout(5) // We are using @Timeout(Default unit is Seconds) instead of Thread.sleep(5000);
    // @Timeout(5) :- This test-case is going to wait for 5-Seconds before it completes execution.
    void postLibraryEvent() throws JsonProcessingException, InterruptedException {
//        given
        Book book = Book.builder()
                .bookId(201)
                .bookName("Radha-Krishan Ki Gaatha")
                .bookAuthor("RamKrishna")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .libraryEventType(LibraryEventType.NEW)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", MediaType.APPLICATION_JSON.toString());
        HttpEntity<LibraryEvent> request = new HttpEntity<>(libraryEvent, headers);
        String expectedRecord = MAPPER.writeValueAsString(libraryEvent);
//        when
        ResponseEntity<LibraryEvent> responseEntity = testRestTemplate.exchange("/v1/libraryevent", HttpMethod.POST, request, LibraryEvent.class);
//        then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
//        The KafkaTestUtils.getSingleRecord() Call has Asynchronous Behaviour
        ConsumerRecord<Integer, String> consumerRecord = KafkaTestUtils.getSingleRecord(consumer, "library-events");
        // Thread.sleep(5000); // Don't use Thread.sleep(), instead use @Timeout(Default Unit is Seconds)
        String value = consumerRecord.value();
        assertEquals(expectedRecord, value);
    }
}

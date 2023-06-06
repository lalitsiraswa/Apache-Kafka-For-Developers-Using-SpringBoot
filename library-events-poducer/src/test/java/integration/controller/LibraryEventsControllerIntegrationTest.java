package controller;

import com.learnkafka.LibraryEventsPoducerApplication;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// "webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT" this is going to take care of generating a random
// PORT every time you launch the application, if you don't provide this configuration, then every time when you
// launch the application, it is going to launch in the same PORT, which is by-default 8080, So this is going to
// avoid the conflicts with the PORT.
@SpringBootTest(classes = LibraryEventsPoducerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LibraryEventsControllerIntegrationTest {
    @Autowired
    TestRestTemplate testRestTemplate;
    @Test
    void postLibraryEvent(){
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
//        when
        ResponseEntity<LibraryEvent> responseEntity = testRestTemplate.exchange("/v1/libraryevent", HttpMethod.POST, request, LibraryEvent.class);
//        then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
    }
}

package controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkafka.LibraryEventsPoducerApplication;
import com.learnkafka.controller.LibraryEventsController;
import com.learnkafka.domain.Book;
import com.learnkafka.domain.LibraryEvent;
import com.learnkafka.domain.LibraryEventType;
import com.learnkafka.producer.LibraryEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import javax.validation.constraints.NotNull;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// We can use @WebMvcTest(Name of Controller.class) annotation when we are writing Unit-Test for controller Layers.
@WebMvcTest(LibraryEventsController.class)
@ContextConfiguration(classes=LibraryEventsPoducerApplication.class)
@AutoConfigureMockMvc
public class LibraryEventControllerUnitTest {
    //  This MocMvc now have the access to all the end-points that are the part of "LibraryEventsController.class" Controller
    @Autowired
    MockMvc mockMvc;
    private static ObjectMapper MAPPER = new ObjectMapper();
    @MockBean
    LibraryEventProducer LibraryEventProducer;

    @Test
    public void postLibraryEvent() throws Exception {
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
        String json = MAPPER.writeValueAsString(libraryEvent);
        doNothing().when(LibraryEventProducer).sendLibraryEvent_Approach2(isA(LibraryEvent.class));
//        when
        mockMvc.perform(post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
//        then
    }

    @Test
    public void postLibraryEvent_4xx() throws Exception {
//        given
        Book book = Book.builder()
                .bookId(null)
                .bookName(null)
                .bookAuthor("RamKrishna")
                .build();
        LibraryEvent libraryEvent = LibraryEvent.builder()
                .libraryEventId(null)
                .book(book)
                .libraryEventType(LibraryEventType.NEW)
                .build();
        String json = MAPPER.writeValueAsString(libraryEvent);
        doNothing().when(LibraryEventProducer).sendLibraryEvent_Approach2(isA(LibraryEvent.class));
//        when
        String expectedErrorMessage = "book.bookId - must not be null, book.bookName - must not be blank";
        mockMvc.perform(post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(expectedErrorMessage));
//        then
    }
}

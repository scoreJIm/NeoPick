package com.neopick.adapter.web.controller;

import com.neopick.application.message.MessageUseCase;
import com.neopick.domain.message.*;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
@Import(MessageUseCase.class)
@DisplayName("Message API Integration Tests")
class MessageControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private ConversationRepository conversationRepository;
    @MockBean private SecurityContext securityContext;

    private static final String CONV_ID = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final ConversationId CONVERSATION_ID = ConversationId.from(CONV_ID);

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn("student-001");
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of("student-001"));
    }

    @Nested
    @DisplayName("GET /api/v1/conversations — List conversations")
    class ListConversations {

        @Test
        @DisplayName("should return user's conversations")
        void shouldReturnConversations() throws Exception {
            var conv = new Conversation(CONVERSATION_ID, "student-001", 100L);
            when(conversationRepository.findByStudentId("student-001"))
                    .thenReturn(List.of(conv));

            mockMvc.perform(get("/api/v1/conversations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/conversations — Start conversation")
    class StartConversation {

        @Test
        @DisplayName("should create a new conversation")
        void shouldStartConversation() throws Exception {
            var conv = new Conversation(ConversationId.generate(), "student-001", 100L);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);

            mockMvc.perform(post("/api/v1/conversations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"teacher_id\": 100}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.student_id").value("student-001"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/conversations/{id}/messages — Send message")
    class SendMessage {

        @Test
        @DisplayName("should send a message in conversation")
        void shouldSendMessage() throws Exception {
            var msg = new ChatMessage(UUID.randomUUID(), CONV_ID, "student-001",
                    "100", "Hello!", MessageType.TEXT);
            when(conversationRepository.findById(CONVERSATION_ID))
                    .thenReturn(Optional.of(new Conversation(CONVERSATION_ID, "student-001", 100L)));
            when(conversationRepository.saveMessage(any(ChatMessage.class))).thenReturn(msg);

            mockMvc.perform(post("/api/v1/conversations/{id}/messages", CONV_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"Hello!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Hello!"));
        }

        @Test
        @DisplayName("should reject empty message content")
        void shouldRejectEmptyMessage() throws Exception {
            mockMvc.perform(post("/api/v1/conversations/{id}/messages", CONV_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/conversations/{id}/messages — Get messages")
    class GetMessages {

        @Test
        @DisplayName("should return messages in conversation")
        void shouldReturnMessages() throws Exception {
            var msg = new ChatMessage(UUID.randomUUID(), CONV_ID, "student-001",
                    "100", "Hi", MessageType.TEXT);
            when(conversationRepository.findMessages(CONV_ID, 0, 50))
                    .thenReturn(List.of(msg));

            mockMvc.perform(get("/api/v1/conversations/{id}/messages", CONV_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].content").value("Hi"));
        }
    }
}

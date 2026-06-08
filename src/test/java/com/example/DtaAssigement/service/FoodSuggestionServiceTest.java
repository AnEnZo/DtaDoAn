package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.FoodSuggestionRequest;
import com.example.DtaAssigement.dto.FoodSuggestionResponse;
import com.example.DtaAssigement.entity.FoodSuggestion;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.repository.FoodSuggestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodSuggestionServiceTest {

    @Mock
    private FoodSuggestionRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private FoodSuggestionService service;

    private User testUser;
    private FoodSuggestionRequest request;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .displayName("Test User")
                .build();

        request = FoodSuggestionRequest.builder()
                .foodName("Pho Bo")
                .description("Delicious beef noodle soup")
                .category("MAIN_DISH")
                .build();
    }

    @Test
    void submitSuggestion_anonymousUser_throwsUnauthorized() {
        assertThrows(ResponseStatusException.class, () -> {
            service.submitSuggestion(request, null);
        });
    }

    @Test
    void submitSuggestion_withinRateLimit_newFood_createsSuggestion() {
        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Mock DB
        when(repository.findByFoodNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(FoodSuggestion.class))).thenAnswer(invocation -> {
            FoodSuggestion s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        FoodSuggestionResponse response = service.submitSuggestion(request, testUser);

        assertNotNull(response);
        assertEquals("pho bo", response.getFoodName());
        assertEquals(1, response.getVotes());
        verify(repository, times(1)).save(any(FoodSuggestion.class));
    }

    @Test
    void submitSuggestion_withinRateLimit_existingFood_incrementsVotes() {
        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L);

        // Mock DB
        FoodSuggestion existingSuggestion = FoodSuggestion.builder()
                .id(2L)
                .foodName("pho bo")
                .votes(3)
                .build();
        when(repository.findByFoodNameIgnoreCase("pho bo")).thenReturn(Optional.of(existingSuggestion));

        FoodSuggestionResponse response = service.submitSuggestion(request, testUser);

        assertNotNull(response);
        assertEquals("pho bo", response.getFoodName());
        assertEquals(4, response.getVotes());
        verify(repository, times(1)).save(existingSuggestion);
    }

    @Test
    void submitSuggestion_exceedsRateLimit_throwsTooManyRequests() {
        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(11L); // 11th suggestion

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            service.submitSuggestion(request, testUser);
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatusCode());
        assertTrue(exception.getReason().contains("quá giới hạn đề xuất 10 món"));
        verify(repository, never()).save(any());
    }
}

package com.streamworx.todoservice.service;

import com.streamworx.todoservice.dto.TodoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);

    private final RestClient restClient;
    private final String todosUrl;

    public TodoService(@Value("${jsonplaceholder.todos-url:https://jsonplaceholder.typicode.com/todos}") String todosUrl) {
        this.todosUrl = todosUrl;
        this.restClient = RestClient.builder().build();
    }

    public List<TodoDto> getTodos(Long userId) {
        try {
            log.info("Fetching todos from {} (userId filter: {})", todosUrl, userId);
            TodoDto[] response;
            if (userId != null) {
                response = restClient.get()
                        .uri(todosUrl + "?userId={userId}", userId)
                        .retrieve()
                        .body(TodoDto[].class);
            } else {
                response = restClient.get()
                        .uri(todosUrl)
                        .retrieve()
                        .body(TodoDto[].class);
            }

            if (response != null) {
                return Arrays.asList(response);
            }
        } catch (Exception e) {
            log.warn("Error retrieving todos from external source: {}. Using fallback dummy data.", e.getMessage());
            try (java.io.InputStream is = getClass().getResourceAsStream("/todos-fallback.json")) {
                if (is != null) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    TodoDto[] fallback = mapper.readValue(is, TodoDto[].class);
                    if (fallback != null) {
                        if (userId != null) {
                            return Arrays.stream(fallback)
                                    .filter(t -> userId.equals(t.getUserId()))
                                    .toList();
                        }
                        return Arrays.asList(fallback);
                    }
                }
            } catch (Exception ex) {
                log.error("Failed to load fallback todos: {}", ex.getMessage());
            }
        }
        return Collections.emptyList();
    }
}

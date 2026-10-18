package com.streamworx.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.streamworx.userservice.dto.PostDto;
import com.streamworx.userservice.dto.PostXmlResponse;
import com.streamworx.userservice.dto.TodoDto;
import com.streamworx.userservice.dto.UserDetailResponse;
import com.streamworx.userservice.dto.UserDto;
import com.streamworx.userservice.exception.UserNotFoundException;
import com.streamworx.userservice.loader.UserDataLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;
    private final RestClient restClient;
    private final UserDataLoader userDataLoader;
    private final String todoServiceUrl;
    private final String postServiceUrl;

    public UserService(StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       XmlMapper xmlMapper,
                       RestClient restClient,
                       UserDataLoader userDataLoader,
                       @Value("${services.todo.url:http://localhost:8083}") String todoServiceUrl,
                       @Value("${services.post.url:http://localhost:8082}") String postServiceUrl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.xmlMapper = xmlMapper;
        this.restClient = restClient;
        this.userDataLoader = userDataLoader;
        this.todoServiceUrl = todoServiceUrl;
        this.postServiceUrl = postServiceUrl;
    }

    public UserDetailResponse getUserDetail(Long id) {
        log.info("Fetching combined user detail for userId={}", id);

        // 1. Fetch User from Redis Cache
        UserDto user = getUserFromRedis(id);
        if (user == null) {
            Boolean isSeeded = redisTemplate.hasKey("users:seeded");
            if (!Boolean.TRUE.equals(isSeeded)) {
                log.info("Redis cache not seeded yet. Attempting on-demand seeding...");
                userDataLoader.seedUsers();
                user = getUserFromRedis(id);
            }
        }

        if (user == null) {
            throw new UserNotFoundException("User with id " + id + " not found in Redis cache.");
        }

        // 2. Fetch Todos from API 3 (JSON)
        List<TodoDto> todos = fetchTodosForUser(id);

        // 3. Fetch Posts from API 2 (XML)
        List<PostDto> posts = fetchPostsForUser(id);

        // 4. Combine into final response
        return new UserDetailResponse(user, todos, posts);
    }

    public List<UserDto> getAllUsers() {
        Set<String> idStrings = redisTemplate.opsForSet().members("users:ids");
        if (idStrings == null || idStrings.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserDto> users = new ArrayList<>();
        for (String idStr : idStrings) {
            try {
                Long id = Long.valueOf(idStr);
                UserDto user = getUserFromRedis(id);
                if (user != null) {
                    users.add(user);
                }
            } catch (Exception e) {
                log.warn("Failed to parse user from id: {}", idStr);
            }
        }
        users.sort(Comparator.comparing(UserDto::getId));
        return users;
    }

    private UserDto getUserFromRedis(Long id) {
        try {
            String userJson = redisTemplate.opsForValue().get("user:" + id);
            if (userJson != null) {
                return objectMapper.readValue(userJson, UserDto.class);
            }
        } catch (Exception e) {
            log.error("Error reading user {} from Redis: {}", id, e.getMessage(), e);
        }
        return null;
    }

    private List<TodoDto> fetchTodosForUser(Long userId) {
        try {
            String uri = todoServiceUrl + "/todos?userId=" + userId;
            log.info("Calling API 3 (Todo Service) at: {}", uri);
            TodoDto[] todos = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(TodoDto[].class);

            if (todos != null) {
                return Arrays.asList(todos);
            }
        } catch (Exception e) {
            log.error("Failed to fetch todos from API 3 for userId {}: {}", userId, e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private List<PostDto> fetchPostsForUser(Long userId) {
        try {
            String uri = postServiceUrl + "/posts?userId=" + userId;
            log.info("Calling API 2 (Post Service) at: {}", uri);
            String xmlResponse = restClient.get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_XML)
                    .retrieve()
                    .body(String.class);

            if (xmlResponse != null && !xmlResponse.isBlank()) {
                PostXmlResponse postXml = xmlMapper.readValue(xmlResponse, PostXmlResponse.class);
                if (postXml != null && postXml.getRows() != null) {
                    return postXml.getRows();
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch/parse posts XML from API 2 for userId {}: {}", userId, e.getMessage(), e);
        }
        return Collections.emptyList();
    }
}

package com.streamworx.userservice.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamworx.userservice.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataLoader.class);
    private static final String SEED_FLAG_KEY = "users:seeded";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String usersUrl;

    public UserDataLoader(StringRedisTemplate redisTemplate,
                          ObjectMapper objectMapper,
                          @Value("${jsonplaceholder.users-url:https://jsonplaceholder.typicode.com/users}") String usersUrl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.usersUrl = usersUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public void run(String... args) {
        seedUsers();
    }

    public synchronized void seedUsers() {
        try {
            Boolean isSeeded = redisTemplate.hasKey(SEED_FLAG_KEY);
            if (Boolean.TRUE.equals(isSeeded)) {
                log.info("Redis cache is already seeded with user data. Skipping seeding.");
                return;
            }

            log.info("Redis cache is empty for users. Auto-seeding from: {}", usersUrl);
            UserDto[] users = null;
            int maxRetries = 5;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    users = restClient.get()
                            .uri(usersUrl)
                            .retrieve()
                            .body(UserDto[].class);
                    if (users != null && users.length > 0) {
                        break;
                    }
                } catch (Exception ex) {
                    log.warn("Seeding attempt {}/{} failed: {}. Retrying in 2 seconds...", attempt, maxRetries, ex.getMessage());
                    if (attempt < maxRetries) {
                        Thread.sleep(2000);
                    }
                }
            }

            if (users == null || users.length == 0) {
                log.info("Attempting to load fallback dummy users from classpath: /users-fallback.json");
                try (java.io.InputStream is = getClass().getResourceAsStream("/users-fallback.json")) {
                    if (is != null) {
                        users = objectMapper.readValue(is, UserDto[].class);
                        log.info("Successfully loaded {} fallback dummy users from classpath.", users != null ? users.length : 0);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to load fallback dummy users: {}", ex.getMessage());
                }
            }

            if (users != null && users.length > 0) {
                for (UserDto user : users) {
                    String userJson = objectMapper.writeValueAsString(user);
                    // Store individually by user ID: user:{id}
                    redisTemplate.opsForValue().set("user:" + user.getId(), userJson);
                    // Store user ID in set for directory lookup
                    redisTemplate.opsForSet().add("users:ids", String.valueOf(user.getId()));
                }
                redisTemplate.opsForValue().set(SEED_FLAG_KEY, "true");
                log.info("Successfully seeded {} users into Redis cache.", users.length);
            } else {
                log.warn("No users retrieved from {}", usersUrl);
            }
        } catch (Exception e) {
            log.error("Failed to seed users into Redis: {}", e.getMessage(), e);
        }
    }
}

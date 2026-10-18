package com.streamworx.postservice.loader;

import com.streamworx.postservice.entity.Post;
import com.streamworx.postservice.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;

@Component
public class PostDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PostDataLoader.class);

    private final PostRepository postRepository;
    private final RestClient restClient;
    private final String postsUrl;

    public PostDataLoader(PostRepository postRepository,
                          @Value("${jsonplaceholder.posts-url:https://jsonplaceholder.typicode.com/posts}") String postsUrl) {
        this.postRepository = postRepository;
        this.postsUrl = postsUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public void run(String... args) {
        try {
            long currentCount = postRepository.count();
            if (currentCount == 0) {
                log.info("Post database is empty. Starting auto-seed from: {}", postsUrl);
                Post[] posts = restClient.get()
                        .uri(postsUrl)
                        .retrieve()
                        .body(Post[].class);

                if (posts != null && posts.length > 0) {
                    postRepository.saveAll(Arrays.asList(posts));
                    log.info("Successfully seeded {} posts into PostgreSQL database 'posts_db'.", posts.length);
                } else {
                    log.warn("No posts received from {}", postsUrl);
                }
            } else {
                log.info("Post database already contains {} records. Seeding skipped.", currentCount);
            }
        } catch (Exception e) {
            log.error("Failed to seed posts into PostgreSQL: {}", e.getMessage(), e);
        }
    }
}

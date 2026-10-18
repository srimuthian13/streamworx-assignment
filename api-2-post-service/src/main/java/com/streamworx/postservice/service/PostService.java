package com.streamworx.postservice.service;

import com.streamworx.postservice.dto.PostDto;
import com.streamworx.postservice.dto.PostXmlResponse;
import com.streamworx.postservice.entity.Post;
import com.streamworx.postservice.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PostService {

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public PostXmlResponse getPostsXml(Long userId) {
        List<Post> posts;
        if (userId != null) {
            log.info("Fetching posts for userId={}", userId);
            posts = postRepository.findByUserId(userId);
        } else {
            log.info("Fetching all posts");
            posts = postRepository.findAll();
        }

        List<PostDto> dtos = posts.stream()
                .map(this::mapToDto)
                .toList();

        return new PostXmlResponse(dtos);
    }

    public PostXmlResponse getPostByIdXml(Long id) {
        log.info("Fetching post by id={}", id);
        return postRepository.findById(id)
                .map(post -> new PostXmlResponse(Collections.singletonList(mapToDto(post))))
                .orElseGet(() -> new PostXmlResponse(Collections.emptyList()));
    }

    private PostDto mapToDto(Post post) {
        return new PostDto(
                post.getUserId(),
                post.getId(),
                post.getTitle(),
                post.getBody()
        );
    }
}

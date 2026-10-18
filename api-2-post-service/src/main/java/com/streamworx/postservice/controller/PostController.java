package com.streamworx.postservice.controller;

import com.streamworx.postservice.dto.PostXmlResponse;
import com.streamworx.postservice.service.PostService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<PostXmlResponse> getPosts(@RequestParam(required = false) Long userId) {
        PostXmlResponse response = postService.getPostsXml(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<PostXmlResponse> getPostById(@PathVariable Long id) {
        PostXmlResponse response = postService.getPostByIdXml(id);
        return ResponseEntity.ok(response);
    }
}

package com.streamworx.todoservice.controller;

import com.streamworx.todoservice.dto.TodoDto;
import com.streamworx.todoservice.service.TodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ResponseEntity<List<TodoDto>> getTodos(@RequestParam(required = false) Long userId) {
        List<TodoDto> todos = todoService.getTodos(userId);
        return ResponseEntity.ok(todos);
    }
}

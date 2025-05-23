package com.example.labspring1.controller;

import com.example.labspring1.dto.GroupDto;
import com.example.labspring1.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupDto> getAllGroups() {
        return groupService.findAll();
    }

    @GetMapping("/{id}")
    public GroupDto getGroupById(@PathVariable Long id) {
        return groupService.findById(id);
    }

    @GetMapping("/number/{groupNumber}")
    public GroupDto getGroupByNumber(@PathVariable String groupNumber) {
        return groupService.findByGroupNumber(groupNumber);
    }

    @PostMapping
    public GroupDto createGroup(@RequestBody GroupDto groupDto) {
        return groupService.create(groupDto);
    }

    @PutMapping("/{id}")
    public GroupDto updateGroup(@PathVariable Long id, @RequestBody GroupDto groupDto) {
        return groupService.update(id, groupDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
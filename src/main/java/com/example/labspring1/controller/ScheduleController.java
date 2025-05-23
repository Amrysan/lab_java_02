package com.example.labspring1.controller;

import com.example.labspring1.dto.ScheduleDto;
import com.example.labspring1.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<ScheduleDto> getAllSchedules() {
        return scheduleService.findAll();
    }

    @GetMapping("/{id}")
    public ScheduleDto getScheduleById(@PathVariable Long id) {
        return scheduleService.findById(id);
    }

    @GetMapping("/group/{groupId}")
    public List<ScheduleDto> getSchedulesByGroupId(@PathVariable Long groupId) {
        return scheduleService.findByGroupId(groupId);
    }

    @GetMapping("/api")
    public ScheduleDto getScheduleFromApi(@RequestParam String group, @RequestParam String date) {
        return scheduleService.getSchedule(group, date);
    }

    @PostMapping
    public ScheduleDto createSchedule(@RequestBody ScheduleDto scheduleDto) {
        return scheduleService.create(scheduleDto);
    }

    @PutMapping("/{id}")
    public ScheduleDto updateSchedule(@PathVariable Long id, @RequestBody ScheduleDto scheduleDto) {
        return scheduleService.update(id, scheduleDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
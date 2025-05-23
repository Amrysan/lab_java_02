package com.example.labspring1.service;

import com.example.labspring1.dto.GroupDto;
import com.example.labspring1.dto.ScheduleDto;
import com.example.labspring1.model.Group;
import com.example.labspring1.model.Schedule;
import com.example.labspring1.repository.GroupRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupDto> findAll() {
        return groupRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupDto findById(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        return convertToDto(group);
    }

    @Transactional(readOnly = true)
    public GroupDto findByGroupNumber(String groupNumber) {
        Group group = groupRepository.findByGroupNumber(groupNumber)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with number: " + groupNumber));
        return convertToDto(group);
    }

    @Transactional
    public GroupDto create(GroupDto groupDto) {
        Group group = new Group();
        group.setGroupNumber(groupDto.getGroupNumber());
        Group savedGroup = groupRepository.save(group);
        return convertToDto(savedGroup);
    }

    @Transactional
    public GroupDto update(Long id, GroupDto groupDto) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
        group.setGroupNumber(groupDto.getGroupNumber());
        Group updatedGroup = groupRepository.save(group);
        return convertToDto(updatedGroup);
    }

    @Transactional
    public void delete(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new EntityNotFoundException("Group not found with id: " + id);
        }
        groupRepository.deleteById(id);
    }

    private GroupDto convertToDto(Group group) {
        List<ScheduleDto> scheduleDtos = group.getSchedules().stream()
                .map(this::convertToScheduleDto)
                .collect(Collectors.toList());
        return new GroupDto(group.getId(), group.getGroupNumber(), scheduleDtos);
    }

    private ScheduleDto convertToScheduleDto(Schedule schedule) {
        return new ScheduleDto(
                schedule.getId(),
                schedule.getSubject(),
                schedule.getLessonType(),
                schedule.getTime(),
                schedule.getAuditorium(),
                schedule.getGroup().getId()
        );
    }
}
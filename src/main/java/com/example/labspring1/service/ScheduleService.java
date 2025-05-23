package com.example.labspring1.service;

import com.example.labspring1.dto.ScheduleDto;
import com.example.labspring1.model.Group;
import com.example.labspring1.model.Schedule;
import com.example.labspring1.repository.GroupRepository;
import com.example.labspring1.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final String BSUIR_API_URL = "https://iis.bsuir.by/api/v1/schedule?studentGroup=%s";
    private static final Map<String, String> DAY_OF_WEEK_MAP = Map.of(
            "monday", "Понедельник",
            "tuesday", "Вторник",
            "wednesday", "Среда",
            "thursday", "Четверг",
            "friday", "Пятница",
            "saturday", "Суббота",
            "sunday", "Воскресенье"
    );

    private final GroupRepository groupRepository;
    private final ScheduleRepository scheduleRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ScheduleService(GroupRepository groupRepository, ScheduleRepository scheduleRepository) {
        this.groupRepository = groupRepository;
        this.scheduleRepository = scheduleRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Transactional(readOnly = true)
    public ScheduleDto getSchedule(String groupNumber, String date) {
        Group group = groupRepository.findByGroupNumber(groupNumber)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with number: " + groupNumber));

        ScheduleDto response = new ScheduleDto();
        response.setGroupNumber(groupNumber);
        response.setDate(date);
        response.setGroupId(group.getId());

        try {
            String url = String.format(BSUIR_API_URL, groupNumber);
            String jsonResponse = restTemplate.getForObject(url, String.class);
            System.out.println("API Response for group " + groupNumber + ": " + jsonResponse);

            Map<String, Object> jsonMap = objectMapper.readValue(jsonResponse, Map.class);
            Map<String, List<Map<String, Object>>> schedules = (Map<String, List<Map<String, Object>>>) jsonMap.get("schedules");

            if (schedules == null || schedules.isEmpty()) {
                System.out.println("No schedules found in API response for group " + groupNumber);
                response.setSchedules(Collections.emptyList());
                return response;
            }

            LocalDate targetDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            String targetDayOfWeek = DAY_OF_WEEK_MAP.get(targetDate.getDayOfWeek().toString().toLowerCase());
            System.out.println("Target Day of Week: " + targetDayOfWeek + " for date " + date);

            List<ScheduleDto> scheduleDtos = new ArrayList<>();
            List<Map<String, Object>> lessons = schedules.get(targetDayOfWeek);
            if (lessons != null) {
                for (Map<String, Object> lesson : lessons) {
                    String startLessonDate = (String) lesson.get("startLessonDate");
                    String endLessonDate = (String) lesson.get("endLessonDate");
                    String dateLesson = (String) lesson.get("dateLesson");
                    List<Integer> weekNumber = (List<Integer>) lesson.get("weekNumber");

                    if (!isLessonInDateRange(targetDate, startLessonDate, endLessonDate, dateLesson, weekNumber)) {
                        System.out.println("Lesson skipped due to date/week mismatch: " + lesson.get("subjectFullName"));
                        continue;
                    }

                    ScheduleDto scheduleDto = new ScheduleDto();
                    scheduleDto.setSubject((String) lesson.get("subjectFullName"));
                    scheduleDto.setLessonType((String) lesson.get("lessonTypeAbbrev"));
                    scheduleDto.setTime(lesson.get("startLessonTime") + "-" + lesson.get("endLessonTime"));
                    scheduleDto.setAuditorium(getAuditoryValue(lesson));
                    scheduleDto.setGroupId(group.getId());
                    scheduleDtos.add(scheduleDto);
                    System.out.println("Added schedule: " + scheduleDto.getSubject() + " at " + scheduleDto.getTime());
                }
            }

            response.setSchedules(scheduleDtos);
            System.out.println("Found schedules: " + scheduleDtos.size() + " for group " + groupNumber);
        } catch (Exception e) {
            System.out.println("Error processing API response for group " + groupNumber + ": " + e.getMessage());
            response.setSchedules(Collections.emptyList());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> findAll() {
        return scheduleRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduleDto findById(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found with id: " + id));
        return convertToDto(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> findByGroupId(Long groupId) {
        return scheduleRepository.findByGroupId(groupId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleDto create(ScheduleDto scheduleDto) {
        Group group = groupRepository.findById(scheduleDto.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + scheduleDto.getGroupId()));
        Schedule schedule = new Schedule();
        schedule.setSubject(scheduleDto.getSubject());
        schedule.setLessonType(scheduleDto.getLessonType());
        schedule.setTime(scheduleDto.getTime());
        schedule.setAuditorium(scheduleDto.getAuditorium());
        schedule.setGroup(group);
        Schedule savedSchedule = scheduleRepository.save(schedule);
        return convertToDto(savedSchedule);
    }

    @Transactional
    public ScheduleDto update(Long id, ScheduleDto scheduleDto) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found with id: " + id));
        Group group = groupRepository.findById(scheduleDto.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + scheduleDto.getGroupId()));
        schedule.setSubject(scheduleDto.getSubject());
        schedule.setLessonType(scheduleDto.getLessonType());
        schedule.setTime(scheduleDto.getTime());
        schedule.setAuditorium(scheduleDto.getAuditorium());
        schedule.setGroup(group);
        Schedule updatedSchedule = scheduleRepository.save(schedule);
        return convertToDto(updatedSchedule);
    }

    @Transactional
    public void delete(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new EntityNotFoundException("Schedule not found with id: " + id);
        }
        scheduleRepository.deleteById(id);
    }

    private ScheduleDto convertToDto(Schedule schedule) {
        return new ScheduleDto(
                schedule.getId(),
                schedule.getSubject(),
                schedule.getLessonType(),
                schedule.getTime(),
                schedule.getAuditorium(),
                schedule.getGroup().getId()
        );
    }

    private String getAuditoryValue(Map<String, Object> lesson) {
        List<String> auditories = (List<String>) lesson.get("auditories");
        return auditories != null && !auditories.isEmpty() ? auditories.get(0) : "";
    }

    private boolean isLessonInDateRange(LocalDate targetDate, String startLessonDate, String endLessonDate, String dateLesson, List<Integer> weekNumber) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            if (weekNumber != null && !weekNumber.isEmpty()) {
                LocalDate startOfSemester = LocalDate.parse("09.02.2025", formatter);
                long daysSinceStart = startOfSemester.until(targetDate).getDays();
                int targetWeek = (int) (daysSinceStart / 7) + 1;
                if (!weekNumber.contains(targetWeek)) {
                    return false;
                }
            }

            if (dateLesson != null && !dateLesson.isEmpty()) {
                LocalDate lessonDate = LocalDate.parse(dateLesson, formatter);
                return lessonDate.equals(targetDate);
            }

            if (startLessonDate != null && !startLessonDate.isEmpty() && endLessonDate != null && !endLessonDate.isEmpty()) {
                LocalDate start = LocalDate.parse(startLessonDate, formatter);
                LocalDate end = LocalDate.parse(endLessonDate, formatter);
                return !targetDate.isBefore(start) && !targetDate.isAfter(end);
            }

            return true;
        } catch (Exception e) {
            System.out.println("Error parsing dates: " + e.getMessage());
            return true;
        }
    }
}
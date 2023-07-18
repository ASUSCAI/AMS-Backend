package com.ams.restapi.attendance;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ams.restapi.courseInfo.CourseInfo;
import com.ams.restapi.courseInfo.CourseInfoRepository;
import com.ams.restapi.timeConfig.DateSpecificTimeConfig;
import com.ams.restapi.timeConfig.DateSpecificTimeRepository;
import com.ams.restapi.timeConfig.TimeConfig;

import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Attendance Record Management endpoints
 * @author Ryan Woo (rtwoo)
 */
@RestController
class AttendanceController {
    
    private final AttendanceRepository repository;
    private final CourseInfoRepository courseInfo;
    private final DateSpecificTimeRepository dateConfigs;

    AttendanceController(AttendanceRepository repository,
        CourseInfoRepository courseInfo, DateSpecificTimeRepository dateConfigs) {
        this.repository = repository;
        this.courseInfo = courseInfo;
        this.dateConfigs = dateConfigs;
    }

    // Multi-item

    @CrossOrigin(origins = "http://localhost:3000")
    @GetMapping("/attendance")
    ResponseEntity<List<AttendanceRecordDTO>> search(
        @RequestParam("room") Optional<String> room,
        @RequestParam("date") Optional<LocalDate> date,
        @RequestParam("startTime") Optional<LocalTime> startTime,
        @RequestParam("endTime") Optional<LocalTime> endTime,
        @RequestParam("sid") Optional<String> sid,
        @RequestParam("type") Optional<String> type,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sortBy") Optional<String> sortBy,
        @RequestParam("sortType") Optional<String> sortType) {

            Pageable pageable;
            if (sortBy.isPresent())
                pageable = PageRequest.of(page, size,
                    Sort.by(sortType.orElse("asc").equals("desc")
                        ? Direction.DESC : Direction.ASC,
                    sortBy.get()));
            else
                pageable = PageRequest.of(page, size);

            Page<AttendanceRecord> result = repository.search(
                room.orElse(null),
                date.orElse(null),
                startTime.orElse(null),
                endTime.orElse(null),
                sid.orElse(null),
                type.orElse(null),
                pageable
            );

            if (page > result.getTotalPages()) {
                throw new AttendanceLogPageOutofBoundsException(page, size);
            }

            return ResponseEntity.ok()
                .header("totalPages", Integer.toString(result.getTotalPages()))
                .body(result.getContent().stream().map(AttendanceRecordDTO::new)
                    .collect(Collectors.toList()));
    }

    // Single item

    @PostMapping("/attendance")
    AttendanceRecordDTO createSingle(@RequestBody AttendanceRecordDTO newLog) {
        LocalDate rDate;
        LocalTime rTime;

        System.out.println(newLog.getTimestamp());

        try {
            if (newLog.getTimestamp() != null) {    
                LocalDateTime triggerTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(newLog.getTimestamp()),
                    ZoneId.of("MST", ZoneId.SHORT_IDS)); 
                rDate = triggerTime.toLocalDate();
                rTime = triggerTime.toLocalTime();
            } else {
                rDate = LocalDate.parse(newLog.getDate());
                rTime = LocalTime.parse(newLog.getTime());
            }
        } catch(DateTimeException e) {
            e.printStackTrace();
            return null;
        }

        TimeConfig config;
        try {
            config =
                dateConfigs.resolve(newLog.getRoom(), rDate, rTime)
                    .orElse(courseInfo.resolve(newLog.getRoom(),
                        rDate.getDayOfWeek(), rTime).get());
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            return null;
        }
        
        List<AttendanceRecord> previousScans = repository.findByRoomAndDateAndTimeBetweenAndSid(
            newLog.getRoom(), rDate, config.getCourse().getStartTime(), config.getCourse().getEndTime(), newLog.getSid());

        AttendanceRecord.AttendanceType rType;
        if (previousScans.size() == 0) { // ARRIVE
            if (rTime.isAfter(config.getBeginIn().minusMinutes(1L)) &&
                    rTime.isBefore(config.getEndIn().plusMinutes(1L))) {
                rType = AttendanceRecord.AttendanceType.ARRIVED;
            } else if (rTime.isAfter(config.getEndIn()) &&
                    rTime.isBefore(config.getEndLate().plusMinutes(1L))) {
                rType = AttendanceRecord.AttendanceType.ARRIVED_LATE;
            } else {
                rType = AttendanceRecord.AttendanceType.ARRIVED_INVALID;
            }
        } else if (previousScans.size() == 1) { // LEAVE
            if (rTime.isAfter(config.getBeginOut().minusMinutes(1L)) &&
                    rTime.isBefore(config.getEndOut().plusMinutes(1L))) {
                rType = AttendanceRecord.AttendanceType.LEFT;
            } else {
                rType = AttendanceRecord.AttendanceType.LEFT_INVALID;
            }
        } else { // INVALID
            rType = AttendanceRecord.AttendanceType.INVALID;
        }

        AttendanceRecord record = newLog.toEntity(rDate, rTime, rType);
        
        return new AttendanceRecordDTO(repository.save(record));
    }

    @GetMapping("/attendance/{id}")
    AttendanceRecord getSingle(@PathVariable Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new AttendanceLogNotFoundException(id));
    }

    @PutMapping("/attendance/{id}")
    AttendanceRecord updateSingle(@PathVariable Long id, @RequestBody AttendanceRecord newLog) {
        newLog.setId(id);
        return repository.save(newLog);
    }

    @DeleteMapping("/attendance/{id}")
    void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

}

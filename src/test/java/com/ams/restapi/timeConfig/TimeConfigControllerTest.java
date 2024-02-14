package com.ams.restapi.timeConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.ams.restapi.courseInfo.CourseInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class TimeConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TimeConfigController controller;

    @Test
    void contextLoads() throws Exception {
        assertNotNull(controller);
    }

    @Test
    void updateTimeConfig() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalTime startTime = LocalTime.of(8, 30);
        LocalTime endTime = LocalTime.of(9, 45);
        CourseInfo testCourseInfo = new CourseInfo(1234L, 1234L, "CSE 110", "COOR170",
                List.of(DayOfWeek.MONDAY),
                startTime, endTime);

        LocalTime expectedBeginIn = startTime.minusMinutes(CourseInfo.DEFAULT_TOLERANCE);
        LocalTime expectedEndIn = startTime.plusMinutes(CourseInfo.DEFAULT_TOLERANCE);
        LocalTime expectedEndLate = startTime.plusMinutes(CourseInfo.DEFAULT_LATE_TOLERANCE);
        LocalTime expectedBeginOut = endTime.minusMinutes(CourseInfo.DEFAULT_TOLERANCE);
        LocalTime expectedEndOut = endTime.plusMinutes(CourseInfo.DEFAULT_TOLERANCE);

        mockMvc.perform(put("/timeConfig/1234")
                .contentType("application/json")
                .content(mapper.writeValueAsString(testCourseInfo)))
                .andExpect(status().isOk()).andDo(print());

        mockMvc.perform(get("/timeConfig/1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.beginIn").value(mapper.writeValueAsString(expectedBeginIn)))
                .andExpect(jsonPath("$.endIn").value(mapper.writeValueAsString(expectedEndIn)))
                .andExpect(jsonPath("$.endLate").value(mapper.writeValueAsString(expectedEndLate)))
                .andExpect(jsonPath("$.beginOut").value(mapper.writeValueAsString(expectedBeginOut)))
                .andExpect(jsonPath("$.endOut").value(mapper.writeValueAsString(expectedEndOut)))
                .andDo(print());
    }
}

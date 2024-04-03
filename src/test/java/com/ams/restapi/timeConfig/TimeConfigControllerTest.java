package com.ams.restapi.timeConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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

    private String unwrapQuotes(String str) {
        if (str.indexOf("\"") == 0 && str.lastIndexOf("\"") == str.length() - 1)
            return str.substring(1, str.length() - 1);
        else
            throw new IllegalArgumentException("Not wrapped in double quotes");
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void updateTimeConfig() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        LocalTime startTime = LocalTime.of(8, 30);
        LocalTime endTime = LocalTime.of(9, 45);
        CourseInfo testCourseInfo = new CourseInfo(1234L, 1234L, "CSE 110", "COOR170",
                List.of(DayOfWeek.MONDAY),
                startTime, endTime);


        mockMvc.perform(put("/courseInfo/1234").with(csrf())
                .contentType("application/json")
                .content(mapper.writeValueAsString(testCourseInfo)))
                .andExpect(status().isOk()).andDo(print());

        TimeConfig updatedTimeConfig = new TimeConfig(
                testCourseInfo,
                LocalTime.of(7, 10),
                LocalTime.of(7, 20),
                LocalTime.of(7, 30),
                LocalTime.of(8, 0),
                LocalTime.of(8, 20));


        mockMvc.perform(put("/timeConfig/1234").with(csrf())
                .contentType("application/json")
                .content(mapper.writeValueAsString(updatedTimeConfig)))
                .andExpect(status().isOk()).andDo(print());

        mockMvc.perform(get("/timeConfig/1234"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.beginIn")
                        .value(unwrapQuotes(mapper.writeValueAsString(updatedTimeConfig.getBeginIn()))))
                .andExpect(jsonPath("$.endIn")
                        .value(unwrapQuotes(mapper.writeValueAsString(updatedTimeConfig.getEndIn()))))
                .andExpect(jsonPath("$.endLate")
                        .value(unwrapQuotes(mapper.writeValueAsString(updatedTimeConfig.getEndLate()))))
                .andExpect(jsonPath("$.beginOut")
                        .value(unwrapQuotes(mapper.writeValueAsString(updatedTimeConfig.getBeginOut()))))
                .andExpect(jsonPath("$.endOut")
                        .value(unwrapQuotes(mapper.writeValueAsString(updatedTimeConfig.getEndOut()))))
                .andDo(print());

    }
}
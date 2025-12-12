package com.example.appointments.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

@ConfigurationProperties(prefix = "appointments")
public class AppointmentsProperties {

    /**
     * Working day start time (local), e.g., 09:00
     */
    private LocalTime workingHoursStart = LocalTime.of(9, 0);

    /**
     * Working day end time (local), e.g., 17:00
     */
    private LocalTime workingHoursEnd = LocalTime.of(17, 0);

    public LocalTime getWorkingHoursStart() {
        return workingHoursStart;
    }

    public void setWorkingHoursStart(LocalTime workingHoursStart) {
        this.workingHoursStart = workingHoursStart;
    }

    public LocalTime getWorkingHoursEnd() {
        return workingHoursEnd;
    }

    public void setWorkingHoursEnd(LocalTime workingHoursEnd) {
        this.workingHoursEnd = workingHoursEnd;
    }
}

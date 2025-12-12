package com.example.appointments.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreateAppointmentRequest {

    @NotNull
    private Long doctorId;

    @NotBlank
    private String patientName;

    @NotNull
    @Future
    private LocalDateTime startTime;

    public CreateAppointmentRequest() {
    }

    public CreateAppointmentRequest(Long doctorId, String patientName, LocalDateTime startTime) {
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.startTime = startTime;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getPatientName() {
        return patientName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
}

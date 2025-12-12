package com.example.appointments.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateDoctorRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String specialization;

    @NotBlank
    @Email
    private String contactEmail;

    @NotNull
    @Min(5)
    private Integer perSlotDurationMinutes;

    public CreateDoctorRequest() {
    }

    public CreateDoctorRequest(String name, String specialization, String contactEmail, Integer perSlotDurationMinutes) {
        this.name = name;
        this.specialization = specialization;
        this.contactEmail = contactEmail;
        this.perSlotDurationMinutes = perSlotDurationMinutes;
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public Integer getPerSlotDurationMinutes() {
        return perSlotDurationMinutes;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public void setPerSlotDurationMinutes(Integer perSlotDurationMinutes) {
        this.perSlotDurationMinutes = perSlotDurationMinutes;
    }
}

package com.example.appointments.dto;

public class DoctorResponse {
    private Long id;
    private String name;
    private String specialization;
    private String contactEmail;
    private Integer perSlotDurationMinutes;

    public DoctorResponse() {
    }

    public DoctorResponse(Long id, String name, String specialization, String contactEmail, Integer perSlotDurationMinutes) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.contactEmail = contactEmail;
        this.perSlotDurationMinutes = perSlotDurationMinutes;
    }

    public Long getId() {
        return id;
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

    public void setId(Long id) {
        this.id = id;
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

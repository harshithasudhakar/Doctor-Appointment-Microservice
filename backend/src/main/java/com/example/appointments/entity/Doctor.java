package com.example.appointments.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String specialization;

    @NotBlank
    @Email
    @Column(name = "contact_email", nullable = false, unique = true)
    private String contactEmail;

    @NotNull
    @Min(5)
    @Column(name = "per_slot_duration", nullable = false)
    private Integer perSlotDurationMinutes;

    public Doctor() {
    }

    public Doctor(String name, String specialization, String contactEmail, Integer perSlotDurationMinutes) {
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

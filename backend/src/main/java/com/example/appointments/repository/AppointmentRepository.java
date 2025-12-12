package com.example.appointments.repository;

import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorAndStatusAndStartTimeBetween(
        Doctor doctor,
        AppointmentStatus status,
        LocalDateTime startInclusive,
        LocalDateTime endInclusive
    );

    boolean existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
        Doctor doctor,
        AppointmentStatus status,
        LocalDateTime endExclusive,
        LocalDateTime startExclusive
    );

    Optional<Appointment> findByDoctorAndStartTime(Doctor doctor, LocalDateTime startTime);

    List<Appointment> findByDoctorAndStatusAndEndTimeGreaterThanAndStartTimeLessThan(
        Doctor doctor,
        AppointmentStatus status,
        LocalDateTime startExclusive,
        LocalDateTime endExclusive
    );
}

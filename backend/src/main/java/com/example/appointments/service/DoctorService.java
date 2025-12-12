package com.example.appointments.service;

import com.example.appointments.config.AppointmentsProperties;
import com.example.appointments.dto.CreateDoctorRequest;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import com.example.appointments.exception.ResourceNotFoundException;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentsProperties properties;

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         AppointmentsProperties properties) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.properties = properties;
    }

    @Transactional
    public Doctor createDoctor(CreateDoctorRequest req) {
        Doctor doctor = new Doctor(req.getName(), req.getSpecialization(), req.getContactEmail(), req.getPerSlotDurationMinutes());
        return doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public List<Doctor> listDoctors(String specialization) {
        if (specialization == null || specialization.isBlank()) {
            return doctorRepository.findAll();
        }
        return doctorRepository.findBySpecializationIgnoreCase(specialization);
    }

    @Transactional(readOnly = true)
    public Doctor getByIdOrThrow(Long id) {
        return doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }

    /**
     * Compute available start-times (ISO-8601 strings) for a doctor on a given date.
     * Uses configured working hours and the doctor's perSlotDurationMinutes.
     * Excludes any slot that overlaps with an existing CONFIRMED appointment.
     */
    @Transactional(readOnly = true)
    public List<String> getAvailability(Long doctorId, LocalDate date) {
        Doctor doctor = getByIdOrThrow(doctorId);

        LocalTime startTime = properties.getWorkingHoursStart();
        LocalTime endTime = properties.getWorkingHoursEnd();

        LocalDateTime dayStart = date.atTime(startTime);
        LocalDateTime dayEnd = date.atTime(endTime);

        // Load all appointments overlapping the working window
        List<Appointment> busy = appointmentRepository
            .findByDoctorAndStatusAndEndTimeGreaterAndStartTimeLess(
                doctor,
                AppointmentStatus.CONFIRMED,
                dayStart,
                dayEnd
            );

        List<String> available = new ArrayList<>();
        int slotMinutes = doctor.getPerSlotDurationMinutes();
        LocalDateTime cursor = dayStart;

        while (!cursor.plusMinutes(slotMinutes).isAfter(dayEnd)) {
            // Use effectively-final locals inside the lambda to satisfy Java requirements
            final LocalDateTime slotStart = cursor;
            final LocalDateTime slotEnd = slotStart.plusMinutes(slotMinutes);

            boolean conflicts = busy.stream().anyMatch(a ->
                a.getStartTime().isBefore(slotEnd) && a.getEndTime().isAfter(slotStart)
            );
            if (!conflicts) {
                available.add(slotStart.toString()); // ISO-8601 (e.g., 2025-12-31T10:00)
            }
            cursor = cursor.plusMinutes(slotMinutes);
        }

        return available;
    }
}

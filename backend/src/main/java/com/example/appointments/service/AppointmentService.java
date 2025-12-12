package com.example.appointments.service;

import com.example.appointments.dto.CreateAppointmentRequest;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import com.example.appointments.exception.ResourceNotFoundException;
import com.example.appointments.exception.SlotAlreadyBookedException;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Books an appointment with optimistic locking and unique slot constraint.
     * Strategy:
     * - Enforce unique (doctor_id, start_time) at DB level to prevent duplicate inserts for the same slot.
     * - If a slot row exists and is CANCELLED, update it to CONFIRMED; concurrent updates will trigger optimistic locking via @Version.
     * - If no row exists, insert a new CONFIRMED appointment; concurrent inserts will throw DataIntegrityViolationException, translated to 409.
     */
    @Transactional
    public Appointment book(CreateAppointmentRequest req) {
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + req.getDoctorId()));

        int minutes = doctor.getPerSlotDurationMinutes();
        LocalDateTime start = req.getStartTime();
        LocalDateTime end = start.plusMinutes(minutes);

        // Optional overlap check for non-slot-aligned requests
        boolean overlapExists = appointmentRepository.existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreater(
                doctor,
                AppointmentStatus.CONFIRMED,
                end,
                start
        );
        if (overlapExists) {
            throw new SlotAlreadyBookedException("Requested slot is already booked.");
        }

        // Try to reuse existing slot row (optimistic locking applies on update)
        return appointmentRepository.findByDoctorAndStartTime(doctor, start)
                .map(existing -> {
                    if (existing.getStatus() == AppointmentStatus.CONFIRMED) {
                        throw new SlotAlreadyBookedException("Requested slot is already booked.");
                    }
                    existing.setStatus(AppointmentStatus.CONFIRMED);
                    existing.setPatientName(req.getPatientName());
                    existing.setEndTime(end);
                    try {
                        return appointmentRepository.save(existing);
                    } catch (DataIntegrityViolationException e) {
                        // Concurrent update/constraint issue
                        throw new SlotAlreadyBookedException("Slot has been booked by another user.");
                    }
                })
                .orElseGet(() -> {
                    try {
                        Appointment appt = new Appointment(doctor, req.getPatientName(), start, end);
                        return appointmentRepository.save(appt);
                    } catch (DataIntegrityViolationException e) {
                        // Another concurrent request likely inserted the same (doctor_id, start_time)
                        throw new SlotAlreadyBookedException("Slot has been booked by another user.");
                    }
                });
    }

    @Transactional
    public Appointment cancel(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        appt.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appt);
    }
}

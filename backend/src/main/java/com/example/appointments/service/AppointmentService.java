package com.example.appointments.service;

import com.example.appointments.dto.CreateAppointmentRequest;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import com.example.appointments.exception.ResourceNotFoundException;
import com.example.appointments.exception.SlotAlreadyBookedException;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
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

    @Transactional
    public Appointment book(CreateAppointmentRequest req) {
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + req.getDoctorId()));

        int minutes = doctor.getPerSlotDurationMinutes();
        LocalDateTime start = req.getStartTime();
        LocalDateTime end = start.plusMinutes(minutes);

        boolean overlapExists = appointmentRepository.existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreater(
                doctor,
                AppointmentStatus.CONFIRMED,
                end,
                start
        );

        if (overlapExists) {
            throw new SlotAlreadyBookedException("Requested slot is already booked.");
        }

        Appointment appt = new Appointment(doctor, req.getPatientName(), start, end);
        return appointmentRepository.save(appt);
    }

    @Transactional
    public Appointment cancel(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
        appt.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appt);
    }
}

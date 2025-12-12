package com.example.appointments.controller;

import com.example.appointments.dto.AppointmentResponse;
import com.example.appointments.dto.CreateAppointmentRequest;
import com.example.appointments.entity.Appointment;
import com.example.appointments.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody CreateAppointmentRequest req) {
        Appointment saved = appointmentService.book(req);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable("id") Long id) {
        Appointment saved = appointmentService.cancel(id);
        return ResponseEntity.ok(toResponse(saved));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getId(),
                a.getDoctor().getId(),
                a.getPatientName(),
                a.getStartTime(),
                a.getEndTime(),
                a.getStatus()
        );
    }
}

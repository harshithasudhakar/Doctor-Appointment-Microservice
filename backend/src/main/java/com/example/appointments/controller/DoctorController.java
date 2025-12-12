package com.example.appointments.controller;

import com.example.appointments.dto.CreateDoctorRequest;
import com.example.appointments.dto.DoctorResponse;
import com.example.appointments.entity.Doctor;
import com.example.appointments.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest req) {
        Doctor saved = doctorService.createDoctor(req);
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> listDoctors(@RequestParam(name = "specialization", required = false) String specialization) {
        List<DoctorResponse> res = doctorService.listDoctors(specialization).stream().map(this::toResponse).toList();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<String>> availability(
            @PathVariable("id") Long doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<String> slots = doctorService.getAvailability(doctorId, date);
        return ResponseEntity.ok(slots);
    }

    private DoctorResponse toResponse(Doctor d) {
        return new DoctorResponse(d.getId(), d.getName(), d.getSpecialization(), d.getContactEmail(), d.getPerSlotDurationMinutes());
    }
}

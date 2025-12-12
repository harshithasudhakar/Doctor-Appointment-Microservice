package com.example.appointments.service;

import com.example.appointments.config.AppointmentsProperties;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceAvailabilityTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private DoctorService doctorService;

    private Doctor doctor;
    private AppointmentsProperties props;

    @BeforeEach
    void setUp() {
        props = new AppointmentsProperties();
        props.setWorkingHoursStart(LocalTime.of(9, 0));
        props.setWorkingHoursEnd(LocalTime.of(12, 0));

        doctorService = new DoctorService(doctorRepository, appointmentRepository, props);

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Availability");
        doctor.setSpecialization("General");
        doctor.setContactEmail("avail@example.com");
        doctor.setPerSlotDurationMinutes(30);
    }

    @Test
    void availability_excludesBusySlots_andReturnsISOStartTimes() {
        LocalDate date = LocalDate.of(2025, 12, 31);
        LocalDateTime dayStart = date.atTime(9, 0);
        LocalDateTime dayEnd = date.atTime(12, 0);

        // Busy appointments at 10:00-10:30 and 11:00-11:30
        Appointment a1 = new Appointment(doctor, "John", dayStart.plusHours(1), dayStart.plusHours(1).plusMinutes(30));
        a1.setStatus(AppointmentStatus.CONFIRMED);

        Appointment a2 = new Appointment(doctor, "Jane", dayStart.plusHours(2), dayStart.plusHours(2).plusMinutes(30));
        a2.setStatus(AppointmentStatus.CONFIRMED);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorAndStatusAndEndTimeGreaterAndStartTimeLess(
                eq(doctor), eq(AppointmentStatus.CONFIRMED), eq(dayStart), eq(dayEnd)
        )).thenReturn(List.of(a1, a2));

        List<String> slots = doctorService.getAvailability(1L, date);

        // Expected available starts: 09:00, 09:30, 10:30, 11:30
        assertTrue(slots.contains(dayStart.toString()));
        assertTrue(slots.contains(dayStart.plusMinutes(30).toString()));
        assertTrue(slots.contains(dayStart.plusMinutes(90).toString()) == false); // 10:30 present? check explicitly below
        assertTrue(slots.contains(dayStart.plusHours(1).toString()) == false); // 10:00 blocked
        assertTrue(slots.contains(dayStart.plusHours(2).toString()) == false); // 11:00 blocked
        assertTrue(slots.contains(dayStart.plusHours(1).plusMinutes(30).toString())); // 10:30 available
        assertTrue(slots.contains(dayStart.plusHours(2).plusMinutes(30).toString())); // 11:30 available

        // Ensure total count equals 4 (09:00, 09:30, 10:30, 11:30)
        assertEquals(4, slots.size());
    }

    @Test
    void availability_noBusySlots_returnsAll() {
        LocalDate date = LocalDate.of(2025, 6, 1);
        LocalDateTime dayStart = date.atTime(9, 0);
        LocalDateTime dayEnd = date.atTime(12, 0);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByDoctorAndStatusAndEndTimeGreaterAndStartTimeLess(
                eq(doctor), eq(AppointmentStatus.CONFIRMED), eq(dayStart), eq(dayEnd)
        )).thenReturn(List.of());

        List<String> slots = doctorService.getAvailability(1L, date);

        // For 9-12 with 30 min slots: 09:00, 09:30, 10:00, 10:30, 11:00, 11:30
        assertEquals(6, slots.size());
        assertEquals(List.of(
                dayStart.toString(),
                dayStart.plusMinutes(30).toString(),
                dayStart.plusHours(1).toString(),
                dayStart.plusHours(1).plusMinutes(30).toString(),
                dayStart.plusHours(2).toString(),
                dayStart.plusHours(2).plusMinutes(30).toString()
        ), slots);
    }
}

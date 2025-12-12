package com.example.appointments.service;

import com.example.appointments.dto.CreateAppointmentRequest;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import com.example.appointments.exception.ResourceNotFoundException;
import com.example.appointments.exception.SlotAlreadyBookedException;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private AppointmentService appointmentService;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, doctorRepository);
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setName("Dr. Smith");
        doctor.setSpecialization("Cardiology");
        doctor.setContactEmail("drsmith@example.com");
        doctor.setPerSlotDurationMinutes(30);
    }

    @Test
    void book_success_createsAppointment() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateAppointmentRequest req = new CreateAppointmentRequest(1L, "John Doe", start);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(doctor), eq(AppointmentStatus.CONFIRMED), eq(start.plusMinutes(30)), eq(start)
        )).thenReturn(false);
        when(appointmentRepository.findByDoctorAndStartTime(eq(doctor), eq(start))).thenReturn(Optional.empty());

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        when(appointmentRepository.save(captor.capture())).thenAnswer(invocation -> {
            Appointment saved = captor.getValue();
            saved.setId(100L);
            return saved;
        });

        Appointment result = appointmentService.book(req);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("John Doe", result.getPatientName());
        assertEquals(start.plusMinutes(30), result.getEndTime());
        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
    }

    @Test
    void book_overlap_throwsSlotAlreadyBooked() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateAppointmentRequest req = new CreateAppointmentRequest(1L, "John Doe", start);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(doctor), eq(AppointmentStatus.CONFIRMED), eq(start.plusMinutes(30)), eq(start)
        )).thenReturn(true);

        assertThrows(SlotAlreadyBookedException.class, () -> appointmentService.book(req));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void book_concurrentInsert_throwsSlotAlreadyBooked() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0);
        CreateAppointmentRequest req = new CreateAppointmentRequest(1L, "Jane Doe", start);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(doctor), eq(AppointmentStatus.CONFIRMED), eq(start.plusMinutes(30)), eq(start)
        )).thenReturn(false);
        when(appointmentRepository.findByDoctorAndStartTime(eq(doctor), eq(start))).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        SlotAlreadyBookedException ex = assertThrows(SlotAlreadyBookedException.class, () -> appointmentService.book(req));
        assertTrue(ex.getMessage().toLowerCase().contains("slot"));
    }

    @Test
    void book_reuseCancelledSlot_updatesToConfirmed() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        CreateAppointmentRequest req = new CreateAppointmentRequest(1L, "Alice", start);

        Appointment existing = new Appointment(doctor, "Old Patient", start, start.plusMinutes(30));
        existing.setStatus(AppointmentStatus.CANCELLED);
        existing.setId(200L);

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsByDoctorAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(doctor), eq(AppointmentStatus.CONFIRMED), eq(start.plusMinutes(30)), eq(start)
        )).thenReturn(false);
        when(appointmentRepository.findByDoctorAndStartTime(eq(doctor), eq(start))).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.book(req);
        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        assertEquals("Alice", result.getPatientName());
        assertEquals(200L, result.getId());
    }

    @Test
    void cancel_updatesStatusToCancelled() {
        Appointment appt = new Appointment(doctor, "Bob", LocalDateTime.now().plusDays(1).withHour(14).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(1).withHour(14).withMinute(30).withSecond(0).withNano(0));
        appt.setId(300L);
        appt.setStatus(AppointmentStatus.CONFIRMED);

        when(appointmentRepository.findById(300L)).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(appt)).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result = appointmentService.cancel(300L);
        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
    }

    @Test
    void book_doctorNotFound_throwsResourceNotFound() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        CreateAppointmentRequest req = new CreateAppointmentRequest(99L, "Ghost", start);

        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.book(req));
    }
}

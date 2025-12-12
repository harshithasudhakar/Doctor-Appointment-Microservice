package com.example.appointments.service;

import com.example.appointments.config.AppointmentsProperties;
import com.example.appointments.dto.CreateDoctorRequest;
import com.example.appointments.entity.Doctor;
import com.example.appointments.exception.ResourceNotFoundException;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    private DoctorService doctorService;

    @BeforeEach
    void setUp() {
        AppointmentsProperties props = new AppointmentsProperties();
        // Defaults are fine; set explicitly to ensure deterministic tests
        props.setWorkingHoursStart(LocalTime.of(9, 0));
        props.setWorkingHoursEnd(LocalTime.of(17, 0));
        doctorService = new DoctorService(doctorRepository, appointmentRepository, props);
    }

    @Test
    void createDoctor_savesAndReturns() {
        CreateDoctorRequest req = new CreateDoctorRequest("Dr. Who", "Timeology", "drwho@example.com", 20);

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        when(doctorRepository.save(captor.capture())).thenAnswer(invocation -> {
            Doctor saved = captor.getValue();
            saved.setId(1L);
            return saved;
        });

        Doctor result = doctorService.createDoctor(req);
        assertNotNull(result.getId());
        assertEquals("Dr. Who", result.getName());
        assertEquals("Timeology", result.getSpecialization());
        assertEquals("drwho@example.com", result.getContactEmail());
        assertEquals(20, result.getPerSlotDurationMinutes());
    }

    @Test
    void listDoctors_noFilter_returnsAll() {
        when(doctorRepository.findAll()).thenReturn(List.of(new Doctor(), new Doctor()));
        List<Doctor> result = doctorService.listDoctors(null);
        assertEquals(2, result.size());
    }

    @Test
    void listDoctors_withFilter_usesRepositoryFilter() {
        when(doctorRepository.findBySpecializationIgnoreCase("Cardiology")).thenReturn(List.of(new Doctor()));
        List<Doctor> result = doctorService.listDoctors("Cardiology");
        assertEquals(1, result.size());
    }

    @Test
    void getByIdOrThrow_notFound_throws() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> doctorService.getByIdOrThrow(99L));
    }

    @Test
    void getByIdOrThrow_found_returns() {
        Doctor d = new Doctor();
        d.setId(2L);
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(d));
        Doctor result = doctorService.getByIdOrThrow(2L);
        assertEquals(2L, result.getId());
    }
}

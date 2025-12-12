package com.example.appointments.integration;

import com.example.appointments.dto.CreateAppointmentRequest;
import com.example.appointments.entity.Appointment;
import com.example.appointments.entity.AppointmentStatus;
import com.example.appointments.entity.Doctor;
import com.example.appointments.exception.SlotAlreadyBookedException;
import com.example.appointments.repository.AppointmentRepository;
import com.example.appointments.repository.DoctorRepository;
import com.example.appointments.service.AppointmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using Testcontainers Postgres to demonstrate concurrent booking attempts
 * resulting in exactly one success and one conflict (HTTP 409 equivalent).
 */
@Testcontainers
@SpringBootTest
@ExtendWith(org.testcontainers.junit.jupiter.TestcontainersExtension.class)
class ConcurrencyIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("appointmentsdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // For integration test, let Hibernate update/create tables and ensure Flyway targets Postgres migrations
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration/postgres");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @AfterEach
    void cleanup() {
        appointmentRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Test
    @Timeout(20)
    void concurrentBooking_sameSlot_onlyOneSucceeds_otherConflicts() throws InterruptedException {
        Doctor doctor = new Doctor("Dr. Concurrency", "General", "concurrency@example.com", 30);
        doctor = doctorRepository.save(doctor);

        LocalDateTime slotStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateAppointmentRequest req1 = new CreateAppointmentRequest(doctor.getId(), "Alice", slotStart);
        CreateAppointmentRequest req2 = new CreateAppointmentRequest(doctor.getId(), "Bob", slotStart);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        List<Object> results = new ArrayList<>();

        Runnable task1 = () -> {
            ready.countDown();
            try {
                startGate.await(10, TimeUnit.SECONDS);
                Appointment a = appointmentService.book(req1);
                results.add(a);
            } catch (SlotAlreadyBookedException e) {
                results.add(e);
            } catch (Exception e) {
                results.add(e);
            }
        };

        Runnable task2 = () -> {
            ready.countDown();
            try {
                startGate.await(10, TimeUnit.SECONDS);
                Appointment a = appointmentService.book(req2);
                results.add(a);
            } catch (SlotAlreadyBookedException e) {
                results.add(e);
            } catch (Exception e) {
                results.add(e);
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        startGate.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        long successCount = results.stream().filter(r -> r instanceof Appointment).count();
        long conflictCount = results.stream().filter(r -> r instanceof SlotAlreadyBookedException).count();

        assertEquals(1, successCount, "Exactly one booking should succeed");
        assertEquals(1, conflictCount, "Exactly one booking should conflict");

        List<Appointment> all = appointmentRepository.findAll();
        assertEquals(1, all.size());
        Appointment saved = all.get(0);
        assertEquals(doctor.getId(), saved.getDoctor().getId());
        assertEquals(slotStart, saved.getStartTime());
        assertEquals(AppointmentStatus.CONFIRMED, saved.getStatus());
    }
}

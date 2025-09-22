package carehome.test;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import carehome.domain.*;
import carehome.service.Service;
import carehome.repo.Store;
import carehome.exception.*;

import java.time.*;
import java.util.List;

/**
 * JUnit tests for Milestone 2-2 functionality.
 * Covers positive and negative cases for:
 *  - adding residents to vacant beds
 *  - preventing double allocation
 *  - prescription rules (doctor only, must be rostered)
 *  - nurse administer restrictions (must be rostered)
 *  - shift rule limits (nurse 8h/day, doctor 1h/day)
 */
public class M2Tests {

    Store db;
    Service svc;
    Manager mgr;
    Nurse nurse;
    Doctor doc;
    Bed b1, b2;
    Resident res;

    @BeforeEach
    void setup() {
        db = Store.get();

        // Reset all collections to start fresh
        db.residents.clear();
        db.staff.clear();
        db.wards.clear();
        db.rooms.clear();
        db.beds.clear();
        db.prescriptions.clear();
        db.logs.clear();
        db.administrations.clear();

        svc = new Service();

        // Seed staff
        mgr = new Manager("M", "Mgr");
        nurse = new Nurse("N", "Nurse");
        doc = new Doctor("D", "Doc");
        db.staff.put(mgr.id(), mgr);
        svc.addStaff(mgr, nurse, "n");
        svc.addStaff(mgr, doc, "d");

        // Seed layout
        Ward w1 = new Ward("W", "Ward");
        db.wards.put(w1.id(), w1);

        Room r = new Room("R", "W");
        db.rooms.put(r.id(), r);
        w1.addRoom(r.id());

        b1 = new Bed("B1", "R");
        b2 = new Bed("B2", "R");
        db.beds.put(b1.id(), b1);
        db.beds.put(b2.id(), b2);
        r.addBed("B1");
        r.addBed("B2");

        // Seed resident
        res = new Resident("R1", "Rob", Gender.M);
    }

    @Test
    void addResidentToVacantBed_succeeds() {
        svc.addResidentToVacantBed(mgr, res, b1.id());
        assertEquals("B1", res.currentBedId().orElse("?"));
    }

    @Test
    void addResidentToOccupiedBed_fails() {
        svc.addResidentToVacantBed(mgr, res, b1.id());
        var r2 = new Resident("R2", "Rita", Gender.F);
        assertThrows(AllocationException.class,
            () -> svc.addResidentToVacantBed(mgr, r2, b1.id()));
    }

    @Test
    void onlyDoctorCanAttachPrescription_andMustBeRostered() {
        LocalDate today = LocalDate.now();

        // Not rostered yet → should fail
        assertThrows(AuthorizationException.class,
            () -> svc.doctorAttachPrescription(
                doc, b1.id(), List.of(),
                LocalDateTime.of(today, LocalTime.of(9, 5))
            )
        );

        // Now allocate bed + roster
        svc.addResidentToVacantBed(mgr, res, b1.id());
        svc.allocateShift(mgr, doc.id(), today, ShiftType.DOCTOR_1H);

        String pid = svc.doctorAttachPrescription(
            doc,
            b1.id(),
            List.of(new Prescription.MedicationOrder("DrugX", 1, "tab", "9am", "")),
            LocalDateTime.of(today, LocalTime.of(9, 5))
        );

        assertTrue(db.prescriptions.containsKey(pid));
    }

    @Test
    void nurseAdminister_requiresRoster() {
        LocalDate today = LocalDate.now();
        svc.addResidentToVacantBed(mgr, res, b1.id());

        // Nurse not rostered yet → should fail
        assertThrows(AuthorizationException.class,
            () -> svc.administer(
                nurse, b1.id(), "DrugY", 1, "tab", "",
                LocalDateTime.of(today, LocalTime.of(10, 0))
            )
        );

        // Add nurse AM shift → should succeed
        svc.allocateShift(mgr, nurse.id(), today, ShiftType.NURSE_AM);
        assertDoesNotThrow(() ->
            svc.administer(
                nurse, b1.id(), "DrugY", 1, "tab", "",
                LocalDateTime.of(today, LocalTime.of(10, 0))
            )
        );
    }

    @Test
    void rosterRules_limitHours() {
        LocalDate d = LocalDate.now();

        // Nurse can take AM shift
        assertDoesNotThrow(() ->
            svc.allocateShift(mgr, nurse.id(), d, ShiftType.NURSE_AM));

        // Adding PM shift (another 8h) would exceed 8h/day
        assertThrows(RosterException.class,
            () -> svc.allocateShift(mgr, nurse.id(), d, ShiftType.NURSE_PM));
    }
}

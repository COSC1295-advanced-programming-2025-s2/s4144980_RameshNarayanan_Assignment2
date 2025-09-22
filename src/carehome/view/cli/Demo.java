package carehome.view.cli;

import carehome.repo.Store;
import carehome.domain.*;
import carehome.service.Service;
import carehome.domain.Prescription.MedicationOrder;

import java.time.*;
import java.util.List;

/**
 * Scripted console demo for Milestone 2-2.
 * 
 * Demonstrates:
 *  - Adding staff and allocating shifts
 *  - Adding a resident to a vacant bed
 *  - Doctor attaching a prescription
 *  - Nurse administering medication
 *  - Nurse moving a resident to another bed
 *  - Logging all actions with timestamp + staff ID
 */
public class Demo {

	public static void main(String[] args) {
	    // Persist across runs
	    Store db = Store.get();
	    Runtime.getRuntime().addShutdownHook(new Thread(db::save));

	    // --- reset state for a clean demo run ---
	    db.residents.clear();
	    db.staff.clear();
	    db.wards.clear();
	    db.rooms.clear();
	    db.beds.clear();
	    db.prescriptions.clear();
	    db.administrations.clear();
	    db.logs.clear();

        Service svc = new Service();

        // ----- Seed layout (ward/room/beds) -----
        Ward w1 = new Ward("W1", "Ward 1");
        db.wards.put(w1.id(), w1);

        Room r1 = new Room("W1-R1", "W1");
        db.rooms.put(r1.id(), r1);
        w1.addRoom(r1.id());

        Bed b1 = new Bed("W1-R1-B1", "W1-R1");
        Bed b2 = new Bed("W1-R1-B2", "W1-R1");
        db.beds.put(b1.id(), b1);
        db.beds.put(b2.id(), b2);
        r1.addBed(b1.id());
        r1.addBed(b2.id());

        // ----- Staff setup -----
        Manager mgr = new Manager("M-1", "Alice Manager");
        db.staff.put(mgr.id(), mgr);
        mgr.setPassword("admin");

        Nurse nurse = new Nurse("N-1", "Nina Nurse");
        Doctor doc  = new Doctor("D-1", "Dan Doctor");

        svc.addStaff(mgr, nurse, "nurse");
        svc.addStaff(mgr, doc,   "doctor");

        // ----- Shifts for today -----
        LocalDate today = LocalDate.now();
        svc.allocateShift(mgr, nurse.id(), today, ShiftType.NURSE_AM);
        svc.allocateShift(mgr, doc.id(),   today, ShiftType.DOCTOR_1H);

        // ----- Add resident -----
        Resident res = new Resident("R-1", "Bob Resident", Gender.M);
        svc.addResidentToVacantBed(mgr, res, b1.id());

        // ----- Doctor attaches prescription (09:05) -----
        LocalDateTime nineOhFive = LocalDateTime.of(today, LocalTime.of(9, 5));
        svc.doctorAttachPrescription(
            doc,
            b1.id(),
            List.of(new MedicationOrder("Amoxicillin", 500, "mg", "8am, 8pm", "after food")),
            nineOhFive
        );

        // ----- Nurse administers at 10:30 (AM shift) -----
        LocalDateTime tenThirty = LocalDateTime.of(today, LocalTime.of(10, 30));
        svc.administer(nurse, b1.id(), "Amoxicillin", 500, "mg", "first dose", tenThirty);

        // ----- Nurse moves resident at 14:30 (PM shift) -----
        LocalDateTime twoThirty = LocalDateTime.of(today, LocalTime.of(14, 30));
        svc.moveResident(nurse, b1.id(), b2.id(), twoThirty);

        // ----- Verify resident location -----
        Resident rr = svc.checkResidentDetails(mgr, b2.id());
        System.out.println("Resident now in " + b2.id() + ": " + rr.name());

        // ----- Print action logs -----
        System.out.println("\n--- Action Logs ---");
        db.logs.forEach(l ->
            System.out.println(l.when() + " | " + l.staffId() + " | " + l.type() + " | " + l.details())
        );
    }
}

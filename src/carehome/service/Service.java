package carehome.service;

import carehome.domain.*;
import carehome.exception.*;
import carehome.repo.Store;

import java.time.*;
import java.util.*;

/**
 * Business logic façade for the system.
 * Contains methods for staff management, shift allocation,
 * resident/bed management, prescriptions, and administration.
 */
public class Service {

    private final Store db = Store.get();

    // ---- Logging & lookup helpers ------------------------------------------------

    private void log(String staffId, String type, String details) {
        db.logs.add(new ActionLog(Instant.now(), staffId, type, details));
    }

    private <T> T get(Map<String, T> map, String id, String what) {
        return Optional.ofNullable(map.get(id))
                       .orElseThrow(() -> new NotFoundException(what + " not found: " + id));
    }

    // ---- Staff management --------------------------------------------------------

    public void addStaff(Manager manager, Staff newStaff, String password) {
        requireManager(manager);
        newStaff.setPassword(password);
        db.staff.put(newStaff.id(), newStaff);
        log(manager.id(), "ADD_STAFF", newStaff.toString());
    }

    public void modifyStaffPassword(Manager manager, String staffId, String newPassword) {
        requireManager(manager);
        Staff s = get(db.staff, staffId, "Staff");
        s.setPassword(newPassword);
        log(manager.id(), "MODIFY_STAFF_PWD", s.toString());
    }

    // ---- Shifts -----------------------------------------------------------------

    public void allocateShift(Manager manager, String staffId, LocalDate date, ShiftType type) {
        requireManager(manager);
        Staff s = get(db.staff, staffId, "Staff");
        s.assignShift(date, type);

        // Enforce per-day hour limits
        int hours = s.hoursOn(date);
        if (s.role() == Role.NURSE && hours > 8) {
            s.removeShift(date, type);
            throw new RosterException("Nurse exceeds 8h on " + date);
        }
        if (s.role() == Role.DOCTOR && hours > 1) {
            s.removeShift(date, type);
            throw new RosterException("Doctor exceeds 1h on " + date);
        }
        log(manager.id(), "ALLOCATE_SHIFT", staffId + " " + date + " " + type);
    }

    public void modifyShift(Manager manager, String staffId,
                            LocalDate date, ShiftType remove, ShiftType add) {
        requireManager(manager);
        Staff s = get(db.staff, staffId, "Staff");

        if (remove != null) {
            s.removeShift(date, remove);
        }
        if (add != null) {
            allocateShift(manager, staffId, date, add);
        }
        log(manager.id(), "MODIFY_SHIFT", staffId + " " + date + " -" + remove + " +" + add);
    }

    // ---- Residents & beds -------------------------------------------------------

    public void addResidentToVacantBed(Manager manager, Resident r, String bedId) {
        requireManager(manager);

        Bed b = get(db.beds, bedId, "Bed");
        if (!b.isVacant()) {
            throw new AllocationException("Bed occupied: " + bedId);
        }

        db.residents.put(r.id(), r);
        b.occupy(r.id(), r.gender());
        r.assignBed(b.id());

        log(manager.id(), "ADD_RESIDENT", r.id() + " -> " + bedId);
    }

    public void moveResident(Nurse nurse, String fromBedId, String toBedId, LocalDateTime when) {
        requireRoleAndRoster(nurse, Role.NURSE, when);

        Bed from = get(db.beds, fromBedId, "Bed");
        Bed to   = get(db.beds, toBedId,   "Bed");

        if (from.isVacant()) {
            throw new AllocationException("Source bed empty: " + fromBedId);
        }
        if (!to.isVacant()) {
            throw new AllocationException("Target bed occupied: " + toBedId);
        }

        Resident r = get(db.residents, from.residentId(), "Resident");

        from.vacate();
        to.occupy(r.id(), r.gender());
        r.assignBed(to.id());

        log(nurse.id(), "MOVE_RESIDENT", r.id() + " " + fromBedId + " -> " + toBedId);
    }

    public Resident checkResidentDetails(Staff staff, String bedId) {
        Bed b = get(db.beds, bedId, "Bed");
        if (b.isVacant()) {
            throw new NotFoundException("No resident in bed: " + bedId);
        }
        return get(db.residents, b.residentId(), "Resident");
    }

    // ---- Prescriptions ----------------------------------------------------------

    public String doctorAttachPrescription(Doctor doctor, String bedId,
                                           List<Prescription.MedicationOrder> orders,
                                           LocalDateTime when) {
        requireRoleAndRoster(doctor, Role.DOCTOR, when);

        Bed b = get(db.beds, bedId, "Bed");
        if (b.isVacant()) {
            throw new NotFoundException("No resident in bed: " + bedId);
        }

        Resident r = get(db.residents, b.residentId(), "Resident");

        String pid = "P-" + System.nanoTime();
        Prescription p = new Prescription(pid, r.id(), doctor.id());
        for (var mo : orders) {
            p.addOrder(mo);
        }

        db.prescriptions.put(pid, p);
        r.attachPrescription(pid);

        log(doctor.id(), "ADD_PRESCRIPTION", r.id() + " " + pid + " orders=" + orders.size());
        return pid;
    }

    public void administer(Nurse nurse, String bedId,
                           String drug, double dose, String unit, String notes,
                           LocalDateTime when) {
        requireRoleAndRoster(nurse, Role.NURSE, when);

        Bed b = get(db.beds, bedId, "Bed");
        if (b.isVacant()) {
            throw new NotFoundException("No resident in bed: " + bedId);
        }

        Resident r = get(db.residents, b.residentId(), "Resident");

        var rec = new AdministrationRecord(
            r.id(),
            drug,
            dose,
            unit,
            Instant.from(when.atZone(ZoneId.systemDefault())),
            nurse.id(),
            notes
        );

        db.administrations.add(rec);
        log(nurse.id(), "ADMINISTER", r.id() + " " + drug + " " + dose + unit);
    }

    // ---- Role/roster guards -----------------------------------------------------

    private void requireManager(Staff who) {
        if (who == null || who.role() != Role.MANAGER) {
            throw new AuthorizationException("Manager required");
        }
    }

    private void requireRoleAndRoster(Staff who, Role role, LocalDateTime when) {
        if (who == null || who.role() != role) {
            throw new AuthorizationException("Must be " + role);
        }
        if (!who.isRosteredAt(when)) {
            throw new AuthorizationException("Not rostered at " + when);
        }
    }
}

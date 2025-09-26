package carehome.view.cli;

import carehome.repo.Store;
import carehome.service.Service;
import carehome.domain.*;
import carehome.domain.Prescription.MedicationOrder;
import carehome.exception.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    private static final Scanner sc = new Scanner(System.in);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static void main(String[] args) {
        Store db = Store.get();

        // start clean each run (optional: comment these lines if you want persistence)
        db.residents.clear();
        db.staff.clear();
        db.wards.clear();
        db.rooms.clear();
        db.beds.clear();
        db.prescriptions.clear();
        db.administrations.clear();
        db.logs.clear();

        // seed: one ward/room with 2 beds
        Ward w1 = new Ward("W1", "Ward 1"); db.wards.put(w1.id(), w1);
        Room r1 = new Room("W1-R1", "W1"); db.rooms.put(r1.id(), r1); w1.addRoom(r1.id());
        Bed b1 = new Bed("W1-R1-B1", "W1-R1"); Bed b2 = new Bed("W1-R1-B2", "W1-R1");
        db.beds.put(b1.id(), b1); db.beds.put(b2.id(), b2); r1.addBed(b1.id()); r1.addBed(b2.id());

        // seed: one manager (acts as "admin")
        Manager mgr = new Manager("M-1", "Alice Manager");
        db.staff.put(mgr.id(), mgr);
        mgr.setPassword("admin");

        Service svc = new Service();

        System.out.println("CareHome CLI (Milestone 2-2) — simple menu. Type numbers and press Enter.");

        while (true) {
            try {
                printMenu();
                int choice = readInt("Choice: ");
                switch (choice) {
                    case 1 -> addNurse(svc, mgr);
                    case 2 -> addDoctor(svc, mgr);
                    case 3 -> modifyStaffPassword(svc, mgr);
                    case 4 -> allocateShift(svc, mgr);
                    case 5 -> addResidentToVacantBed(svc, mgr, db);
                    case 6 -> checkResidentDetails(svc);
                    case 7 -> doctorAttachPrescription(svc);
                    case 8 -> nurseAdminister(svc);
                    case 9 -> nurseMoveResident(svc);
                    case 10 -> showLogs(db);
                    case 11 -> listResidents(db);
                    case 12 -> listBeds(db);
                    case 0 -> {
                        db.save();
                        System.out.println("Saved. Bye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (RuntimeException ex) {
                // show service-layer exceptions nicely without stack traces
                System.out.println("ERROR: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
        }
    }

    // ---------------- Menu items ----------------

    private static void printMenu() {
        System.out.println("""
                
                --- MENU ---
                1. Add Nurse    (admin)
                2. Add Doctor   (admin)
                3. Modify Staff Password (admin)
                4. Allocate Shift (admin)
                5. Add Resident to Vacant Bed (admin)
                6. Check Resident Details by Bed
                7. Doctor: Attach Prescription
                8. Nurse: Administer Medication
                9. Nurse: Move Resident to Different Bed
                10. Show Action Logs
                11. List Residents
                12. List Beds
                0. Save & Exit
                """);
    }

    private static void addNurse(Service svc, Manager mgr) {
        String id = readLine("Nurse ID: ");
        String name = readLine("Name: ");
        String pwd = readLine("Password: ");
        Nurse n = new Nurse(id, name);
        svc.addStaff(mgr, n, pwd);
        System.out.println("Added " + n);
    }

    private static void addDoctor(Service svc, Manager mgr) {
        String id = readLine("Doctor ID: ");
        String name = readLine("Name: ");
        String pwd = readLine("Password: ");
        Doctor d = new Doctor(id, name);
        svc.addStaff(mgr, d, pwd);
        System.out.println("Added " + d);
    }

    private static void modifyStaffPassword(Service svc, Manager mgr) {
        String sid = readLine("Staff ID to update: ");
        String pwd = readLine("New password: ");
        svc.modifyStaffPassword(mgr, sid, pwd);
        System.out.println("Password updated for " + sid);
    }

    private static void allocateShift(Service svc, Manager mgr) {
        String sid = readLine("Staff ID: ");
        LocalDate date = readDate("Date (yyyy-MM-dd): ");
        ShiftType type = readShiftType();
        svc.allocateShift(mgr, sid, date, type);
        System.out.println("Allocated " + type + " on " + date + " to " + sid);
    }

    private static void addResidentToVacantBed(Service svc, Manager mgr, Store db) {
        String rid = readLine("Resident ID: ");
        String name = readLine("Resident Name: ");
        Gender g = readGender();
        String bedId = readLine("Target Bed ID: ");
        Resident r = new Resident(rid, name, g);
        svc.addResidentToVacantBed(mgr, r, bedId);
        System.out.println("Resident added to bed: " + bedId);
    }

    private static void checkResidentDetails(Service svc) {
        String bedId = readLine("Bed ID: ");
        Resident r = svc.checkResidentDetails(fakeStaffForCheck(), bedId);
        System.out.println("Resident in " + bedId + ": " + r.name() + " (ID=" + r.id() + ", Gender=" + r.gender() + ")");
    }

    private static void doctorAttachPrescription(Service svc) {
        String docId = readLine("Doctor ID: ");
        String bedId = readLine("Bed ID: ");
        LocalDate date = readDate("Date (yyyy-MM-dd): ");
        LocalTime time = readTime("Time (HH:mm) [doctor window 09:00–10:00]: ");
        LocalDateTime when = LocalDateTime.of(date, time);

        List<MedicationOrder> orders = new ArrayList<>();
        while (true) {
            String drug = readLine("Drug (empty to finish): ");
            if (drug.isBlank()) break;
            double dose = readDouble("Dose (number): ");
            String unit = readLine("Unit (e.g., mg, tab): ");
            String sched = readLine("Schedule (e.g., 8am, 8pm): ");
            String notes = readLine("Notes (optional): ");
            orders.add(new MedicationOrder(drug, dose, unit, sched, notes));
        }

        // build a temp Doctor object only to pass ID/role; real staff must already be in Store
        Doctor doc = new Doctor(docId, "Doctor");
        String pid = svc.doctorAttachPrescription(doc, bedId, orders, when);
        System.out.println("Prescription added: " + pid + " (orders=" + orders.size() + ")");
    }

    private static void nurseAdminister(Service svc) {
        String nurseId = readLine("Nurse ID: ");
        String bedId = readLine("Bed ID: ");
        String drug = readLine("Drug: ");
        double dose = readDouble("Dose (number): ");
        String unit = readLine("Unit (e.g., mg, tab): ");
        String notes = readLine("Notes (optional): ");
        LocalDate date = readDate("Date (yyyy-MM-dd): ");
        LocalTime time = readTime("Time (HH:mm) [NURSE_AM 08:00–16:00, NURSE_PM 14:00–22:00]: ");
        LocalDateTime when = LocalDateTime.of(date, time);

        Nurse n = new Nurse(nurseId, "Nurse");
        svc.administer(n, bedId, drug, dose, unit, notes, when);
        System.out.println("Administration recorded.");
    }

    private static void nurseMoveResident(Service svc) {
        String nurseId = readLine("Nurse ID: ");
        String fromBed = readLine("From Bed ID: ");
        String toBed = readLine("To Bed ID: ");
        LocalDate date = readDate("Date (yyyy-MM-dd): ");
        LocalTime time = readTime("Time (HH:mm): ");
        LocalDateTime when = LocalDateTime.of(date, time);

        Nurse n = new Nurse(nurseId, "Nurse");
        svc.moveResident(n, fromBed, toBed, when);
        System.out.println("Resident moved.");
    }

    private static void showLogs(Store db) {
        System.out.println("\n--- Action Logs ---");
        db.logs.forEach(l -> System.out.println(l.when() + " | " + l.staffId() + " | " + l.type() + " | " + l.details()));
    }

    private static void listResidents(Store db) {
        System.out.println("\n--- Residents ---");
        if (db.residents.isEmpty()) System.out.println("(none)");
        db.residents.values().forEach(r -> System.out.println(r.id() + " | " + r.name() + " | " + r.gender() + " | bed=" + r.currentBedId().orElse("-")));
    }

    private static void listBeds(Store db) {
        System.out.println("\n--- Beds ---");
        if (db.beds.isEmpty()) System.out.println("(none)");
        db.beds.values().forEach(b -> {
            String occ = b.isVacant() ? "(vacant)" : "occupied by " + b.residentId();
            System.out.println(b.id() + " | room=" + b.roomId() + " | " + occ);
        });
    }

    // ---------------- Helpers ----------------

    private static ShiftType readShiftType() {
        System.out.println("Shift Types: 1) NURSE_AM (8h), 2) NURSE_PM (8h), 3) DOCTOR_1H (1h)");
        int n = readInt("Select (1-3): ");
        return switch (n) {
            case 1 -> ShiftType.NURSE_AM;
            case 2 -> ShiftType.NURSE_PM;
            case 3 -> ShiftType.DOCTOR_1H;
            default -> throw new IllegalArgumentException("Invalid shift selection");
        };
    }

    private static Gender readGender() {
        System.out.println("Gender: 1) M, 2) F");
        int n = readInt("Select (1-2): ");
        return switch (n) {
            case 1 -> Gender.M;
            case 2 -> Gender.F;
            default -> throw new IllegalArgumentException("Invalid gender selection");
        };
    }

    private static LocalDate readDate(String prompt) {
        String s = readLine(prompt);
        return LocalDate.parse(s, DATE_FMT);
    }

    private static LocalTime readTime(String prompt) {
        String s = readLine(prompt);
        return LocalTime.parse(s, TIME_FMT);
    }

    private static int readInt(String prompt) {
        System.out.print(prompt);
        while (!sc.hasNextInt()) { sc.nextLine(); System.out.print("Enter a number: "); }
        int val = sc.nextInt();
        sc.nextLine(); // consume EOL
        return val;
    }

    private static double readDouble(String prompt) {
        System.out.print(prompt);
        while (!sc.hasNextDouble()) { sc.nextLine(); System.out.print("Enter a number: "); }
        double val = sc.nextDouble();
        sc.nextLine(); // consume EOL
        return val;
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    /**
     * For 'checkResidentDetails' we don't require a specific role; pass any Staff.
     * Here we pass a Manager-shaped staff with ID "M-QUERY" just to satisfy method signature.
     */
    private static Staff fakeStaffForCheck() {
        return new Manager("M-QUERY", "Query Manager");
    }
}

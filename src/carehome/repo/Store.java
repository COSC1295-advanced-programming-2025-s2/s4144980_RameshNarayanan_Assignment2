package carehome.repo;

import java.io.*;
import java.util.*;
import carehome.domain.*;

/**
 * Singleton repository that stores all in-memory objects
 * (residents, staff, wards, rooms, beds, prescriptions, logs).
 * Provides simple serialization to save/load state to a file.
 */
public final class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FILE = "store.dat";
    private static Store INSTANCE;

    public final Map<String, Resident> residents = new HashMap<>();
    public final Map<String, Staff> staff = new HashMap<>();
    public final Map<String, Ward> wards = new HashMap<>();
    public final Map<String, Room> rooms = new HashMap<>();
    public final Map<String, Bed> beds = new HashMap<>();
    public final Map<String, Prescription> prescriptions = new HashMap<>();
    public final List<AdministrationRecord> administrations = new ArrayList<>();
    public final List<ActionLog> logs = new ArrayList<>();

    private Store() { }

    public static synchronized Store get() {
        if (INSTANCE == null) {
            INSTANCE = load().orElse(new Store());
        }
        return INSTANCE;
    }

    public static Optional<Store> load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE))) {
            return Optional.of((Store) ois.readObject());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE))) {
            oos.writeObject(this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data", e);
        }
    }
}

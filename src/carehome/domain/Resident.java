package carehome.domain;

import java.io.Serializable;
import java.util.*;

public class Resident implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private Gender gender;
    private String currentBedId;
    private final List<String> prescriptionIds = new ArrayList<>();

    public Resident(String id, String name, Gender gender) {
        this.id = id;
        this.name = name;
        this.gender = gender;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Gender gender() {
        return gender;
    }

    public Optional<String> currentBedId() {
        return Optional.ofNullable(currentBedId);
    }

    public void assignBed(String bedId) {
        this.currentBedId = bedId;
    }

    public List<String> prescriptions() {
        return Collections.unmodifiableList(prescriptionIds);
    }

    public void attachPrescription(String prescId) {
        prescriptionIds.add(prescId);
    }
}

package carehome.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

public class Prescription implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final class MedicationOrder implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String drug;
        public final double dose;
        public final String unit;
        public final String schedule;
        public final String notes;

        public MedicationOrder(String drug, double dose, String unit, String schedule, String notes) {
            this.drug = drug;
            this.dose = dose;
            this.unit = unit;
            this.schedule = schedule;
            this.notes = notes;
        }

        @Override
        public String toString() {
            return drug + " " + dose + unit + " @ " + schedule + (notes == null ? "" : " (" + notes + ")");
        }
    }

    private final String id;
    private final String residentId;
    private final String doctorId;
    private final Instant createdAt = Instant.now();
    private final List<MedicationOrder> orders = new ArrayList<>();

    public Prescription(String id, String residentId, String doctorId) {
        this.id = id;
        this.residentId = residentId;
        this.doctorId = doctorId;
    }

    public String id() {
        return id;
    }

    public String residentId() {
        return residentId;
    }

    public String doctorId() {
        return doctorId;
    }

    public List<MedicationOrder> orders() {
        return Collections.unmodifiableList(orders);
    }

    public void addOrder(MedicationOrder mo) {
        orders.add(mo);
    }
}

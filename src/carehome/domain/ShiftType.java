package carehome.domain;

// Duration hours are used for roster rule checks
public enum ShiftType {
    NURSE_AM(8),
    NURSE_PM(8),
    DOCTOR_1H(1);

    public final int hours;

    ShiftType(int h) {
        this.hours = h;
    }
}

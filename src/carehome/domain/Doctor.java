package carehome.domain;

public final class Doctor extends Staff {

    public Doctor(String id, String name) {
        super(id, name, Role.DOCTOR);
    }
}

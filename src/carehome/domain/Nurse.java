package carehome.domain;

public final class Nurse extends Staff {

    public Nurse(String id, String name) {
        super(id, name, Role.NURSE);
    }
}

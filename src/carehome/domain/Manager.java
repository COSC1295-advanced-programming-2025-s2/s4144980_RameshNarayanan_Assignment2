package carehome.domain;

public final class Manager extends Staff {

    public Manager(String id, String name) {
        super(id, name, Role.MANAGER);
    }
}

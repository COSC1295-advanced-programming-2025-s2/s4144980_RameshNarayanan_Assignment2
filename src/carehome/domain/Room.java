package carehome.domain;

import java.io.Serializable;
import java.util.*;

public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String wardId;
    private final List<String> bedIds = new ArrayList<>();

    public Room(String id, String wardId) {
        this.id = id;
        this.wardId = wardId;
    }

    public String id() {
        return id;
    }

    public String wardId() {
        return wardId;
    }

    public List<String> bedIds() {
        return Collections.unmodifiableList(bedIds);
    }

    public void addBed(String bedId) {
        bedIds.add(bedId);
    }
}

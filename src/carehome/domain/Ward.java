package carehome.domain;

import java.io.Serializable;
import java.util.*;

public class Ward implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String name;
    private final List<String> roomIds = new ArrayList<>();

    public Ward(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public List<String> roomIds() {
        return Collections.unmodifiableList(roomIds);
    }

    public void addRoom(String roomId) {
        roomIds.add(roomId);
    }
}

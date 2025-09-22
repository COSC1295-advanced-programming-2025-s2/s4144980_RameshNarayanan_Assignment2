package carehome.domain;

import java.io.Serializable;

public class Bed implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String roomId;
    private String residentId;
    private Gender genderTag;

    public Bed(String id, String roomId) {
        this.id = id;
        this.roomId = roomId;
    }

    public String id() {
        return id;
    }

    public String roomId() {
        return roomId;
    }

    public boolean isVacant() {
        return residentId == null;
    }

    public String residentId() {
        return residentId;
    }

    public Gender genderTag() {
        return genderTag;
    }

    public void occupy(String residentId, Gender g) {
        this.residentId = residentId;
        this.genderTag = g;
    }

    public void vacate() {
        this.residentId = null;
        this.genderTag = null;
    }
}

package carehome.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public abstract class Staff implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private String name;
    private final Role role;
    private String password;

    // Simple roster: date -> set of shift types
    private final Map<LocalDate, EnumSet<ShiftType>> roster = new HashMap<>();

    protected Staff(String id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Role role() {
        return role;
    }

    public void rename(String newName) {
        this.name = Objects.requireNonNull(newName);
    }

    public void setPassword(String pwd) {
        this.password = Objects.requireNonNull(pwd);
    }

    public boolean checkPassword(String pwd) {
        return Objects.equals(password, pwd);
    }

    public Map<LocalDate, EnumSet<ShiftType>> roster() {
        return roster;
    }

    public void assignShift(LocalDate date, ShiftType type) {
        roster.computeIfAbsent(date, d -> EnumSet.noneOf(ShiftType.class)).add(type);
    }

    public void removeShift(LocalDate date, ShiftType type) {
        var set = roster.get(date);
        if (set != null) {
            set.remove(type);
            if (set.isEmpty()) {
                roster.remove(date);
            }
        }
    }

    public int hoursOn(LocalDate date) {
        return roster.getOrDefault(date, EnumSet.noneOf(ShiftType.class))
                     .stream()
                     .mapToInt(t -> t.hours)
                     .sum();
    }

    public boolean isRosteredAt(LocalDateTime when) {
        var set = roster.get(when.toLocalDate());
        if (set == null) return false;

        int h = when.getHour();
        boolean am = h >= 8 && h < 16;
        boolean pm = h >= 14 && h < 22;
        boolean doc = h >= 9 && h < 10; // doctor 1h window

        return (am && set.contains(ShiftType.NURSE_AM))
            || (pm && set.contains(ShiftType.NURSE_PM))
            || (doc && set.contains(ShiftType.DOCTOR_1H));
    }
    
    @Override
    public String toString() {
        return role() + "{" + id() + ": " + name() + "}";
    }

}

package carehome.domain;

import java.io.Serializable;
import java.time.Instant;

public record AdministrationRecord(
    String residentId,
    String drug,
    double dose,
    String unit,
    Instant time,
    String nurseId,
    String notes
) implements Serializable {}

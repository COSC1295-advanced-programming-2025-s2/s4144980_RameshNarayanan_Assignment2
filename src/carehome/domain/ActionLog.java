package carehome.domain;

import java.io.Serializable;
import java.time.Instant;

public record ActionLog(
    Instant when,
    String staffId,
    String type,
    String details
) implements Serializable {}

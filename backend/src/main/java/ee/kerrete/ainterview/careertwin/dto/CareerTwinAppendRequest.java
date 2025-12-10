package ee.kerrete.ainterview.careertwin.dto;

import jakarta.validation.constraints.NotBlank;

public record CareerTwinAppendRequest(@NotBlank String entryText) {
}


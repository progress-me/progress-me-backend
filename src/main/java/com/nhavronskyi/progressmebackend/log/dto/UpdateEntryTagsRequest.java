package com.nhavronskyi.progressmebackend.log.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record UpdateEntryTagsRequest(
        @NotNull List<UUID> valueIds
) {
}

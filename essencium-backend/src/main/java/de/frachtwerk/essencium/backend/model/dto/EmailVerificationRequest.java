package de.frachtwerk.essencium.backend.model.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EmailVerificationRequest(@NotNull UUID emailVerifyToken) {}

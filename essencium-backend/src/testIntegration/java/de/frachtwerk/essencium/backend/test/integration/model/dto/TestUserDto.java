package de.frachtwerk.essencium.backend.test.integration.model.dto;

import de.frachtwerk.essencium.backend.model.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Data
public class TestUserDto extends UserDto<Long> {}

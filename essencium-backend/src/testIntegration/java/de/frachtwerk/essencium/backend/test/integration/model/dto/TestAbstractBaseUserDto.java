package de.frachtwerk.essencium.backend.test.integration.model.dto;

import de.frachtwerk.essencium.backend.model.dto.AbstractBaseUserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@Data
public class TestAbstractBaseUserDto extends AbstractBaseUserDto<Long> {}

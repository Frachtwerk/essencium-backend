/*
 * Copyright (C) 2023 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of essencium-backend.
 *
 * essencium-backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * essencium-backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with essencium-backend. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.essencium.backend.model.validation;

import com.nulabinc.zxcvbn.Zxcvbn;
import de.frachtwerk.essencium.backend.configuration.properties.SecurityConfigProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

  private final Zxcvbn zxcvbn;

  private boolean allowEmpty;

  private final SecurityConfigProperties securityConfigProperties;

  @Autowired
  public StrongPasswordValidator(SecurityConfigProperties securityConfigProperties) {
    this.securityConfigProperties = securityConfigProperties;
    this.zxcvbn = new Zxcvbn();
  }

  @Override
  public void initialize(StrongPassword annotation) {
    allowEmpty = annotation.allowEmpty();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return (Strings.isEmpty(value) && allowEmpty)
        || zxcvbn.measure(value).getScore() >= securityConfigProperties.getMinPasswordStrength();
  }
}

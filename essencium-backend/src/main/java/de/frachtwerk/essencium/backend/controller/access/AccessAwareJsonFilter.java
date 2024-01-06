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

package de.frachtwerk.essencium.backend.controller.access;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import de.frachtwerk.essencium.backend.model.Ownable;
import de.frachtwerk.essencium.backend.model.Role;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor
public class AccessAwareJsonFilter<USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends SimpleBeanPropertyFilter {
  private USER principal;

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jsonGenerator, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    JsonAllowFor ann = writer.getMember().getAnnotation(JsonAllowFor.class);
    if (include(writer)
        && (ann == null
            || Arrays.stream(ann.roles())
                .anyMatch(s -> principal.getRoles().stream().map(Role::getName).anyMatch(s::equals))
            || Stream.of(ann.rights())
                .anyMatch(
                    r ->
                        principal.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .anyMatch(r::equals))
            || (ann.allowForOwner() && isOwner(pojo)))) {
      writer.serializeAsField(pojo, jsonGenerator, provider);
    } else if (!jsonGenerator.canOmitFields()) {
      writer.serializeAsOmittedField(pojo, jsonGenerator, provider);
    }
  }

  @SuppressWarnings("unchecked")
  private boolean isOwner(Object obj) {
    if (Ownable.class.isAssignableFrom(obj.getClass())) {
      return ((Ownable<USER, ID>) obj).isOwnedBy(principal);
    } else {
      return false;
    }
  }
}

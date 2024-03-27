/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import de.frachtwerk.essencium.backend.model.ApiTokenUser;
import de.frachtwerk.essencium.backend.model.Ownable;
import de.frachtwerk.essencium.backend.model.Role;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@AllArgsConstructor
public class AccessAwareJsonFilter<
        U extends UserDetails, USER extends AbstractBaseUser<ID>, ID extends Serializable>
    extends SimpleBeanPropertyFilter {
  private U userDetails;

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jsonGenerator, SerializerProvider provider, PropertyWriter writer)
      throws Exception {

    if (userDetails instanceof ApiTokenUser principal) {
      JsonAllowFor ann = writer.getMember().getAnnotation(JsonAllowFor.class);
      if (isAllowedToAccess(principal, ann)) {
        serializeField(pojo, jsonGenerator, provider, writer);
      }
    }

    if (userDetails instanceof AbstractBaseUser<?> principal) {
      JsonAllowFor ann = writer.getMember().getAnnotation(JsonAllowFor.class);
      if (isAllowedToAccess(pojo, principal, ann)) {
        serializeField(pojo, jsonGenerator, provider, writer);
      }
    }
  }

  void serializeField(
      Object pojo, JsonGenerator jsonGenerator, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    if (include(writer)) {
      writer.serializeAsField(pojo, jsonGenerator, provider);
    } else if (!jsonGenerator.canOmitFields()) {
      writer.serializeAsOmittedField(pojo, jsonGenerator, provider);
    }
  }

  boolean isAllowedToAccess(Object pojo, AbstractBaseUser<?> principal, JsonAllowFor ann) {
    return ann == null
        || Arrays.stream(ann.roles())
            .anyMatch(s -> principal.getRoles().stream().map(Role::getName).anyMatch(s::equals))
        || Stream.of(ann.rights())
            .anyMatch(
                r ->
                    principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(r::equals))
        || (ann.allowForOwner() && isOwner(pojo));
  }

  boolean isAllowedToAccess(ApiTokenUser principal, JsonAllowFor ann) {
    return ann == null
        || Stream.of(ann.rights())
            .anyMatch(
                r ->
                    principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(r::equals));
  }

  @SuppressWarnings("unchecked")
  private boolean isOwner(Object obj) {
    if (Ownable.class.isAssignableFrom(obj.getClass())) {
      return ((Ownable<USER, ID>) obj).isOwnedBy((USER) userDetails);
    } else {
      return false;
    }
  }
}

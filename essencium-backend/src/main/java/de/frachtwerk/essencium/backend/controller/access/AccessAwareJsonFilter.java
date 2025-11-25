/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import de.frachtwerk.essencium.backend.model.Ownable;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@AllArgsConstructor
public class AccessAwareJsonFilter<
        AUTHUSER extends EssenciumUserDetails<ID>, ID extends Serializable>
    implements PropertyFilter {
  private AUTHUSER principal;

  @Override
  public void serializeAsField(
      Object pojo, JsonGenerator jsonGenerator, SerializerProvider provider, PropertyWriter writer)
      throws Exception {
    JsonAllowFor ann = writer.getMember().getAnnotation(JsonAllowFor.class);
    if (ann == null
        || Arrays.stream(ann.roles())
            .anyMatch(
                s ->
                    principal.getRoles().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(s::equals))
        || Stream.of(ann.rights())
            .anyMatch(
                r ->
                    principal.getRights().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(r::equals))
        || (ann.allowForOwner() && isOwner(pojo))) {
      writer.serializeAsField(pojo, jsonGenerator, provider);
    } else if (!jsonGenerator.canOmitFields()) {
      writer.serializeAsOmittedField(pojo, jsonGenerator, provider);
    }
  }

  @Override
  public void serializeAsElement(
      Object elementValue,
      JsonGenerator jsonGenerator,
      SerializerProvider provider,
      PropertyWriter writer)
      throws Exception {
    // For array/collection elements, just serialize normally
    writer.serializeAsField(elementValue, jsonGenerator, provider);
  }

  @Override
  public void depositSchemaProperty(
      PropertyWriter writer, JsonObjectFormatVisitor objectVisitor, SerializerProvider provider) {
    // Default implementation - include all properties in schema
  }

  @Override
  public void depositSchemaProperty(
      PropertyWriter writer, ObjectNode propertiesNode, SerializerProvider provider) {
    // Default implementation - include all properties in schema
  }

  @SuppressWarnings("unchecked")
  private boolean isOwner(Object obj) {
    if (Ownable.class.isAssignableFrom(obj.getClass())) {
      return ((Ownable<AUTHUSER, ID>) obj).isOwnedBy(principal);
    } else {
      return false;
    }
  }
}

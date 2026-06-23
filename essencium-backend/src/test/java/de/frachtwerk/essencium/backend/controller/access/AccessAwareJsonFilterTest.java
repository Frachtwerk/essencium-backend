/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Ownable;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.PropertyWriter;

@ExtendWith(MockitoExtension.class)
class AccessAwareJsonFilterTest {

  @Mock private EssenciumUserDetails<Long> principal;
  @Mock private PropertyWriter writer;
  @Mock private JsonGenerator jsonGenerator;
  @Mock private SerializationContext serializationContext;
  @Mock private AnnotatedMember annotatedMember;
  @Mock private JsonAllowFor annotation;

  private AccessAwareJsonFilter<EssenciumUserDetails<Long>, Long> filter;

  @BeforeEach
  void setUp() {
    filter = new AccessAwareJsonFilter<>(principal);
    lenient().doReturn(toAuthorities(Set.of())).when(principal).getRoles();
    lenient().doReturn(toAuthorities(Set.of())).when(principal).getRights();
  }

  @Test
  void serializeAsProperty_allowsUnannotatedProperty() throws Exception {
    Object pojo = new Object();
    when(writer.getMember()).thenReturn(annotatedMember);
    when(annotatedMember.getAnnotation(JsonAllowFor.class)).thenReturn(null);

    filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, writer);

    verify(writer).serializeAsProperty(pojo, jsonGenerator, serializationContext);
    verify(writer, never()).serializeAsOmittedProperty(any(), any(), any());
  }

  @Test
  void serializeAsProperty_omitsDeniedPropertyWhenGeneratorCannotOmit() throws Exception {
    Object pojo = new Object();
    denyByAnnotation();
    when(jsonGenerator.canOmitProperties()).thenReturn(false);

    filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, writer);

    verify(writer, never()).serializeAsProperty(any(), any(), any());
    verify(writer).serializeAsOmittedProperty(pojo, jsonGenerator, serializationContext);
  }

  @Test
  void serializeAsProperty_skipsDeniedPropertyWhenGeneratorCanOmit() throws Exception {
    Object pojo = new Object();
    denyByAnnotation();
    when(jsonGenerator.canOmitProperties()).thenReturn(true);

    filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, writer);

    verify(writer, never()).serializeAsProperty(any(), any(), any());
    verify(writer, never()).serializeAsOmittedProperty(any(), any(), any());
  }

  @Test
  void serializeAsProperty_allowsMatchingRole() throws Exception {
    Object pojo = new Object();
    when(writer.getMember()).thenReturn(annotatedMember);
    when(annotatedMember.getAnnotation(JsonAllowFor.class)).thenReturn(annotation);
    when(annotation.roles()).thenReturn(new String[] {"ROLE_ADMIN"});
    stubPrincipalRoles(Set.of("ROLE_ADMIN"));

    filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, writer);

    verify(writer).serializeAsProperty(pojo, jsonGenerator, serializationContext);
    verify(writer, never()).serializeAsOmittedProperty(any(), any(), any());
  }

  @Test
  void serializeAsProperty_allowsMatchingRight() throws Exception {
    Object pojo = new Object();
    when(writer.getMember()).thenReturn(annotatedMember);
    when(annotatedMember.getAnnotation(JsonAllowFor.class)).thenReturn(annotation);
    when(annotation.roles()).thenReturn(new String[0]);
    when(annotation.rights()).thenReturn(new String[] {"RIGHT_READ"});
    stubPrincipalRights(Set.of("RIGHT_READ"));

    filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, writer);

    verify(writer).serializeAsProperty(pojo, jsonGenerator, serializationContext);
    verify(writer, never()).serializeAsOmittedProperty(any(), any(), any());
  }

  @Test
  void serializeAsProperty_allowsOwnerIfConfigured() throws Exception {
    OwnedResource ownedResource = new OwnedResource(true);
    when(writer.getMember()).thenReturn(annotatedMember);
    when(annotatedMember.getAnnotation(JsonAllowFor.class)).thenReturn(annotation);
    when(annotation.roles()).thenReturn(new String[0]);
    when(annotation.rights()).thenReturn(new String[0]);
    when(annotation.allowForOwner()).thenReturn(true);

    filter.serializeAsProperty(ownedResource, jsonGenerator, serializationContext, writer);

    verify(writer).serializeAsProperty(ownedResource, jsonGenerator, serializationContext);
    verify(writer, never()).serializeAsOmittedProperty(any(), any(), any());
  }

  @Test
  void serializeAsElement_omitsDeniedElement() throws Exception {
    Object value = new Object();
    denyByAnnotation();

    filter.serializeAsElement(value, jsonGenerator, serializationContext, writer);

    verify(writer, never()).serializeAsElement(any(), any(), any());
    verify(writer).serializeAsOmittedProperty(value, jsonGenerator, serializationContext);
  }

  @Test
  void snapshot_returnsSameFilterInstance() {
    PropertyFilter snapshot = filter.snapshot();

    assertThat(snapshot).isSameAs(filter);
  }

  private void denyByAnnotation() {
    when(writer.getMember()).thenReturn(annotatedMember);
    when(annotatedMember.getAnnotation(JsonAllowFor.class)).thenReturn(annotation);
    when(annotation.roles()).thenReturn(new String[0]);
    when(annotation.rights()).thenReturn(new String[0]);
    when(annotation.allowForOwner()).thenReturn(false);
  }

  private void stubPrincipalRoles(Set<String> roles) {
    doReturn(toAuthorities(roles)).when(principal).getRoles();
  }

  private void stubPrincipalRights(Set<String> rights) {
    doReturn(toAuthorities(rights)).when(principal).getRights();
  }

  private Set<? extends GrantedAuthority> toAuthorities(Set<String> authorities) {
    return authorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
  }

  private static final class OwnedResource implements Ownable<EssenciumUserDetails<Long>, Long> {
    private final boolean owned;

    private OwnedResource(boolean owned) {
      this.owned = owned;
    }

    @Override
    public boolean isOwnedBy(EssenciumUserDetails<Long> user) {
      return owned;
    }
  }
}

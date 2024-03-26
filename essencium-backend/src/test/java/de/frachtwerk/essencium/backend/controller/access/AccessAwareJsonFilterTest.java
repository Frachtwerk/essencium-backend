package de.frachtwerk.essencium.backend.controller.access;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import de.frachtwerk.essencium.backend.model.*;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AccessAwareJsonFilterTest {

  @Test
  void noUserDetails() {
    AccessAwareJsonFilter accessAwareJsonFilter = new AccessAwareJsonFilter(null);
    assertDoesNotThrow(
        () ->
            accessAwareJsonFilter.serializeAsField(
                mock(Object.class),
                mock(JsonGenerator.class),
                mock(SerializerProvider.class),
                mock(PropertyWriter.class)));
  }

  @Nested
  class AccessAwareJsonFilterAbstractBaseUserTest {
    @Test
    void annotationNonExisting() {
      AbstractBaseUser<?> userDetails = mock(AbstractBaseUser.class);
      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertTrue(accessAwareJsonFilter.isAllowedToAccess(mock(Object.class), userDetails, null));
    }

    @Test
    void annotationExistingButNothingElse() {
      AbstractBaseUser<?> userDetails = mock(AbstractBaseUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);

      when(jsonAllowFor.roles()).thenReturn(new String[0]);
      when(jsonAllowFor.rights()).thenReturn(new String[0]);
      when(jsonAllowFor.allowForOwner()).thenReturn(false);

      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertFalse(
          accessAwareJsonFilter.isAllowedToAccess(mock(Object.class), userDetails, jsonAllowFor));
    }

    @Test
    void annotationExistingAndRole() {
      AbstractBaseUser<?> userDetails = mock(AbstractBaseUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);

      when(jsonAllowFor.roles()).thenReturn(new String[] {"role"});

      when(userDetails.getRoles()).thenReturn(Set.of(Role.builder().name("role").build()));

      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertTrue(
          accessAwareJsonFilter.isAllowedToAccess(mock(Object.class), userDetails, jsonAllowFor));
    }

    @Test
    void annotationExistingAndRight() {
      AbstractBaseUser<?> userDetails = mock(AbstractBaseUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);

      when(jsonAllowFor.roles()).thenReturn(new String[0]);
      when(jsonAllowFor.rights()).thenReturn(new String[] {"right"});

      when(userDetails.getAuthorities())
          .thenReturn(Set.of(Right.builder().authority("right").build()));

      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertTrue(
          accessAwareJsonFilter.isAllowedToAccess(mock(Object.class), userDetails, jsonAllowFor));
    }

    @Test
    void annotationExistingAndOwner() {
      AbstractBaseUser<Serializable> userDetails = mock(AbstractBaseUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);
      Ownable<AbstractBaseUser<Serializable>, Serializable> ownedObject = mock(Ownable.class);

      when(jsonAllowFor.roles()).thenReturn(new String[0]);
      when(jsonAllowFor.rights()).thenReturn(new String[0]);
      when(jsonAllowFor.allowForOwner()).thenReturn(true);

      when(ownedObject.isOwnedBy(userDetails)).thenReturn(true);

      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertTrue(accessAwareJsonFilter.isAllowedToAccess(ownedObject, userDetails, jsonAllowFor));
    }

    @Test
    void annotationExistingAndNotOwner() {
      AbstractBaseUser<Serializable> userDetails = mock(AbstractBaseUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);
      Ownable<AbstractBaseUser<Serializable>, Serializable> ownedObject = mock(Ownable.class);

      when(jsonAllowFor.roles()).thenReturn(new String[0]);
      when(jsonAllowFor.rights()).thenReturn(new String[0]);
      when(jsonAllowFor.allowForOwner()).thenReturn(true);

      when(ownedObject.isOwnedBy(any())).thenReturn(false);

      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertFalse(accessAwareJsonFilter.isAllowedToAccess(ownedObject, userDetails, jsonAllowFor));
    }

    @Test
    void annotationExistingAndOwnerButIgnored() {
      AbstractBaseUser<Serializable> userDetails = mock(AbstractBaseUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);
      Ownable<AbstractBaseUser<Serializable>, Serializable> ownedObject = mock(Ownable.class);

      when(jsonAllowFor.roles()).thenReturn(new String[0]);
      when(jsonAllowFor.rights()).thenReturn(new String[0]);
      when(jsonAllowFor.allowForOwner()).thenReturn(false);

      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertFalse(accessAwareJsonFilter.isAllowedToAccess(ownedObject, userDetails, jsonAllowFor));
    }
  }

  @Nested
  class AccessAwareJsonFilterApiTokenUserTest {
    @Test
    void annotationNonExisting() {
      ApiTokenUser userDetails = mock(ApiTokenUser.class);
      AccessAwareJsonFilter<ApiTokenUser, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertTrue(accessAwareJsonFilter.isAllowedToAccess(userDetails, null));
    }

    @Test
    void annotationExistingButNothingElse() {
      ApiTokenUser userDetails = mock(ApiTokenUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);

      when(jsonAllowFor.rights()).thenReturn(new String[0]);

      AccessAwareJsonFilter<ApiTokenUser, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertFalse(accessAwareJsonFilter.isAllowedToAccess(userDetails, jsonAllowFor));
    }

    @Test
    void annotationExistingAndRight() {
      ApiTokenUser userDetails = mock(ApiTokenUser.class);
      JsonAllowFor jsonAllowFor = mock(JsonAllowFor.class);
      Collection<? extends GrantedAuthority> rights =
          List.of(Right.builder().authority("right").build());

      when(jsonAllowFor.rights()).thenReturn(new String[] {"right"});

      when(userDetails.getAuthorities()).thenAnswer(invocation -> rights);

      AccessAwareJsonFilter<ApiTokenUser, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(userDetails);
      assertTrue(accessAwareJsonFilter.isAllowedToAccess(userDetails, jsonAllowFor));
    }
  }

  @Nested
  class AccessAwareJsonFilterSerializeAsFieldTest {

    PropertyWriter writer = mock(PropertyWriter.class);
    AnnotatedMember member = mock(AnnotatedMember.class);

    @BeforeEach
    public void setUp() {
      when(writer.getMember()).thenReturn(member);
      when(member.getAnnotation(JsonAllowFor.class)).thenReturn(null);
    }

    @Test
    void apiTokenUserTest() throws Exception {
      AccessAwareJsonFilter<ApiTokenUser, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(mock(ApiTokenUser.class));

      accessAwareJsonFilter.serializeAsField(
          mock(Object.class), mock(JsonGenerator.class), mock(SerializerProvider.class), writer);

      verify(writer, times(1)).getMember();
    }

    @Test
    void abstractBaseUserTest() throws Exception {
      AccessAwareJsonFilter<AbstractBaseUser<?>, AbstractBaseUser<Serializable>, Serializable>
          accessAwareJsonFilter = new AccessAwareJsonFilter<>(mock(AbstractBaseUser.class));

      accessAwareJsonFilter.serializeAsField(
          mock(Object.class), mock(JsonGenerator.class), mock(SerializerProvider.class), writer);

      verify(writer, times(1)).getMember();
    }
  }
}

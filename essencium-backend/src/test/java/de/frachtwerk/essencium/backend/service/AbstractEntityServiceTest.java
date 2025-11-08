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

package de.frachtwerk.essencium.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.SequenceIdModel;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.model.exception.ResourceUpdateException;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class AbstractEntityServiceTest {

  private final BaseRepository<TestSequenceIdModel, Long> repositoryMock = mock(TestRepo.class);
  private final TestImpl testSubject = new TestImpl(repositoryMock);

  @Test
  void getAll() {
    var resultMock = new ArrayList<TestSequenceIdModel>();

    when(repositoryMock.findAll()).thenReturn(resultMock);

    assertThat(testSubject.getAll()).isEqualTo(resultMock);

    Mockito.verify(repositoryMock, Mockito.times(1)).findAll();
  }

  @Test
  void getAll_paged() {
    var pageableMock = mock(Pageable.class);
    var resultMock = mock(Page.class);
    // noinspection unchecked
    when(resultMock.map(Mockito.any())).thenReturn(resultMock);

    // noinspection unchecked
    when(repositoryMock.findAll(pageableMock)).thenReturn(resultMock);

    assertThat(testSubject.getAll(pageableMock)).isEqualTo(resultMock);

    Mockito.verify(repositoryMock, Mockito.times(1)).findAll(pageableMock);
  }

  @Nested
  class GetById {
    @Test
    void entityNotFound() {
      var inputId = 42L;

      when(repositoryMock.findById(inputId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.getById(inputId))
          .isInstanceOf(ResourceNotFoundException.class);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
    }

    @Test
    void getById() {
      var inputId = 42L;
      var resultEntity = new TestSequenceIdModel(1337L);

      when(repositoryMock.findById(inputId)).thenReturn(Optional.of(resultEntity));

      assertThat(testSubject.getById(inputId)).isSameAs(resultEntity);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
    }
  }

  @Test
  void create() {
    var inputEntity = 42L;
    var savedEntity = mock(TestSequenceIdModel.class);
    final TestSequenceIdModel[] entityToSave = new TestSequenceIdModel[1];

    when(repositoryMock.save(any()))
        .thenAnswer(
            invocationOnMock -> {
              entityToSave[0] = invocationOnMock.getArgument(0);

              return savedEntity;
            });

    assertThat(testSubject.create(inputEntity)).isSameAs(savedEntity);
    assertThat(entityToSave[0].identifier).isEqualTo(inputEntity);

    Mockito.verify(repositoryMock, Mockito.times(1)).save(entityToSave[0]);
  }

  @Nested
  class Update {

    @Test
    void idDoesNotMatch() {
      var inputId = 42L;
      var inputEntity = new TestSequenceIdModel(43L);
      inputEntity.setId(inputId);

      inputEntity.setId(43L);

      when(repositoryMock.findById(inputId)).thenReturn(Optional.of(inputEntity));

      assertThatThrownBy(() -> testSubject.update(inputId, 43L))
          .isInstanceOf(ResourceUpdateException.class);
    }

    @Test
    void entityNotFound() {
      var inputId = 42L;

      when(repositoryMock.existsById(inputId)).thenReturn(false);

      assertThatThrownBy(() -> testSubject.update(inputId, 42L))
          .isInstanceOf(ResourceNotFoundException.class);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
    }

    @Test
    void update() {
      var inputId = 42L;
      var inputEntity = new TestSequenceIdModel(inputId);
      inputEntity.setId(inputId);
      var savedEntity = mock(TestSequenceIdModel.class);

      when(repositoryMock.findById(inputId)).thenReturn(Optional.ofNullable(savedEntity));
      when(repositoryMock.save(inputEntity)).thenReturn(savedEntity);

      assertThat(testSubject.update(inputId, 42L)).isSameAs(savedEntity);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
      Mockito.verify(repositoryMock, Mockito.times(1)).save(inputEntity);
    }
  }

  @Nested
  class Patch {
    @Test
    void entityNotFound() {
      var inputId = 42L;
      var inputMap = new HashMap<String, Object>();

      when(repositoryMock.existsById(inputId)).thenReturn(false);

      assertThatThrownBy(() -> testSubject.patch(inputId, inputMap))
          .isInstanceOf(ResourceNotFoundException.class);

      Mockito.verify(repositoryMock, Mockito.times(0)).getReferenceById(inputId);
    }

    @Test
    void unknownField() {
      var inputId = 42L;
      var inputMap = new HashMap<String, Object>();
      var databaseEntity = new TestSequenceIdModel(4711L);

      inputMap.put("TOTALLY_UNKNOWN!!!", "Dont care");
      when(repositoryMock.existsById(inputId)).thenReturn(true);
      when(repositoryMock.findById(inputId)).thenReturn(Optional.of(databaseEntity));

      assertThatThrownBy(() -> testSubject.patch(inputId, inputMap))
          .isInstanceOf(ResourceUpdateException.class);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
    }

    @Test
    void patch() {
      var inputId = 42L;
      var inputMap = new HashMap<String, Object>();
      var databaseEntity = new TestSequenceIdModel(4711L);

      inputMap.put("identifier", 42L);

      when(repositoryMock.existsById(inputId)).thenReturn(true);
      when(repositoryMock.findById(inputId)).thenReturn(Optional.of(databaseEntity));
      when(repositoryMock.save(any(TestSequenceIdModel.class))).thenAnswer(i -> i.getArgument(0));

      final var patchResult = testSubject.patch(inputId, inputMap);
      assertThat(patchResult.identifier).isEqualTo(42L);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
      Mockito.verify(repositoryMock, Mockito.times(1)).save(any(TestSequenceIdModel.class));
    }
  }

  @Nested
  class delete {
    @Test
    void entityNotFound() {
      var inputId = 42L;

      when(repositoryMock.findById(inputId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.deleteById(inputId))
          .isInstanceOf(ResourceNotFoundException.class);

      Mockito.verify(repositoryMock, Mockito.times(1)).existsById(inputId);
    }

    @Test
    void deleteById() {
      var inputId = 42L;

      when(repositoryMock.existsById(inputId)).thenReturn(true);
      Mockito.doNothing().when(repositoryMock).deleteById(inputId);

      testSubject.deleteById(inputId);

      Mockito.verify(repositoryMock, Mockito.times(1)).deleteById(inputId);
    }
  }

  @Nested
  class ConvertFieldValue {

    @Test
    void shouldReturnNullWhenValueIsNull() {
      Object result = testSubject.convertFieldValue(String.class, null);
      assertThat(result).isNull();
    }

    @Test
    void shouldReturnValueWhenAlreadyOfTargetType() {
      String value = "test";
      Object result = testSubject.convertFieldValue(String.class, value);
      assertThat(result).isEqualTo(value);
    }

    @Test
    void shouldReturnNonStringValueAsIs() {
      Integer value = 42;
      Object result = testSubject.convertFieldValue(String.class, value);
      assertThat(result).isEqualTo(value);
    }

    @Nested
    class LocaleConversion {
      @Test
      void shouldConvertStringToLocale() {
        Object result = testSubject.convertFieldValue(Locale.class, "de-DE");
        assertThat(result).isInstanceOf(Locale.class);
        assertThat(result).isEqualTo(Locale.forLanguageTag("de-DE"));
      }
    }

    @Nested
    class UUIDConversion {
      @Test
      void shouldConvertStringToUUID() {
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        Object result = testSubject.convertFieldValue(UUID.class, uuidString);
        assertThat(result).isInstanceOf(UUID.class);
        assertThat(result).isEqualTo(UUID.fromString(uuidString));
      }

      @Test
      void shouldThrowExceptionForInvalidUUID() {
        assertThatThrownBy(() -> testSubject.convertFieldValue(UUID.class, "invalid-uuid"))
            .isInstanceOf(ResourceUpdateException.class)
            .hasMessageContaining("Failed to convert value 'invalid-uuid' to type UUID");
      }
    }

    @Nested
    class DateTimeConversions {
      @Test
      void shouldConvertStringToLocalDateTime() {
        String dateTimeString = "2023-11-08T10:15:30";
        Object result = testSubject.convertFieldValue(LocalDateTime.class, dateTimeString);
        assertThat(result).isInstanceOf(LocalDateTime.class);
        assertThat(result).isEqualTo(LocalDateTime.parse(dateTimeString));
      }

      @Test
      void shouldConvertStringToLocalDate() {
        String dateString = "2023-11-08";
        Object result = testSubject.convertFieldValue(LocalDate.class, dateString);
        assertThat(result).isInstanceOf(LocalDate.class);
        assertThat(result).isEqualTo(LocalDate.parse(dateString));
      }

      @Test
      void shouldConvertStringToLocalTime() {
        String timeString = "10:15:30";
        Object result = testSubject.convertFieldValue(LocalTime.class, timeString);
        assertThat(result).isInstanceOf(LocalTime.class);
        assertThat(result).isEqualTo(LocalTime.parse(timeString));
      }

      @Test
      void shouldConvertStringToInstant() {
        String instantString = "2023-11-08T10:15:30Z";
        Object result = testSubject.convertFieldValue(Instant.class, instantString);
        assertThat(result).isInstanceOf(Instant.class);
        assertThat(result).isEqualTo(Instant.parse(instantString));
      }

      @Test
      void shouldConvertStringToZonedDateTime() {
        String zonedDateTimeString = "2023-11-08T10:15:30+01:00[Europe/Berlin]";
        Object result = testSubject.convertFieldValue(ZonedDateTime.class, zonedDateTimeString);
        assertThat(result).isInstanceOf(ZonedDateTime.class);
        assertThat(result).isEqualTo(ZonedDateTime.parse(zonedDateTimeString));
      }

      @Test
      void shouldConvertStringToOffsetDateTime() {
        String offsetDateTimeString = "2023-11-08T10:15:30+01:00";
        Object result = testSubject.convertFieldValue(OffsetDateTime.class, offsetDateTimeString);
        assertThat(result).isInstanceOf(OffsetDateTime.class);
        assertThat(result).isEqualTo(OffsetDateTime.parse(offsetDateTimeString));
      }
    }

    @Nested
    class NumberConversions {
      @Test
      void shouldConvertStringToInteger() {
        Object result = testSubject.convertFieldValue(Integer.class, "42");
        assertThat(result).isInstanceOf(Integer.class);
        assertThat(result).isEqualTo(42);
      }

      @Test
      void shouldConvertStringToPrimitiveInt() {
        Object result = testSubject.convertFieldValue(int.class, "42");
        assertThat(result).isInstanceOf(Integer.class);
        assertThat(result).isEqualTo(42);
      }

      @Test
      void shouldConvertStringToLong() {
        Object result = testSubject.convertFieldValue(Long.class, "1234567890");
        assertThat(result).isInstanceOf(Long.class);
        assertThat(result).isEqualTo(1234567890L);
      }

      @Test
      void shouldConvertStringToPrimitiveLong() {
        Object result = testSubject.convertFieldValue(long.class, "1234567890");
        assertThat(result).isInstanceOf(Long.class);
        assertThat(result).isEqualTo(1234567890L);
      }

      @Test
      void shouldConvertStringToDouble() {
        Object result = testSubject.convertFieldValue(Double.class, "3.14159");
        assertThat(result).isInstanceOf(Double.class);
        assertThat(result).isEqualTo(3.14159);
      }

      @Test
      void shouldConvertStringToPrimitiveDouble() {
        Object result = testSubject.convertFieldValue(double.class, "3.14159");
        assertThat(result).isInstanceOf(Double.class);
        assertThat(result).isEqualTo(3.14159);
      }

      @Test
      void shouldConvertStringToFloat() {
        Object result = testSubject.convertFieldValue(Float.class, "3.14");
        assertThat(result).isInstanceOf(Float.class);
        assertThat(result).isEqualTo(3.14f);
      }

      @Test
      void shouldConvertStringToPrimitiveFloat() {
        Object result = testSubject.convertFieldValue(float.class, "3.14");
        assertThat(result).isInstanceOf(Float.class);
        assertThat(result).isEqualTo(3.14f);
      }

      @Test
      void shouldConvertStringToShort() {
        Object result = testSubject.convertFieldValue(Short.class, "100");
        assertThat(result).isInstanceOf(Short.class);
        assertThat(result).isEqualTo((short) 100);
      }

      @Test
      void shouldConvertStringToPrimitiveShort() {
        Object result = testSubject.convertFieldValue(short.class, "100");
        assertThat(result).isInstanceOf(Short.class);
        assertThat(result).isEqualTo((short) 100);
      }

      @Test
      void shouldConvertStringToByte() {
        Object result = testSubject.convertFieldValue(Byte.class, "42");
        assertThat(result).isInstanceOf(Byte.class);
        assertThat(result).isEqualTo((byte) 42);
      }

      @Test
      void shouldConvertStringToPrimitiveByte() {
        Object result = testSubject.convertFieldValue(byte.class, "42");
        assertThat(result).isInstanceOf(Byte.class);
        assertThat(result).isEqualTo((byte) 42);
      }

      @Test
      void shouldConvertStringToBigDecimal() {
        Object result = testSubject.convertFieldValue(BigDecimal.class, "123.456789");
        assertThat(result).isInstanceOf(BigDecimal.class);
        assertThat(result).isEqualTo(new BigDecimal("123.456789"));
      }

      @Test
      void shouldConvertStringToBigInteger() {
        Object result = testSubject.convertFieldValue(BigInteger.class, "123456789012345");
        assertThat(result).isInstanceOf(BigInteger.class);
        assertThat(result).isEqualTo(new BigInteger("123456789012345"));
      }

      @Test
      void shouldThrowExceptionForInvalidNumber() {
        assertThatThrownBy(() -> testSubject.convertFieldValue(Integer.class, "not-a-number"))
            .isInstanceOf(ResourceUpdateException.class)
            .hasMessageContaining("Failed to convert value 'not-a-number' to type Integer");
      }
    }

    @Nested
    class BooleanConversion {
      @Test
      void shouldConvertStringToBoolean() {
        Object resultTrue = testSubject.convertFieldValue(Boolean.class, "true");
        assertThat(resultTrue).isInstanceOf(Boolean.class);
        assertThat(resultTrue).isEqualTo(true);

        Object resultFalse = testSubject.convertFieldValue(Boolean.class, "false");
        assertThat(resultFalse).isInstanceOf(Boolean.class);
        assertThat(resultFalse).isEqualTo(false);
      }

      @Test
      void shouldConvertStringToPrimitiveBoolean() {
        Object result = testSubject.convertFieldValue(boolean.class, "true");
        assertThat(result).isInstanceOf(Boolean.class);
        assertThat(result).isEqualTo(true);
      }
    }

    @Nested
    class CharacterConversion {
      @Test
      void shouldConvertStringToCharacter() {
        Object result = testSubject.convertFieldValue(Character.class, "A");
        assertThat(result).isInstanceOf(Character.class);
        assertThat(result).isEqualTo('A');
      }

      @Test
      void shouldConvertStringToPrimitiveChar() {
        Object result = testSubject.convertFieldValue(char.class, "B");
        assertThat(result).isInstanceOf(Character.class);
        assertThat(result).isEqualTo('B');
      }

      @Test
      void shouldThrowExceptionForMultiCharacterString() {
        assertThatThrownBy(() -> testSubject.convertFieldValue(Character.class, "AB"))
            .isInstanceOf(ResourceUpdateException.class)
            .hasMessageContaining("Failed to convert value 'AB' to type Character");
      }
    }

    @Nested
    class URLAndURIConversions {
      @Test
      void shouldConvertStringToURL() throws Exception {
        String urlString = "https://example.com";
        Object result = testSubject.convertFieldValue(URL.class, urlString);
        assertThat(result).isInstanceOf(URL.class);
        assertThat(result).isEqualTo(new URL(urlString));
      }

      @Test
      void shouldConvertStringToURI() {
        String uriString = "https://example.com/path";
        Object result = testSubject.convertFieldValue(URI.class, uriString);
        assertThat(result).isInstanceOf(URI.class);
        assertThat(result).isEqualTo(URI.create(uriString));
      }

      @Test
      void shouldThrowExceptionForInvalidURL() {
        assertThatThrownBy(() -> testSubject.convertFieldValue(URL.class, "invalid url"))
            .isInstanceOf(ResourceUpdateException.class)
            .hasMessageContaining("Failed to convert value 'invalid url' to type URL");
      }
    }

    @Nested
    class EnumConversion {
      enum TestEnum {
        VALUE_ONE,
        VALUE_TWO
      }

      @Test
      void shouldConvertStringToEnum() {
        Object result = testSubject.convertFieldValue(TestEnum.class, "VALUE_ONE");
        assertThat(result).isInstanceOf(TestEnum.class);
        assertThat(result).isEqualTo(TestEnum.VALUE_ONE);
      }

      @Test
      void shouldThrowExceptionForInvalidEnumValue() {
        assertThatThrownBy(() -> testSubject.convertFieldValue(TestEnum.class, "INVALID_VALUE"))
            .isInstanceOf(ResourceUpdateException.class)
            .hasMessageContaining("Failed to convert value 'INVALID_VALUE' to type TestEnum");
      }
    }

    @Nested
    class UnsupportedConversions {
      @Test
      void shouldReturnOriginalValueForUnsupportedType() {
        class CustomClass {}
        String value = "some-value";
        Object result = testSubject.convertFieldValue(CustomClass.class, value);
        assertThat(result).isEqualTo(value);
      }
    }
  }

  static class TestImpl extends AbstractEntityService<TestSequenceIdModel, Long, Long> {

    TestImpl(final @NotNull BaseRepository<TestSequenceIdModel, Long> repository) {
      super(repository);
    }

    @NotNull
    @Override
    protected <E extends Long> AbstractEntityServiceTest.TestSequenceIdModel convertDtoToEntity(
        @NotNull final E entity, Optional<TestSequenceIdModel> currentEntityOpt) {
      final var model = new TestSequenceIdModel(entity);
      model.setId(entity);

      return model;
    }

    // Make convertFieldValue accessible for testing
    @Override
    public Object convertFieldValue(@NotNull Class<?> targetType, @Nullable Object value) {
      return super.convertFieldValue(targetType, value);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @AllArgsConstructor
  static class TestSequenceIdModel extends SequenceIdModel {
    public Long identifier;

    @Override
    public String getTitle() {
      return identifier.toString();
    }
  }

  interface TestRepo extends BaseRepository<TestSequenceIdModel, Long> {}
}

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

package de.frachtwerk.essencium.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.SequenceIdModel;
import de.frachtwerk.essencium.backend.model.exception.ResourceCannotDeleteException;
import de.frachtwerk.essencium.backend.model.exception.ResourceCannotFindException;
import de.frachtwerk.essencium.backend.model.exception.ResourceCannotUpdateException;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import java.util.HashMap;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class DefaultEntityServiceImplementationTest {

  private final BaseRepository<TestSequenceIdModel, Long> repositoryMock =
      Mockito.mock(TestRepo.class);
  private final BaseEntityServiceDefaultImplementation<TestSequenceIdModel, Long> testSubject =
      new BaseEntityServiceDefaultImplementation<>(repositoryMock);

  @Test
  void getAll() {
    var pageableMock = Mockito.mock(Pageable.class);
    var resultMock = Mockito.mock(Page.class);
    // noinspection unchecked
    when(resultMock.map(Mockito.any())).thenReturn(resultMock);

    // noinspection unchecked
    when(repositoryMock.findAll(pageableMock)).thenReturn(resultMock);

    assertThat(testSubject.getAll(pageableMock)).isSameAs(resultMock);

    Mockito.verify(repositoryMock, Mockito.times(1)).findAll(pageableMock);
  }

  @Nested
  class GetById {
    @Test
    void entityNotFound() {
      var inputId = 42L;

      when(repositoryMock.findById(inputId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.getById(inputId))
          .isInstanceOf(ResourceCannotFindException.class);

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
    var inputEntity = Mockito.mock(TestSequenceIdModel.class);
    var savedEntity = Mockito.mock(TestSequenceIdModel.class);

    when(repositoryMock.save(inputEntity)).thenReturn(savedEntity);

    assertThat(testSubject.create(inputEntity)).isSameAs(savedEntity);

    Mockito.verify(repositoryMock, Mockito.times(1)).save(inputEntity);
  }

  @Nested
  class Update {

    @Test
    void idDoesNotMatch() {
      var inputId = 42L;
      var inputEntity = new TestSequenceIdModel(43L);

      inputEntity.setId(43L);

      when(repositoryMock.findById(inputId)).thenReturn(Optional.of(inputEntity));

      assertThatThrownBy(() -> testSubject.update(inputId, inputEntity))
          .isInstanceOf(ResourceCannotUpdateException.class);
    }

    @Test
    void entityNotFound() {
      var inputId = 42L;
      var inputEntity = new TestSequenceIdModel(42L);
      inputEntity.setId(inputId);

      when(repositoryMock.existsById(inputId)).thenReturn(false);

      assertThatThrownBy(() -> testSubject.update(inputId, inputEntity))
          .isInstanceOf(ResourceCannotUpdateException.class);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
    }

    @Test
    void update() {
      var inputId = 42L;
      var inputEntity = new TestSequenceIdModel(inputId);
      inputEntity.setId(inputId);
      var savedEntity = Mockito.mock(TestSequenceIdModel.class);

      when(repositoryMock.findById(inputId)).thenReturn(Optional.ofNullable(savedEntity));
      when(repositoryMock.save(inputEntity)).thenReturn(savedEntity);

      assertThat(testSubject.update(inputId, inputEntity)).isSameAs(savedEntity);

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
          .isInstanceOf(ResourceCannotUpdateException.class);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(inputId);
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
          .isInstanceOf(ResourceCannotUpdateException.class);

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

      assertThat(testSubject.patch(inputId, inputMap).getIdentifier()).isEqualTo(42L);

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
          .isInstanceOf(ResourceCannotDeleteException.class);

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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @AllArgsConstructor
  static class TestSequenceIdModel extends SequenceIdModel {
    private Long identifier;

    @Override
    public String getTitle() {
      return identifier.toString();
    }
  }

  interface TestRepo extends BaseRepository<TestSequenceIdModel, Long> {}
}

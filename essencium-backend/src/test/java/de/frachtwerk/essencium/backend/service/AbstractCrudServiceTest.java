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

package de.frachtwerk.essencium.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.frachtwerk.essencium.backend.model.SequenceIdModel;
import de.frachtwerk.essencium.backend.model.exception.ResourceNotFoundException;
import de.frachtwerk.essencium.backend.repository.BaseRepository;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class AbstractCrudServiceTest {

  private final BaseRepository<TestSequenceIdModel, Long> repositoryMock =
      Mockito.mock(TestRepo.class);

  private final Map<String, Object> mockMap = new HashMap<>();
  private final Map<String, Object> callMap = new HashMap<>();

  private final AbstractCrudService<TestSequenceIdModel, Long, String> testSubject =
      new TestImpl(repositoryMock, mockMap, callMap);

  @BeforeEach
  void setUp() {
    mockMap.clear();
    callMap.clear();
  }

  @Test
  void countAll() {
    Mockito.when(repositoryMock.count()).thenReturn(42L);

    assertThat(testSubject.countAll()).isEqualTo(42L);
  }

  @Test
  void getAll() {
    var databasePageMock = Mockito.mock(List.class);
    var postProcessedPageMock = Mockito.mock(List.class);

    // noinspection unchecked
    Mockito.when(repositoryMock.findAll()).thenReturn(databasePageMock);
    mockMap.put("getAllPostProcessing", postProcessedPageMock);

    assertThat(testSubject.getAll()).isSameAs(postProcessedPageMock);
    assertThat(callMap.get("getAllPostProcessing")).isSameAs(databasePageMock);

    Mockito.verify(repositoryMock, Mockito.times(1)).findAll();
  }

  @Test
  void getAll_Paged() {
    var inputPageableMock = Mockito.mock(Pageable.class);
    var preprocessedPageableMock = Mockito.mock(Pageable.class);
    var databasePageMock = Mockito.mock(Page.class);
    var postProcessedPageMock = Mockito.mock(Page.class);

    mockMap.put("getAllPagedPreProcessing", preprocessedPageableMock);
    // noinspection unchecked
    Mockito.when(repositoryMock.findAll(preprocessedPageableMock)).thenReturn(databasePageMock);
    mockMap.put("getAllPagedPostProcessing", postProcessedPageMock);

    assertThat(testSubject.getAll(inputPageableMock)).isSameAs(postProcessedPageMock);
    assertThat(callMap.get("getAllPagedPreProcessing")).isSameAs(inputPageableMock);
    assertThat(callMap.get("getAllPagedPostProcessing")).isSameAs(databasePageMock);

    Mockito.verify(repositoryMock, Mockito.times(1)).findAll(preprocessedPageableMock);
  }

  @Nested
  class GetById {
    @Test
    void entityNotFound() {
      var inputId = 42L;
      var preProcessedId = 4711L;

      mockMap.put("getByIdPreProcessing", preProcessedId);
      Mockito.when(repositoryMock.findById(preProcessedId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> testSubject.getById(inputId))
          .isInstanceOf(ResourceNotFoundException.class);
      assertThat(callMap.get("getByIdPreProcessing")).isSameAs(inputId);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(preProcessedId);
    }

    @Test
    void getById() {
      var inputId = 42L;
      var preProcessedId = 4711L;
      var databaseEntity = new TestSequenceIdModel(String.valueOf(preProcessedId));
      var postProcessedEntity = new TestSequenceIdModel("1337");

      mockMap.put("getByIdPreProcessing", preProcessedId);
      Mockito.when(repositoryMock.findById(preProcessedId)).thenReturn(Optional.of(databaseEntity));
      mockMap.put("getByIdPostProcessing", postProcessedEntity);

      assertThat(testSubject.getById(inputId)).isSameAs(postProcessedEntity);
      assertThat(callMap.get("getByIdPreProcessing")).isSameAs(inputId);
      assertThat(callMap.get("getByIdPostProcessing")).isSameAs(databaseEntity);

      Mockito.verify(repositoryMock, Mockito.times(1)).findById(preProcessedId);
    }
  }

  @Test
  void create() {
    var inputDTO = "42";
    var preProcessedEntity = new TestSequenceIdModel(inputDTO);
    var databaseEntity = new TestSequenceIdModel(String.valueOf(preProcessedEntity));
    var postProcessedEntity = new TestSequenceIdModel("1337");

    mockMap.put("createPreProcessing", preProcessedEntity);
    Mockito.when(repositoryMock.save(preProcessedEntity)).thenReturn(databaseEntity);
    mockMap.put("createPostProcessing", postProcessedEntity);

    assertThat(testSubject.create(inputDTO)).isSameAs(postProcessedEntity);
    assertThat(callMap.get("createPreProcessing")).isSameAs(inputDTO);
    assertThat(callMap.get("createPostProcessing")).isSameAs(databaseEntity);

    Mockito.verify(repositoryMock, Mockito.times(1)).save(preProcessedEntity);
  }

  @Test
  void update() {
    var inputId = 42L;
    var inputDTO = "42";
    var preProcessedEntity = new TestSequenceIdModel(inputDTO);
    var databaseEntity = new TestSequenceIdModel(String.valueOf(preProcessedEntity));
    var postProcessedEntity = new TestSequenceIdModel("1337");

    mockMap.put("updatePreProcessing", preProcessedEntity);
    Mockito.when(repositoryMock.save(preProcessedEntity)).thenReturn(databaseEntity);
    mockMap.put("updatePostProcessing", postProcessedEntity);

    assertThat(testSubject.update(inputId, inputDTO)).isSameAs(postProcessedEntity);
    assertThat(callMap.get("updatePreProcessing_id")).isSameAs(inputId);
    assertThat(callMap.get("updatePreProcessing_entity")).isSameAs(inputDTO);
    assertThat(callMap.get("updatePostProcessing")).isSameAs(databaseEntity);

    Mockito.verify(repositoryMock, Mockito.times(1)).save(preProcessedEntity);
  }

  @Test
  void patch() {
    var inputId = 42L;
    var inputMap = new HashMap<String, Object>();
    var preProcessedEntity = new TestSequenceIdModel(String.valueOf(inputId));
    var databaseEntity = new TestSequenceIdModel(String.valueOf(preProcessedEntity));
    var postProcessedEntity = new TestSequenceIdModel("1337");

    mockMap.put("patchPreProcessing", preProcessedEntity);
    Mockito.when(repositoryMock.save(preProcessedEntity)).thenReturn(databaseEntity);
    mockMap.put("patchPostProcessing", postProcessedEntity);

    assertThat(testSubject.patch(inputId, inputMap)).isSameAs(postProcessedEntity);
    assertThat(callMap.get("patchPreProcessing_id")).isSameAs(inputId);
    assertThat(callMap.get("patchPreProcessing_entity")).isSameAs(inputMap);
    assertThat(callMap.get("patchPostProcessing")).isSameAs(databaseEntity);

    Mockito.verify(repositoryMock, Mockito.times(1)).save(preProcessedEntity);
  }

  @Test
  void deleteById() {
    var inputId = 42L;

    Mockito.doNothing().when(repositoryMock).deleteById(inputId);

    testSubject.deleteById(inputId);
    assertThat(callMap.get("deleteById")).isSameAs(inputId);
    Mockito.verify(repositoryMock, Mockito.times(1)).deleteById(inputId);
  }

  @SuppressWarnings("unchecked")
  static class TestImpl extends AbstractCrudService<TestSequenceIdModel, Long, String> {
    private final Map<String, Object> mockMap;
    private final Map<String, Object> callMap;

    TestImpl(
        @NotNull final BaseRepository<TestSequenceIdModel, Long> repository,
        @NotNull final Map<String, Object> mockMap,
        @NotNull final Map<String, Object> callMap) {
      super(repository);
      this.mockMap = mockMap;
      this.callMap = callMap;
    }

    @Override
    protected @NotNull List<TestSequenceIdModel> getAllPostProcessing(
        @NotNull List<TestSequenceIdModel> allEntities) {
      callMap.put("getAllPostProcessing", allEntities);
      return (List<TestSequenceIdModel>) mockMap.get("getAllPostProcessing");
    }

    @Override
    protected @NotNull Pageable getAllPreProcessing(final @NotNull Pageable pageable) {
      callMap.put("getAllPagedPreProcessing", pageable);
      return (Pageable) mockMap.get("getAllPagedPreProcessing");
    }

    @Override
    protected Specification<TestSequenceIdModel> specificationPreProcessing(
        Specification<TestSequenceIdModel> spec) {
      callMap.put("specificationPreProcessing", spec);
      return (Specification<TestSequenceIdModel>) mockMap.get("specificationPreProcessing");
    }

    @Override
    protected @NotNull Page<TestSequenceIdModel> getAllPostProcessing(
        final @NotNull Page<TestSequenceIdModel> page) {
      callMap.put("getAllPagedPostProcessing", page);
      return (Page<TestSequenceIdModel>) mockMap.get("getAllPagedPostProcessing");
    }

    @Override
    protected @NotNull Long getByIdPreProcessing(final @NotNull Long id) {
      callMap.put("getByIdPreProcessing", id);
      return (Long) mockMap.get("getByIdPreProcessing");
    }

    @Override
    protected @NotNull AbstractCrudServiceTest.TestSequenceIdModel getByIdPostProcessing(
        final @NotNull AbstractCrudServiceTest.TestSequenceIdModel entity) {
      callMap.put("getByIdPostProcessing", entity);
      return (TestSequenceIdModel) mockMap.get("getByIdPostProcessing");
    }

    @NotNull
    @Override
    protected <E extends String> AbstractCrudServiceTest.TestSequenceIdModel createPreProcessing(
        @NotNull final E entity) {
      callMap.put("createPreProcessing", entity);
      return (TestSequenceIdModel) mockMap.get("createPreProcessing");
    }

    @NotNull
    @Override
    protected AbstractCrudServiceTest.TestSequenceIdModel createPostProcessing(
        final @NotNull AbstractCrudServiceTest.TestSequenceIdModel saved) {
      callMap.put("createPostProcessing", saved);
      return (TestSequenceIdModel) mockMap.get("createPostProcessing");
    }

    @NotNull
    @Override
    protected <E extends String> AbstractCrudServiceTest.TestSequenceIdModel updatePreProcessing(
        final @NotNull Long id, @NotNull final E entity) {
      callMap.put("updatePreProcessing_id", id);
      callMap.put("updatePreProcessing_entity", entity);
      return (TestSequenceIdModel) mockMap.get("updatePreProcessing");
    }

    @NotNull
    @Override
    protected AbstractCrudServiceTest.TestSequenceIdModel updatePostProcessing(
        final @NotNull AbstractCrudServiceTest.TestSequenceIdModel saved) {
      callMap.put("updatePostProcessing", saved);
      return (TestSequenceIdModel) mockMap.get("updatePostProcessing");
    }

    @NotNull
    @Override
    protected AbstractCrudServiceTest.TestSequenceIdModel patchPreProcessing(
        final @NotNull Long id, final @NotNull Map<String, Object> fieldUpdates) {
      callMap.put("patchPreProcessing_id", id);
      callMap.put("patchPreProcessing_entity", fieldUpdates);
      return (TestSequenceIdModel) mockMap.get("patchPreProcessing");
    }

    @NotNull
    @Override
    protected AbstractCrudServiceTest.TestSequenceIdModel patchPostProcessing(
        final @NotNull AbstractCrudServiceTest.TestSequenceIdModel saved) {
      callMap.put("patchPostProcessing", saved);
      return (TestSequenceIdModel) mockMap.get("patchPostProcessing");
    }

    @Override
    public void deletePreProcessing(final @NotNull Long id) {
      callMap.put("deleteById", id);
    }

    @Override
    protected void deletePostProcessing(@NotNull Long id) {
      callMap.put("deletePostProcessing", id);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @AllArgsConstructor
  static class TestSequenceIdModel extends SequenceIdModel {
    private String identifier;
  }

  interface TestRepo extends BaseRepository<TestSequenceIdModel, Long> {}
}

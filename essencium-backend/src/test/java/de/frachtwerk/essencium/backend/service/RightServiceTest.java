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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RightServiceTest {

  @Mock private RightRepository rightRepository;
  @Mock private RoleService roleService;

  @InjectMocks private RightService rightService;

  @Mock private Pageable pageable;
  @Mock private Page<Right> rightPage;

  @BeforeEach
  void setUp() {
    reset(rightRepository, roleService, pageable, rightPage);
  }

  @Test
  void testGetAll() {
    Right right = new Right();
    when(rightRepository.findAll()).thenReturn(List.of(right));
    List<Right> result = rightService.getAll();
    assertEquals(1, result.size());
    assertSame(right, result.getFirst());
    verify(rightRepository).findAll();
  }

  @Test
  void testGetAllPageable() {
    when(rightRepository.findAll(pageable)).thenReturn(rightPage);
    Page<Right> result = rightService.getAll(pageable);
    assertSame(rightPage, result);
    verify(rightRepository).findAll(pageable);
  }

  @Test
  void testSave() {
    Right right = new Right();
    rightService.save(right);
    verify(rightRepository).save(right);
  }

  @Test
  void testDeleteByAuthority() {
    String authority = "TEST_AUTH";
    Role role = mock(Role.class);
    when(role.getName()).thenReturn("roleName");
    Right right = mock(Right.class);
    when(right.getAuthority()).thenReturn("OTHER_AUTH");
    when(role.getRights()).thenReturn(Set.of(right));
    when(roleService.getByRight(authority)).thenReturn(List.of(role));

    rightService.deleteByAuthority(authority);

    verify(roleService).patch(eq("roleName"), any());
    verify(rightRepository).deleteByAuthority(authority);
  }

  @Test
  void findByAuthorityTest_Present() {
    String authority = "TEST_AUTH";
    Right right = new Right();
    when(rightRepository.findById(authority)).thenReturn(Optional.of(right));
    Optional<Right> result = rightService.findByAuthority(authority);
    assertTrue(result.isPresent());
    assertSame(right, result.get());
    verify(rightRepository, times(1)).findById(authority);
    verifyNoMoreInteractions(rightRepository, roleService);
  }

  @Test
  void findByAuthorityTest_NotPresent() {
    String authority = "TEST_AUTH";
    when(rightRepository.findById(authority)).thenReturn(Optional.empty());
    Optional<Right> result = rightService.findByAuthority(authority);
    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(rightRepository, times(1)).findById(authority);
    verifyNoMoreInteractions(rightRepository, roleService);
  }
}

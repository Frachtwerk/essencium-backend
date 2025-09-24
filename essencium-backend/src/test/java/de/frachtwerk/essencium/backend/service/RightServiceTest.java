package de.frachtwerk.essencium.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.model.Role;
import de.frachtwerk.essencium.backend.repository.RightRepository;
import java.util.List;
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
}

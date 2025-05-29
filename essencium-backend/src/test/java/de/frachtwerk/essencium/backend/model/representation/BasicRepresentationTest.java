package de.frachtwerk.essencium.backend.model.representation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.frachtwerk.essencium.backend.model.AbstractBaseModel;
import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BasicRepresentationTest {

  @Test
  void fromIdAndName() {
    BasicRepresentation basicRepresentation = BasicRepresentation.from(42L, "name");
    assertNotNull(basicRepresentation);
    assertEquals(42L, basicRepresentation.id());
    assertEquals("name", basicRepresentation.name());
  }

  @Test
  void fromIdAndNameNull() {
    BasicRepresentation basicRepresentation = BasicRepresentation.from(null, null);
    assertNull(basicRepresentation);
  }

  @Test
  void fromIdNull() {
    BasicRepresentation basicRepresentation = BasicRepresentation.from(null, "name");
    assertNull(basicRepresentation);
  }

  @Test
  void fromNameNull() {
    BasicRepresentation basicRepresentation = BasicRepresentation.from(42L, null);
    assertNull(basicRepresentation);
  }

  @Test
  void testFromModel() {
    AbstractBaseModel mock = mock(AbstractBaseModel.class);
    when(mock.getId()).thenReturn(42L);
    when(mock.getTitle()).thenReturn("name");

    BasicRepresentation basicRepresentation = BasicRepresentation.from(mock);
    assertNotNull(basicRepresentation);
    assertEquals(42L, basicRepresentation.id());
    assertEquals("name", basicRepresentation.name());
  }

  @Test
  void testFromModelNull() {
    AbstractBaseModel model = null;
    BasicRepresentation basicRepresentation = BasicRepresentation.from(model);
    assertNull(basicRepresentation);
  }

  @Test
  void testFromModelContentNull() {
    AbstractBaseModel mock = mock(AbstractBaseModel.class);
    when(mock.getId()).thenReturn(null);
    when(mock.getTitle()).thenReturn(null);
    BasicRepresentation basicRepresentation = BasicRepresentation.from(mock);
    assertNull(basicRepresentation);
  }

  @Test
  void testFromList() {
    AbstractBaseModel mock = mock(AbstractBaseModel.class);
    when(mock.getId()).thenReturn(42L);
    when(mock.getTitle()).thenReturn("name");
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(List.of(mock));
    assertNotNull(basicRepresentations);
    assertEquals(1, basicRepresentations.size());
    assertEquals(42L, basicRepresentations.getFirst().id());
    assertEquals("name", basicRepresentations.getFirst().name());
  }

  @Test
  void testFromEmptyList() {
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(List.of());
    assertNotNull(basicRepresentations);
    assertTrue(basicRepresentations.isEmpty());
  }

  @Test
  void testFromNullList() {
    List<AbstractBaseModel> list = null;
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(list);
    assertNotNull(basicRepresentations);
    assertTrue(basicRepresentations.isEmpty());
  }

  @Test
  void testFromListOfNull() {
    List<AbstractBaseModel> list = new ArrayList<>();
    list.add(null);
    assertEquals(1, list.size());
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(list);
    assertNotNull(basicRepresentations);
    assertTrue(basicRepresentations.isEmpty());
  }

  @Test
  void testFromSet() {
    AbstractBaseModel mock = mock(AbstractBaseModel.class);
    when(mock.getId()).thenReturn(42L);
    when(mock.getTitle()).thenReturn("name");
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(Set.of(mock));
    assertNotNull(basicRepresentations);
    assertEquals(1, basicRepresentations.size());
    assertEquals(42L, basicRepresentations.getFirst().id());
    assertEquals("name", basicRepresentations.getFirst().name());
  }

  @Test
  void testFromEmptySet() {
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(Set.of());
    assertNotNull(basicRepresentations);
    assertTrue(basicRepresentations.isEmpty());
  }

  @Test
  void testFromNullSet() {
    Set<AbstractBaseModel> set = null;
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(set);
    assertNotNull(basicRepresentations);
    assertTrue(basicRepresentations.isEmpty());
  }

  @Test
  void testFromSetOfNull() {
    Set<AbstractBaseModel> set = new HashSet<>();
    set.add(null);
    assertEquals(1, set.size());
    List<BasicRepresentation> basicRepresentations = BasicRepresentation.from(set);
    assertNotNull(basicRepresentations);
    assertTrue(basicRepresentations.isEmpty());
  }

  @Test
  void testFromUser() {
    AbstractBaseUser user = mock(AbstractBaseUser.class);
    when(user.getId()).thenReturn(42L);
    when(user.getFirstName()).thenReturn("firstName");
    when(user.getLastName()).thenReturn("lastName");
    when(user.getTitle())
        .thenAnswer(invocationOnMock -> user.getFirstName() + " " + user.getLastName());
    BasicRepresentation basicRepresentation = BasicRepresentation.from(user);
    assertNotNull(basicRepresentation);
    assertEquals(42L, basicRepresentation.id());
    assertEquals("firstName lastName", basicRepresentation.name());
  }
}

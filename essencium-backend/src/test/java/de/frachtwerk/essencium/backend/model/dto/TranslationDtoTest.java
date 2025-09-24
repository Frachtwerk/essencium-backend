package de.frachtwerk.essencium.backend.model.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranslationDtoTest {

  @Test
  void compareTo_shouldReturnZero_whenKeysAreEqual() {
    TranslationDto dto1 =
        TranslationDto.builder().locale(Locale.GERMAN).key("testKey").value("Wert1").build();

    TranslationDto dto2 =
        TranslationDto.builder().locale(Locale.ENGLISH).key("testKey").value("Value2").build();

    assertEquals(0, dto1.compareTo(dto2));
  }

  @Test
  void compareTo_shouldReturnNegative_whenKeyIsLessThanOther() {
    TranslationDto dto1 =
        TranslationDto.builder().locale(Locale.GERMAN).key("aKey").value("Wert1").build();

    TranslationDto dto2 =
        TranslationDto.builder().locale(Locale.ENGLISH).key("bKey").value("Value2").build();

    assertTrue(dto1.compareTo(dto2) < 0);
  }

  @Test
  void compareTo_shouldReturnPositive_whenKeyIsGreaterThanOther() {
    TranslationDto dto1 =
        TranslationDto.builder().locale(Locale.GERMAN).key("zKey").value("Wert1").build();

    TranslationDto dto2 =
        TranslationDto.builder().locale(Locale.ENGLISH).key("aKey").value("Value2").build();

    assertTrue(dto1.compareTo(dto2) > 0);
  }
}

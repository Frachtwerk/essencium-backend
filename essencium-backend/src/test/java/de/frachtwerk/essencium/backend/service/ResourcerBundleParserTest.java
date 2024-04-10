package de.frachtwerk.essencium.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import de.frachtwerk.essencium.backend.model.Translation;
import de.frachtwerk.essencium.backend.model.exception.TranslationFileException;
import de.frachtwerk.essencium.backend.service.translation.ResourceBundleParser;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ResourceBundleParserTest {

  private final ResourceBundleParser parser = new ResourceBundleParser();

  @Test
  void parseInvalidLine() {
    // Simulando uma linha inválida no arquivo de recursos
    String invalidLine = "key_without_value";

    // Criando um InputStream com a linha inválida
    InputStream inputStream =
        new ByteArrayInputStream(invalidLine.getBytes(StandardCharsets.UTF_8));

    // Chamando o método parse com o InputStream e uma Locale válida
    // O teste deve lançar uma TranslationFileException devido à linha inválida
    assertThrows(TranslationFileException.class, () -> parser.parse(inputStream, Locale.ENGLISH));
  }

  @Test
  void parseInvalidEncoding() {
    // Simulando uma linha com caracteres inválidos para o UTF-8
    String invalidLine =
        "Inválido: \uD83D\uDE00"; // Emojis, que podem causar problemas de decodificação

    // Criando um InputStream com a linha inválida
    InputStream inputStream =
        new ByteArrayInputStream(invalidLine.getBytes(StandardCharsets.ISO_8859_1));

    // Chamando o método parse com o InputStream e uma Locale válida
    // O teste deve lançar uma TranslationFileException devido à falha na decodificação
    assertThrows(TranslationFileException.class, () -> parser.parse(inputStream, Locale.ENGLISH));
  }

  @Test
  void parseEmptyFile() {
    // Simulando um arquivo de recursos vazio
    String emptyFileContent = "";

    // Criando um InputStream com o conteúdo do arquivo vazio
    InputStream inputStream =
        new ByteArrayInputStream(emptyFileContent.getBytes(StandardCharsets.UTF_8));

    // Chamando o método parse com o InputStream e uma Locale válida
    Collection<Translation> translations = parser.parse(inputStream, Locale.ENGLISH);

    // Verificando se a coleção de traduções está vazia
    assertEquals(Collections.emptyList(), translations);
  }

  @Test
  void parseInvalidLineFormat() {
    // Simulando uma linha com formato inválido no arquivo de recursos
    String invalidLine = "chave_sem_valor";

    // Criando um InputStream com a linha com formato inválido
    InputStream inputStream =
        new ByteArrayInputStream(invalidLine.getBytes(StandardCharsets.UTF_8));

    // Chamando o método parse com o InputStream e uma Locale válida
    // O teste deve lançar uma TranslationFileException devido ao formato inválido da linha
    assertThrows(TranslationFileException.class, () -> parser.parse(inputStream, Locale.ENGLISH));
  }
}

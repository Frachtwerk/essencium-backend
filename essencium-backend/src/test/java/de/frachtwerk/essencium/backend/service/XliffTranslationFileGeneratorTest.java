package de.frachtwerk.essencium.backend.service;

import de.frachtwerk.essencium.backend.service.translation.TranslationService;
import de.frachtwerk.essencium.backend.service.translation.XliffTranslationFileGenerator;
import de.frachtwerk.essencium.backend.repository.TranslationRepository;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class XliffTranslationFileGeneratorTest {

    private final TranslationRepository mockRepository = mock(TranslationRepository.class);
    private final TranslationService translationService = new TranslationService(mockRepository);
    private final XliffTranslationFileGenerator generator = new XliffTranslationFileGenerator(translationService);

    @Test
    void createLocaleTranslationFile() {
        Locale locale = Locale.ENGLISH;
        byte[] translationFile = generator.createLocaleTranslationFile(locale, true);

        assertNotNull(translationFile);
        assertTrue(translationFile.length > 0);

    }

    @Test
    void createLocaleTranslationFile_NoCache() {
        Locale locale = Locale.GERMAN;
        byte[] translationFile = generator.createLocaleTranslationFile(locale, false);

        assertNotNull(translationFile);
        assertTrue(translationFile.length > 0);
    }

    @Test
    void createLocaleTranslationFile_WithCache() {
        Locale locale = Locale.FRENCH;
        byte[] translationFile = generator.createLocaleTranslationFile(locale, true);

        assertNotNull(translationFile);
        assertTrue(translationFile.length > 0);
    }

}

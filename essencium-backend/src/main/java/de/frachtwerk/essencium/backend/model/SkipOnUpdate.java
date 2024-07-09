package de.frachtwerk.essencium.backend.model;

import de.frachtwerk.essencium.backend.service.AbstractEntityService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link SkipOnUpdate} annotation can be used at fields of entity classes, which will be
 * processed via a subclass of the {@link AbstractEntityService}. If the {@link SkipOnUpdate}
 * annotation is present at a field, the field will be skipped and therefore <strong>not</strong>
 * updated during the execution of {@link AbstractEntityService#updateField(AbstractBaseModel,
 * String, Object)}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipOnUpdate {
  /**
   * The {@code ignoreProperty} can be set to a {@link String}, which represents a property key of
   * the application properties. The value of the property key should be a {@link Boolean} value
   * like {@code true} or {@code false}. If the value of {@code ignoreProperty} is present and the
   * value of the key results in <i>"true"</i> the {@link SkipOnUpdate} logic is overruled, and the
   * update on the field will be applied.
   *
   * @return the property key as {@link String}
   */
  String ignoreProperty() default "";
}

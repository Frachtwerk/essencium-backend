package de.frachtwerk.essencium.backend.controller.access;

import java.lang.annotation.*;

/**
 * This annotation can be used on REST controllers to specify the entity type that is served by the
 * REST controller. This is necessary for the {@link RestrictAccessToOwnedEntities} annotation on
 * entity level to work.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExposesEntity {
  /**
   * @return
   */
  Class<?> value();
}

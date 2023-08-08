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

package de.frachtwerk.essencium.backend.controller.access;

import de.frachtwerk.essencium.backend.model.AbstractBaseUser;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import net.kaczmarzyk.spring.data.jpa.web.Utils.Resolvers;
import net.kaczmarzyk.spring.data.jpa.web.WebRequestProcessingContext;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Join;
import net.kaczmarzyk.spring.data.jpa.web.annotation.MissingPathVarPolicy;
import net.kaczmarzyk.spring.data.jpa.web.annotation.OnTypeMismatch;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.expression.ParseException;

@AllArgsConstructor
public class SimpleSpecFactory<USER extends AbstractBaseUser<ID>, ID extends Serializable> {
  private final Resolvers resolvers;
  private final List<Specification<Object>> specs;
  private final WebRequestProcessingContext context;
  private final USER user;
  private final EmbeddedValueResolver embeddedValueResolver;

  public Spec getSimpleAccessSpec(final OwnershipSpec ownershipSpec)
      throws NoSuchFieldException, IllegalAccessException {
    addJoins(ownershipSpec.joins());

    String value =
        getValue(
            ownershipSpec.constVal(),
            ownershipSpec.userAttribute(),
            user,
            ownershipSpec.valueInSpEL());
    return createSpecAnnotation(ownershipSpec.spec(), value, ownershipSpec.path());
  }

  private void addJoins(final Join[] joins) {
    Stream.of(joins)
        .map(j -> resolvers.resolve(j, context))
        .flatMap(Optional::stream)
        .forEach(specs::add);
  }

  private String getValue(
      final String[] constVal, final String userAttribute, USER user, boolean valueInSpEL)
      throws NoSuchFieldException, IllegalAccessException {
    if (constVal.length == 0) {
      // get specified user attribute
      final Field field = getField(user, userAttribute);
      field.setAccessible(true);
      return field.get(user).toString();
    } else if (valueInSpEL) {
      return evaluateRawSpELValue(constVal[0]);
    } else {
      return constVal[0];
    }
  }

  private String evaluateRawSpELValue(String rawSpELValue) {
    try {
      return embeddedValueResolver.resolveStringValue(rawSpELValue);
    } catch (BeansException | ParseException e) {
      throw new IllegalArgumentException("Invalid SpEL expression: '" + rawSpELValue + "'");
    }
  }

  private static Field getField(@NotNull final Object obj, @NotNull String fieldName)
      throws NoSuchFieldException {
    Class<?> cls = obj.getClass();
    while (cls != null) {
      try {
        return cls.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        cls = cls.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }

  @SuppressWarnings("rawtypes")
  private static Spec createSpecAnnotation(
      final Class<? extends Specification> spec, String value, String path) {
    return new Spec() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Spec.class;
      }

      @Override
      public String[] params() {
        return new String[] {};
      }

      @Override
      public char paramSeparator() {
        return 0;
      }

      @Override
      public String[] pathVars() {
        return new String[] {};
      }

      @Override
      public String[] headers() {
        return new String[] {};
      }

      @Override
      public String[] jsonPaths() {
        return new String[] {};
      }

      @Override
      public String[] config() {
        return new String[] {};
      }

      @Override
      public String[] constVal() {
        return new String[] {value};
      }

      @Override
      public String[] defaultVal() {
        return new String[] {};
      }

      @Override
      public boolean valueInSpEL() {
        return false;
      }

      @Override
      public boolean paramsInSpEL() {
        return false;
      }

      @Override
      public OnTypeMismatch onTypeMismatch() {
        return OnTypeMismatch.EMPTY_RESULT;
      }

      @Override
      public String path() {
        return path;
      }

      @Override
      public MissingPathVarPolicy missingPathVarPolicy() {
        return null;
      }

      @Override
      public Class<? extends Specification> spec() {
        return spec;
      }
    };
  }
}

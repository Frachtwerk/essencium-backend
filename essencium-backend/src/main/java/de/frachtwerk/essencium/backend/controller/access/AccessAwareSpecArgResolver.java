/*
 * Copyright (C) 2024 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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
import de.frachtwerk.essencium.backend.model.dto.UserDto;
import de.frachtwerk.essencium.backend.service.AbstractUserService;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;
import net.kaczmarzyk.spring.data.jpa.web.Utils;
import net.kaczmarzyk.spring.data.jpa.web.WebRequestProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public class AccessAwareSpecArgResolver<
        USER extends AbstractBaseUser<ID>, ID extends Serializable, USERDTO extends UserDto<ID>>
    extends SpecificationArgumentResolver {
  private static final Logger LOG = LoggerFactory.getLogger(AccessAwareSpecArgResolver.class);

  private final AbstractUserService<USER, ID, USERDTO> userService;
  private final AbstractApplicationContext applicationContext;

  public AccessAwareSpecArgResolver(
      final AbstractApplicationContext applicationContext,
      final AbstractUserService<USER, ID, USERDTO> userService) {
    super(applicationContext);
    this.applicationContext = applicationContext;
    this.userService = userService;
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory)
      throws Exception {
    // default Specification
    @SuppressWarnings("unchecked")
    final Specification<Object> base =
        (Specification<Object>)
            super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

    List<Specification<Object>> specs = new ArrayList<>();
    if (base != null) {
      specs.add(base);
    }
    Object restrictionSpec = getRestrictionSpec(parameter, webRequest, specs);
    if (Objects.nonNull(restrictionSpec)) {
      return restrictionSpec;
    }
    return base;
  }

  public Object getRestrictionSpec(
      MethodParameter parameter, NativeWebRequest webRequest, List<Specification<Object>> baseList)
      throws Exception {
    // annotation on the method whose parameter should be resolved
    RestrictAccessToOwnedEntities restriction =
        getAnnotation(parameter, RestrictAccessToOwnedEntities.class);

    if (restriction != null) {
      final USER user = userService.getUserFromPrincipal(webRequest.getUserPrincipal());
      final Optional<RestrictAccessToOwnedEntities> r = Optional.of(restriction);
      String[] rights = r.map(RestrictAccessToOwnedEntities::rights).orElse(new String[] {});
      final String[] roles = r.map(RestrictAccessToOwnedEntities::roles).orElse(new String[] {});
      // if user's role should have restricted access
      if (isRestrictionApplyingToUser(rights, roles, user)) {
        LOG.trace("Restriction applies to user.");
        WebRequestProcessingContext context =
            new WebRequestProcessingContext(parameter, webRequest);

        SpecAnnotationFactory<USER, ID> factory =
            new SpecAnnotationFactory<>(applicationContext, context, user, baseList);

        Level level = getLevel(parameter);
        LOG.trace("Found annotations on level {}.", level);

        OwnershipSpec spec = getAnnotation(parameter, OwnershipSpec.class, level);
        if (spec != null) {
          LOG.trace("Found {}.", OwnershipSpec.class.getSimpleName());
          factory.addSpec(spec);
        }
        OwnershipSpec.And and = getAnnotation(parameter, OwnershipSpec.And.class, level);
        if (and != null) {
          LOG.trace("Found {}.", OwnershipSpec.And.class.getSimpleName());
          factory.addSpec(and);
        }
        OwnershipSpec.Or or = getAnnotation(parameter, OwnershipSpec.Or.class, level);
        if (or != null) {
          LOG.trace("Found {}.", OwnershipSpec.Or.class.getSimpleName());
          factory.addSpec(or);
        }
        OwnershipSpec.Disjunction dis =
            getAnnotation(parameter, OwnershipSpec.Disjunction.class, level);
        if (dis != null) {
          LOG.trace("Found {}.", OwnershipSpec.Disjunction.class.getSimpleName());
          factory.addSpec(dis);
        }
        OwnershipSpec.Conjunction con =
            getAnnotation(parameter, OwnershipSpec.Conjunction.class, level);
        if (con != null) {
          LOG.trace("Found {}.", OwnershipSpec.Conjunction.class.getSimpleName());
          factory.addSpec(con);
        }

        return accumulateSpecs(parameter, baseList);
      }
    }
    return null;
  }

  enum Level {
    PARAMETER,
    CLASS,
    ENTITY,
    NONE
  }

  private <T extends Annotation> T getAnnotation(
      MethodParameter parameter, Class<T> annotationClass) {
    T annotation = parameter.getMethodAnnotation(annotationClass);
    final Class<?> containingClass = parameter.getContainingClass();
    if (annotation == null) {
      annotation = containingClass.getAnnotation(annotationClass);
    } else {
      LOG.trace(
          "Found {} on parameter {}.",
          annotationClass.getSimpleName(),
          parameter.getParameterName());
    }
    if (annotation == null) {
      final ExposesEntity ann = containingClass.getAnnotation(ExposesEntity.class);
      if (ann != null) {
        annotation = ann.value().getAnnotation(annotationClass);
        if (annotation != null) {
          LOG.trace(
              "Found {} on entity {}.",
              annotationClass.getSimpleName(),
              ann.value().getSimpleName());
        }
      }
    } else {
      LOG.trace(
          "Found {} on class {}.",
          annotationClass.getSimpleName(),
          containingClass.getSimpleName());
    }
    return annotation;
  }

  private <T extends Annotation> Level getLevel(MethodParameter parameter) {
    final Set<Class<? extends Annotation>> anns =
        Stream.of(
                OwnershipSpec.class,
                OwnershipSpec.And.class,
                OwnershipSpec.Or.class,
                OwnershipSpec.Disjunction.class,
                OwnershipSpec.Conjunction.class)
            .collect(Collectors.toSet());
    if (anns.stream().map(parameter::getMethodAnnotation).anyMatch(Objects::nonNull)) {
      return Level.PARAMETER;
    }
    final Class<?> containingClass = parameter.getContainingClass();
    if (anns.stream().map(containingClass::getAnnotation).anyMatch(Objects::nonNull)) {
      return Level.CLASS;
    }
    final ExposesEntity ann = containingClass.getAnnotation(ExposesEntity.class);
    if (ann != null && anns.stream().map(ann.value()::getAnnotation).anyMatch(Objects::nonNull)) {
      return Level.ENTITY;
    } else {
      return Level.NONE;
    }
  }

  private boolean isRestrictionApplyingToUser(String[] rights, String[] roles, final USER user) {
    return Arrays.stream(roles).anyMatch(s -> user.hasAuthority(() -> s))
        || Stream.of(rights)
            .anyMatch(
                r ->
                    user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(r::equals));
  }

  private <T extends Annotation> T getAnnotation(
      MethodParameter parameter, Class<T> annotationClass, Level level) {
    return switch (level) {
      case PARAMETER -> parameter.getMethodAnnotation(annotationClass);
      case CLASS -> parameter.getContainingClass().getAnnotation(annotationClass);
      case ENTITY -> Optional.ofNullable(
              parameter.getContainingClass().getAnnotation(ExposesEntity.class))
          .map(ann -> ann.value().getAnnotation(annotationClass))
          .orElse(null);
      default -> null;
    };
  }

  private Object accumulateSpecs(MethodParameter parameter, List<Specification<Object>> specs) {
    // accumulate all Specifications
    if (specs.isEmpty()) {
      return null;
    }

    if (specs.size() == 1) {
      Specification<Object> firstSpecification = specs.iterator().next();

      if (Specification.class == parameter.getParameterType()) {
        return firstSpecification;
      } else {
        return Utils.wrapWithIfaceImplementation(parameter.getParameterType(), firstSpecification);
      }
    }

    Specification<Object> con = new net.kaczmarzyk.spring.data.jpa.domain.Conjunction<>(specs);

    return Utils.wrapWithIfaceImplementation(parameter.getParameterType(), con);
  }
}

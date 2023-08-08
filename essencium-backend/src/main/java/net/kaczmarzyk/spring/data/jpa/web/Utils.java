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

package net.kaczmarzyk.spring.data.jpa.web;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Join;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.jpa.domain.Specification;

public class Utils {
  public static SpecificationResolver<Join> joinSpecificationResolver() {
    return new JoinSpecificationResolver();
  }

  public static <T> T wrapWithIfaceImplementation(
      final Class<T> iface, final Specification<Object> targetSpec) {
    return EnhancerUtil.wrapWithIfaceImplementation(iface, targetSpec);
  }

  public static Resolvers getResolvers(
      ConversionService conversionService, AbstractApplicationContext abstractApplicationContext) {
    SimpleSpecificationResolver simpleSpecificationResolver =
        new SimpleSpecificationResolver(
            conversionService, abstractApplicationContext, Locale.getDefault());

    return new Resolvers(
        Stream.of(
                simpleSpecificationResolver,
                new OrSpecificationResolver(simpleSpecificationResolver),
                new DisjunctionSpecificationResolver(simpleSpecificationResolver),
                new ConjunctionSpecificationResolver(simpleSpecificationResolver),
                new AndSpecificationResolver(simpleSpecificationResolver),
                new JoinSpecificationResolver(),
                new JoinFetchSpecificationResolver(),
                new RepeatedJoinFetchResolver(),
                new RepeatedJoinResolver())
            .collect(
                toMap(
                    SpecificationResolver::getSupportedSpecificationDefinition,
                    identity(),
                    (u, v) -> {
                      throw new IllegalStateException(String.format("Duplicate key %s", u));
                    },
                    LinkedHashMap::new)));
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class Resolvers {
    private Map<Object, SpecificationResolver<? extends Annotation>> resolvers;

    @SuppressWarnings("unchecked")
    public Optional<Specification<Object>> resolve(
        Annotation annotation, WebRequestProcessingContext context) {
      @SuppressWarnings("rawtypes")
      final SpecificationResolver resolver = resolvers.get(annotation.annotationType());
      return Optional.of(resolver).map(r -> r.buildSpecification(context, annotation));
    }
  }
}

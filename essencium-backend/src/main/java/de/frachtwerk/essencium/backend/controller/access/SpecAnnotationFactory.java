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
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Function;
import net.kaczmarzyk.spring.data.jpa.web.Utils;
import net.kaczmarzyk.spring.data.jpa.web.Utils.Resolvers;
import net.kaczmarzyk.spring.data.jpa.web.WebRequestProcessingContext;
import net.kaczmarzyk.spring.data.jpa.web.annotation.And;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Conjunction;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Disjunction;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Or;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.data.jpa.domain.Specification;

public class SpecAnnotationFactory<USER extends AbstractBaseUser<ID>, ID extends Serializable> {
  private final Resolvers resolvers;
  private final List<Specification<Object>> specs;
  private final WebRequestProcessingContext context;
  private final SimpleSpecFactory<USER, ID> simpleSpecFactory;

  public SpecAnnotationFactory(
      AbstractApplicationContext applicationContext,
      final WebRequestProcessingContext context,
      USER user,
      List<Specification<Object>> specs) {
    this.specs = specs;
    this.context = context;
    this.resolvers = Utils.getResolvers(null, applicationContext);
    this.simpleSpecFactory =
        new SimpleSpecFactory<>(
            resolvers,
            specs,
            context,
            user,
            new EmbeddedValueResolver(applicationContext.getBeanFactory()));
  }

  public void addSpec(final OwnershipSpec ownershipSpec)
      throws NoSuchFieldException, IllegalAccessException {
    resolveAndAdd(simpleSpecFactory.getSimpleAccessSpec(ownershipSpec));
  }

  public void addSpec(final OwnershipSpec.And and)
      throws NoSuchFieldException, IllegalAccessException {
    resolveAndAdd(getAndAccessSpec(and));
  }

  public void addSpec(final OwnershipSpec.Or or)
      throws NoSuchFieldException, IllegalAccessException {
    resolveAndAdd(getOrAccessSpec(or));
  }

  public void addSpec(final OwnershipSpec.Disjunction dis)
      throws NoSuchFieldException, IllegalAccessException {
    resolveAndAdd(getDisjunctionAccessSpec(dis));
  }

  public void addSpec(final OwnershipSpec.Conjunction con)
      throws NoSuchFieldException, IllegalAccessException {
    resolveAndAdd(getConjunctionAccessSpec(con));
  }

  private And getAndAccessSpec(final OwnershipSpec.And and)
      throws NoSuchFieldException, IllegalAccessException {
    Spec[] result = getSpecs(and.value());
    return new And() {

      @Override
      public Class<? extends Annotation> annotationType() {
        return And.class;
      }

      @Override
      public Spec[] value() {
        return result;
      }
    };
  }

  private Or getOrAccessSpec(final OwnershipSpec.Or and)
      throws NoSuchFieldException, IllegalAccessException {
    Spec[] result = getSpecs(and.value());
    return new Or() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Or.class;
      }

      @Override
      public Spec[] value() {
        return result;
      }
    };
  }

  private Conjunction getConjunctionAccessSpec(final OwnershipSpec.Conjunction con)
      throws NoSuchFieldException, IllegalAccessException {
    Or[] specs = getSpecs(con.value(), this::getOrAccessSpec, Or[]::new);
    final Spec[] and = getSpecs(con.and());
    return new Conjunction() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Conjunction.class;
      }

      @Override
      public Or[] value() {
        return specs;
      }

      @Override
      public Spec[] and() {
        return and;
      }
    };
  }

  private Disjunction getDisjunctionAccessSpec(final OwnershipSpec.Disjunction dis)
      throws NoSuchFieldException, IllegalAccessException {
    And[] specs = getSpecs(dis.value(), this::getAndAccessSpec, And[]::new);
    final Spec[] or = getSpecs(dis.or());
    return new Disjunction() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Disjunction.class;
      }

      @Override
      public And[] value() {
        return specs;
      }

      @Override
      public Spec[] or() {
        return or;
      }
    };
  }

  private Spec[] getSpecs(final OwnershipSpec[] specs)
      throws NoSuchFieldException, IllegalAccessException {
    return getSpecs(specs, simpleSpecFactory::getSimpleAccessSpec, Spec[]::new);
  }

  private <I extends Annotation, O extends Annotation> O[] getSpecs(
      final I[] specs, ThrowingFunction<I, O> resolver, Function<Integer, O[]> arrayInitializer)
      throws NoSuchFieldException, IllegalAccessException {
    O[] result = arrayInitializer.apply(specs.length);
    for (int i = 0; i < specs.length; i++) {
      result[i] = resolver.apply(specs[i]);
    }
    return result;
  }

  @FunctionalInterface
  interface ThrowingFunction<I, O> {
    O apply(I i) throws NoSuchFieldException, IllegalAccessException;
  }

  private <T extends Annotation> void resolveAndAdd(T annotation) {
    resolvers.resolve(annotation, context).ifPresent(specs::add);
  }
}

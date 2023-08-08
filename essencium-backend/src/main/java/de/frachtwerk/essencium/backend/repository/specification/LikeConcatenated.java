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

package de.frachtwerk.essencium.backend.repository.specification;

import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.util.Arrays;
import java.util.Locale;
import lombok.NonNull;
import net.kaczmarzyk.spring.data.jpa.utils.Converter;
import net.kaczmarzyk.spring.data.jpa.utils.QueryContext;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.data.jpa.domain.Specification;

public class LikeConcatenated<T> implements Specification<T> {

  @Serial private static final long serialVersionUID = 1L;

  private final String[] allowedValues;
  private final Converter converter;
  private final String path;
  private final QueryContext queryContext;

  public LikeConcatenated(
      QueryContext queryContext, String path, String[] httpParamValues, Converter converter) {
    if (httpParamValues == null || httpParamValues.length < 1) {
      throw new IllegalArgumentException();
    }
    this.path = path;
    this.allowedValues = httpParamValues;
    this.converter = converter;
    this.queryContext = queryContext;
  }

  @Override
  public Predicate toPredicate(
      @NonNull @NotNull Root<T> root,
      @NonNull @NotNull CriteriaQuery<?> query,
      @NotNull CriteriaBuilder cb) {
    String[] pathElements = this.path.split(",");

    String[] reversedPathElements = Arrays.copyOf(pathElements, pathElements.length);
    String[] reversedAllowedValues = Arrays.copyOf(allowedValues, allowedValues.length);

    ArrayUtils.reverse(reversedPathElements);
    ArrayUtils.reverse(reversedAllowedValues);

    Expression<String> concatPath = createConcatenatedPathExpression(root, cb, pathElements);
    Expression<String> reversedConcatPath =
        createConcatenatedPathExpression(root, cb, reversedPathElements);

    String concatAllowedValues = String.join(" ", allowedValues).toLowerCase(Locale.ROOT);
    String reversedConcatAllowedValues =
        String.join(" ", reversedAllowedValues).toLowerCase(Locale.ROOT);

    return cb.or(
        cb.or(
            cb.like(cb.lower(concatPath), "%" + concatAllowedValues + "%"),
            cb.like(cb.lower(reversedConcatPath), "%" + concatAllowedValues + "%")),
        cb.or(
            cb.like(cb.lower(concatPath), "%" + reversedConcatAllowedValues + "%"),
            cb.like(cb.lower(reversedConcatPath), "%" + reversedConcatAllowedValues + "%")));
  }

  private Expression<String> createConcatenatedPathExpression(
      @NotNull Root<T> root, CriteriaBuilder cb, String[] pathElements) {
    Expression<String> exp = this.path(root, pathElements[0]);
    String[] pathElementsTail = Arrays.copyOfRange(pathElements, 1, pathElements.length);

    for (String element : pathElementsTail) {
      Path<String> path = this.path(root, element);
      exp = cb.concat(exp, " ");
      exp = cb.concat(exp, path);
    }
    return exp;
  }

  @SuppressWarnings("unchecked")
  protected <F> Path<F> path(Root<T> root, String element) {
    Path<?> expr = null;
    for (String field : element.split("\\.")) {
      if (expr == null) {
        if (queryContext != null && queryContext.getEvaluated(field, root) != null) {
          expr = queryContext.getEvaluated(field, root);
        } else {
          expr = root.get(field);
        }
      } else {
        expr = expr.get(field);
      }
    }
    return (Path<F>) expr;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.hashCode(allowedValues);
    result = prime * result + ((converter == null) ? 0 : converter.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    LikeConcatenated<T> other = (LikeConcatenated<T>) obj;
    if (!Arrays.equals(allowedValues, other.allowedValues)) return false;
    if (converter == null) {
      return other.converter == null;
    } else return converter.equals(other.converter);
  }

  @Override
  public String toString() {
    return "LikeIn [allowedValues="
        + Arrays.toString(allowedValues)
        + ", converter="
        + converter
        + "]";
  }
}

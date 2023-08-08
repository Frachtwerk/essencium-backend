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

package de.frachtwerk.essencium.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Types;
import java.util.Locale;
import java.util.Objects;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;
import org.hibernate.annotations.JdbcTypeCode;

@Getter
@Setter
@ToString
@Entity
@IdClass(Translation.TranslationId.class)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Translation implements Comparable<Translation> {
  @Id private Locale locale;
  @Id private String key;

  /* @Lob is currently converted into CLOB by Hibernate which is currently unsupported by PostgreSQL */
  @JdbcTypeCode(Types.LONGVARCHAR)
  private String value;

  @Override
  public int compareTo(@NotNull Translation translation) {
    return key.compareTo(translation.getKey());
  }

  @Data
  @Embeddable
  public static class TranslationId implements Serializable {
    @Id private Locale locale;
    @Id private String key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
    Translation that = (Translation) o;
    return locale != null
        && Objects.equals(locale, that.locale)
        && key != null
        && Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(locale, key);
  }
}

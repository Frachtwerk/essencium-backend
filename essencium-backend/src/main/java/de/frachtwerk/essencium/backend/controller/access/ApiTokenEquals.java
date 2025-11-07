package de.frachtwerk.essencium.backend.controller.access;

import de.frachtwerk.essencium.backend.model.ApiToken;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.security.AdditionalApplicationRights;
import de.frachtwerk.essencium.backend.util.UserUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import net.kaczmarzyk.spring.data.jpa.domain.PathSpecification;
import net.kaczmarzyk.spring.data.jpa.utils.QueryContext;
import org.springframework.lang.NonNull;

public class ApiTokenEquals extends PathSpecification<ApiToken> {
  @Serial private static final long serialVersionUID = 1L;
  private final String paramValue;

  public ApiTokenEquals(QueryContext queryContext, String path, String[] httpParamValues) {
    super(queryContext, path);
    if (httpParamValues == null || httpParamValues.length != 1) {
      throw new IllegalArgumentException();
    }
    this.paramValue = httpParamValues[0];
  }

  @Override
  public Predicate toPredicate(
      @NonNull Root<ApiToken> root,
      CriteriaQuery<?> query,
      @NonNull CriteriaBuilder criteriaBuilder) {
    Optional<EssenciumUserDetails<? extends Serializable>> essenciumUserDetails =
        UserUtil.getUserDetailsFromAuthentication();
    if (essenciumUserDetails.isPresent()) {
      if (UserUtil.hasRight(
          essenciumUserDetails.get(), AdditionalApplicationRights.API_TOKEN_ADMIN.getAuthority())) {
        return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
      }
      return criteriaBuilder.equal(root.get(this.path), this.paramValue);
    }
    return criteriaBuilder.isFalse(criteriaBuilder.literal(true));
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((paramValue == null) ? 0 : paramValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    ApiTokenEquals equal = (ApiTokenEquals) o;
    return Objects.equals(paramValue, equal.paramValue);
  }

  @Override
  public String toString() {
    return "ApiTokenEquals [paramValue=" + paramValue + ", path=" + super.path + "]";
  }
}

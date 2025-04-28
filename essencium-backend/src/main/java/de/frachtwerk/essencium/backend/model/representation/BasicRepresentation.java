package de.frachtwerk.essencium.backend.model.representation;

import de.frachtwerk.essencium.backend.model.TitleConvention;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record BasicRepresentation(Serializable id, String name) {
  public static BasicRepresentation from(Serializable id, String name) {
    if (Objects.isNull(id)) {
      return null;
    } else
    if (Objects.isNull(name)) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    return new BasicRepresentation(id, name);
  }

  public static <M extends TitleConvention<? extends Serializable>> BasicRepresentation from(
      M entity) {
    if (Objects.isNull(entity)) {
      return null;
    }
    return BasicRepresentation.from(entity.getId(), entity.getTitle());
  }

  public static <M extends TitleConvention<? extends Serializable>> List<BasicRepresentation> from(
      Collection<M> list) {
    if (Objects.isNull(list)) {
      return List.of();
    }
    return list.stream().map(BasicRepresentation::from).filter(Objects::nonNull).toList();
  }
}

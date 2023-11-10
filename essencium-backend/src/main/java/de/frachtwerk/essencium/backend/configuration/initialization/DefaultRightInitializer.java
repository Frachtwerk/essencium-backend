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

package de.frachtwerk.essencium.backend.configuration.initialization;

import de.frachtwerk.essencium.backend.model.Right;
import de.frachtwerk.essencium.backend.security.BasicApplicationRight;
import de.frachtwerk.essencium.backend.service.RightService;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

@Configuration
@RequiredArgsConstructor
public class DefaultRightInitializer implements DataInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRightInitializer.class);

  private final RightService rightService;

  @Override
  public int order() {
    return 20;
  }

  /**
   * Providing basic rights used by the essencium-backend library.
   *
   * <p>NOTE: These are non-persistent rights. Persistent rights can be fetched with the provided
   * names of this rights by using the RightService
   *
   * @return a set of non-persistent basic application rights
   */
  public Set<Right> getBasicApplicationRights() {
    return Stream.of(BasicApplicationRight.values())
        .map(r -> new Right(r.name(), r.getDescription()))
        .collect(Collectors.toSet());
  }

  /**
   * Providing additional rights used by the implementing application
   *
   * <p>This function can be overwritten by the implementing application to provide rights the
   * database shall be initialized with on start up if they don't exist.
   *
   * <p>NOTE: the rights must not be persistent!
   *
   * @return set of additional application rights that shall be initialized
   */
  public Set<Right> getAdditionalApplicationRights() {
    return Set.of();
  }

  protected Stream<String> getCombinedRights(Stream<String> methods, String... entities) {
    return methods.flatMap(method -> Stream.of(entities).map(entity -> entity + "_" + method));
  }

  @Override
  public void run() {
    Map<String, Right> existingRights =
        rightService.getAll().stream()
            .collect(Collectors.toMap(Right::getAuthority, Function.identity()));

    final Set<Right> basicRights = getBasicApplicationRights();
    final Set<Right> additionalRights = getAdditionalApplicationRights();
    final Set<Right> allRights =
        Stream.concat(basicRights.stream(), additionalRights.stream()).collect(Collectors.toSet());

    final var basicAuthorities =
        basicRights.stream().map(Right::getAuthority).collect(Collectors.toSet());
    final var additionalAuthorities =
        additionalRights.stream().map(Right::getAuthority).collect(Collectors.toSet());

    final var intersectingAuthority =
        CollectionUtils.findFirstMatch(basicAuthorities, additionalAuthorities);
    if (intersectingAuthority != null) {
      throw new IllegalStateException(
          "Additional right has same authority as basic right[" + intersectingAuthority + "]");
    }

    allRights.stream()
        .filter(r -> !existingRights.containsKey(r.getAuthority()))
        .forEach(
            right -> {
              LOGGER.info("Initializing right [{}]", right.getAuthority());
              rightService.save(right);
            });

    existingRights.values().stream()
        .filter(r -> !allRights.contains(r))
        .map(Right::getAuthority)
        .forEach(
            s -> {
              LOGGER.info("Deleting right [{}]", s);
              rightService.deleteByAuthority(s);
            });
  }
}

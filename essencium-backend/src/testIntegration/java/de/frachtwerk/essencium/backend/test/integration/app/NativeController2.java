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

package de.frachtwerk.essencium.backend.test.integration.app;

import de.frachtwerk.essencium.backend.controller.AbstractAccessAwareController;
import de.frachtwerk.essencium.backend.controller.access.ExposesEntity;
import net.kaczmarzyk.spring.data.jpa.domain.Equal;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/native2")
@ExposesEntity(Native.class)
public class NativeController2
    extends AbstractAccessAwareController.Default<Native, Long, NativeDTO, NativeSpec> {

  @Autowired
  public NativeController2(NativeService service) {
    super(service);
  }

  @Override
  @Secured({"AdminRole", "Test"})
  public Page<Native> findAll(NativeSpec specification, Pageable pageable) {
    return super.findAll(specification, pageable);
  }

  @Secured({"AdminRole", "Test"})
  @GetMapping("/withSpec")
  public Page<Native> findAllWithSpec(
      @Spec(
              params = "prop",
              path = "prop",
              constVal = NativeController.OWNED_BY_ALL_VALUE,
              spec = Equal.class)
          NativeSpec specification,
      Pageable pageable) {
    return super.findAll(specification, pageable);
  }
}

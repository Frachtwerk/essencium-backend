/*
 * Copyright (C) 2025 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
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

package de.frachtwerk.essencium.backend.test.integration.controller;

import de.frachtwerk.essencium.backend.controller.AbstractUserController;
import de.frachtwerk.essencium.backend.controller.access.ExposesEntity;
import de.frachtwerk.essencium.backend.model.dto.EssenciumUserDetails;
import de.frachtwerk.essencium.backend.repository.specification.BaseUserSpec;
import de.frachtwerk.essencium.backend.test.integration.model.TestUser;
import de.frachtwerk.essencium.backend.test.integration.model.assembler.TestUserAssembler;
import de.frachtwerk.essencium.backend.test.integration.model.dto.TestUserDto;
import de.frachtwerk.essencium.backend.test.integration.service.TestUserService;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Primary
@RestController
@RequestMapping("/v1/users")
@ExposesEntity(TestUser.class)
public class TestUserController
    extends AbstractUserController<
        TestUser,
        EssenciumUserDetails<Long>,
        TestUser,
        TestUserDto,
        BaseUserSpec<TestUser, Long>,
        Long> {

  protected TestUserController(TestUserService userService, TestUserAssembler assembler) {
    super(userService, assembler);
  }

  @GetMapping("/token-claims")
  public Map<String, String> testEndpoint(
      @AuthenticationPrincipal EssenciumUserDetails<Long> userDetails) {
    Integer integerClaim =
        userDetails.getAdditionalClaimByKey(TestUser.CLAIM_TEST_INTEGER, Integer.class);
    Long longClaim = userDetails.getAdditionalClaimByKey(TestUser.CLAIM_TEST_LONG, Long.class);
    String stringClaim =
        userDetails.getAdditionalClaimByKey(TestUser.CLAIM_TEST_STRING, String.class);
    Boolean booleanClaim =
        userDetails.getAdditionalClaimByKey(TestUser.CLAIM_TEST_BOOLEAN, Boolean.class);
    Double doubleClaim =
        userDetails.getAdditionalClaimByKey(TestUser.CLAIM_TEST_DOUBLE, Double.class);
    Map mapClaim = userDetails.getAdditionalClaimByKey(TestUser.CLAIM_TEST_MAP, Map.class);

    assert integerClaim != null && integerClaim == 1;
    assert longClaim != null && longClaim == 2L;
    assert stringClaim != null && stringClaim.equals("test");
    assert booleanClaim != null && booleanClaim;
    assert doubleClaim != null && doubleClaim == 3.0;
    assert mapClaim != null && mapClaim.equals(Map.of("key1", "value1", "key2", "value2"));

    HashMap<String, String> map = new HashMap<>();
    map.put("userDetails", userDetails.toString());
    map.put("id", String.valueOf(userDetails.getId()));
    map.put("username", userDetails.getUsername());
    map.put("firstName", userDetails.getFirstName());
    map.put("lastName", userDetails.getLastName());
    map.put("locale", userDetails.getLocale().toLanguageTag());
    map.put(TestUser.CLAIM_TEST_INTEGER, String.valueOf(integerClaim));
    map.put(TestUser.CLAIM_TEST_LONG, String.valueOf(longClaim));
    map.put(TestUser.CLAIM_TEST_STRING, String.valueOf(stringClaim));
    map.put(TestUser.CLAIM_TEST_BOOLEAN, String.valueOf(booleanClaim));
    map.put(TestUser.CLAIM_TEST_DOUBLE, String.valueOf(doubleClaim));
    map.put(TestUser.CLAIM_TEST_MAP, String.valueOf(mapClaim));
    return map;
  }
}

package de.frachtwerk.essencium.backend.api.data.rights;

import de.frachtwerk.essencium.backend.model.Right;

public class TestObjectsRights {
  public Right defaultRight() {
    return Right.builder().authority("RIGHT").description("RIGHT").build();
  }

  public Right rightWithAuthorityAndDescription(String authority) {
    return Right.builder().authority(authority).description(authority).build();
  }
}

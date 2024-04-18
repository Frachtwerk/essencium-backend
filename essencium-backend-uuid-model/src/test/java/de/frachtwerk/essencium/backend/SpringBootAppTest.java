package de.frachtwerk.essencium.backend;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

import org.junit.jupiter.api.Test;

public class SpringBootAppTest {
  @Test
  void main() {
    assertThatNoException().isThrownBy(() -> SpringBootApp.main(new String[] {}));
  }
}

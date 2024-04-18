package de.frachtwerk.essencium.backend;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;

import org.junit.jupiter.api.Test;

class SpringBootAppTest {
  @Test
  void main() {
    assertThatNoException().isThrownBy(() -> SpringBootApp.main(new String[] {}));
  }
}

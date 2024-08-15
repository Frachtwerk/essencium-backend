package de.frachtwerk.essencium.backend.model;

import java.io.Serializable;

public interface TitleConvention<ID extends Serializable> {
  String getTitle();

  ID getId();
}

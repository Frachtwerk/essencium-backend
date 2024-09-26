package de.frachtwerk.essencium.backend.model.exception;

import java.util.Map;

public interface ReportableException {

  Map<String, Object> reportInternals();

  Map<String, Object> reportDebug();
}

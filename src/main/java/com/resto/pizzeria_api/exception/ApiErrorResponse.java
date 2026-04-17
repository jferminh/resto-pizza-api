package com.resto.pizzeria_api.exception;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {
    private final String message;
    private final String codeExtended;
    private Map<String, String> fieldsErrors;
}

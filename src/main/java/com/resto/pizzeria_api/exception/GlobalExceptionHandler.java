package com.resto.pizzeria_api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleApiNotFoundException(
            final ApiNotFoundException ex,
            final HttpServletRequest request) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                ex.getMessage(),
                "CODE_NOT_FOUND",
                new LinkedHashMap<>()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            final MethodArgumentNotValidException ex,
            final HttpServletRequest request) {
        final Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "Erreur validation",
                "CODE_NOT_VALIDATED",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            final ConstraintViolationException ex,
            final HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        ApiErrorResponse resp = new ApiErrorResponse(
                "Erreur constraint",
                "CODE_DB_CONSTRAINT_VIOLATION",
                fieldErrors
        );

        return ResponseEntity.badRequest().body(resp);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolationException(
            final DataIntegrityViolationException ex,
            final HttpServletRequest request) {
        LinkedHashMap fields = new LinkedHashMap<>();

        Throwable root = ex;
        ApiErrorResponse errorResponse = null;

        while (root.getCause() != null) {
            root = root.getCause();

            if (root instanceof org.hibernate.exception.ConstraintViolationException sqlEx) {
                fields = buildConstraintViolationResponse(sqlEx);
                break;
            }
        }

        errorResponse = new ApiErrorResponse(
                "Violation d'intégrité des données : " +
                        "valeur déjà existante ou contrainte SQL non respectée",
                "CODE_DB_INTEGRITY_VIOLATION",
                fields);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonBodyException(
            final HttpMessageNotReadableException ex,
            final HttpServletRequest request) {
        ApiErrorResponse errorResponse = null;

        if (ex.getCause() instanceof InvalidFormatException) {
            errorResponse = new ApiErrorResponse(
                    "Le format de requête n'est pas valide",
                    "CODE_INVALID_FORMAT",
                    new LinkedHashMap<>()
            );
        } else { // UnexpectedEndOfInputException || StreamReadException || MismatchedInputException
            log.error("Erreur: requête Http n'est pas valide", ex);

            errorResponse = new ApiErrorResponse(
                    "Le format de requête n'est pas valide",
                    "CODE_HTTP_NOT_READABLE",
                    new LinkedHashMap<>()
            );
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            final Exception ex,
            final HttpServletRequest request) {
        log.error("Erreur indéfini", ex);

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                "Une erreur interne est survenue",
                "CODE_UNKNOWN",
                new LinkedHashMap<>()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    private LinkedHashMap<String, String> buildConstraintViolationResponse(
            org.hibernate.exception.ConstraintViolationException ex) {
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();

        ConstraintKind kind = ex.getKind();
        String message = "";

        if (kind == null) {
            log.error("Type indéfini");
            return null;
        } else {
            message = databaseReason.get(kind);

            if (message == null) {
                log.error("Message est null pour {}", kind);
                return null;
            }
        }

        String constraint = ex.getConstraintName();
        String name = "";

        if (constraint == null) {
            log.error("Constraint est null");
            return null;
        } else {
            name = databaseEntityToField.get(constraint);

            if (name == null) {
                log.error("Nom de constraint est null pour {}", constraint);
                name = "";
                return null;
            }
        }

        fields.put(name, message);

        return fields;
    }

    private Map<String, String> databaseEntityToField
            = Map.of("dishes.name_dish", "name");

    private Map<ConstraintKind, String> databaseReason
            = Map.of(
            ConstraintKind.UNIQUE, "existe déjà",
            ConstraintKind.NOT_NULL, "ne peut pas être vide"
    );
}

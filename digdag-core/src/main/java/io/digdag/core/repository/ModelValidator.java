package io.digdag.core.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import static java.util.Locale.ENGLISH;

public class ModelValidator
{
    public static ModelValidator builder()
    {
        return new ModelValidator();
    }

    private static final Pattern FIRST_NUMBER_CHAR = Pattern.compile("^[0-9]");

    // Windows path special chars: \ / : * ? " < > |
    // UNIX special chars: ~
    // YAML special char: ! ' " #
    // task mark: +
    // subtask mark: ^
    // unnecessary: % & ( ) ; `
    // allowed: - = $ [ ] { } @ , .
    private static final Pattern RAW_TASK_NAME_CHARS = Pattern.compile("[^a-zA-Z_0-9\\-\\=\\$\\[\\]\\{\\}\\@\\`\\,\\.\\^]");

    private final List<ModelValidationException.Failure> failures = new ArrayList<>();

    private ModelValidator()
    { }

    public ModelValidator check(String fieldName, Object object, boolean expression, String errorMessage)
    {
        if (!expression) {
            failures.add(new ModelValidationException.Failure(fieldName, object, errorMessage));
        }
        return this;
    }

    public ModelValidator checkNotEmpty(String fieldName, String value)
    {
        return check(fieldName, value, !value.isEmpty(), "can't be blank");
    }

    public ModelValidator checkMaxLength(String fieldName, String value, int max)
    {
        return check(fieldName, value, value.length() <= max, "can't be longer than " + max + " characters");
    }

    public ModelValidator checkResourceName(String fieldName, String value)
    {
        checkNotEmpty(fieldName, value);
        check(fieldName, value, !FIRST_NUMBER_CHAR.matcher(value).find(), "can't start with a numeric digit (0-9)");
        checkMaxLength(fieldName, value, 255);
        return this;
    }

    public ModelValidator checkTaskName(String fieldName, String value)
    {
        checkRawTaskName(fieldName, value);
        check(fieldName, value, !value.contains("^"), "can't contain ^ character");
        return this;
    }

    public ModelValidator checkRawTaskName(String fieldName, String value)
    {
        checkNotEmpty(fieldName, value);
        check(fieldName, value, !FIRST_NUMBER_CHAR.matcher(value).find(), "can't start with a digit (0-9)");
        check(fieldName, value, value.startsWith("+"), "must start with '+'");
        Matcher m = RAW_TASK_NAME_CHARS.matcher(value.substring(1));
        if (m.find()) {
            check(fieldName, value, false, "can't contain " + m.group() + " character");
        }
        checkMaxLength(fieldName, value, 255);
        return this;
    }

    public ModelValidator check(Object modelObject, String fieldName, Object object, boolean expression, String errorMessageFormat, Object... objects)
    {
        if (!expression) {
            failures.add(new ModelValidationException.Failure(fieldName, object, String.format(ENGLISH, errorMessageFormat, objects)));
        }
        return this;
    }

    public void validate(String modelType, Object modelObject)
    {
        if (!failures.isEmpty()) {
            throw new ModelValidationException("Validationg "+ modelType + " failed", modelObject, failures);
        }
    }
}

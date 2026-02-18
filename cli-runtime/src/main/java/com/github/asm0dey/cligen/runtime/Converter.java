package com.github.asm0dey.cligen.runtime;

/**
 * Interface for custom converters for CLI option values.
 *
 * @param <T> The target field type.
 */
@SuppressWarnings("unused")
public interface Converter<T> {
    /**
     * Converts a String value from the CLI into the target type.
     *
     * @param value The raw string value.
     * @return The converted value.
     * @throws Exception if conversion fails.
     */
    T convert(String value) throws Exception;
}

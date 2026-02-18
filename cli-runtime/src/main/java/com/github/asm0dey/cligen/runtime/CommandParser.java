package com.github.asm0dey.cligen.runtime;

@SuppressWarnings("unused")
public interface CommandParser<T> {
    ParseResult<T> parse(String[] args) throws ParseException;
    String getHelpText();
}

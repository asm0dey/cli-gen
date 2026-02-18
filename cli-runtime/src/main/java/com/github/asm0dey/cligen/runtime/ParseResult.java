package com.github.asm0dey.cligen.runtime;

import java.util.List;

@SuppressWarnings("unused")
public class ParseResult<T> {
    private final T command;
    private final List<String> remainingArgs;
    
    public ParseResult(T command, List<String> remainingArgs) {
        this.command = command;
        this.remainingArgs = remainingArgs;
    }
    
    public T getCommand() { return command; }
    public List<String> getRemainingArgs() { return remainingArgs; }
}

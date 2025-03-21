package com.classhole.compiler.parser;

public record ParseResult<A>(A result, int nextPos) {}
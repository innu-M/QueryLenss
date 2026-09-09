package com.querylens.command;

public interface Command<T> {
    T execute() throws Exception;
}

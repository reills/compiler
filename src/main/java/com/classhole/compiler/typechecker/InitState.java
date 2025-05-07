package com.classhole.compiler.typechecker;

import java.util.HashSet;
import java.util.Set;

// Track initialized variables for each environment
public class InitState {
    private final Set<String> initialized = new HashSet<>();
    private final InitState parent;

    public InitState() {
        this.parent = null;
    }

    public InitState(InitState parent) {
        this.parent = parent;
    }

    public void initialize(String name) {
        initialized.add(name);
    }

    public boolean isInitialized(String name) {
        if (initialized.contains(name)) return true;
        return parent != null && parent.isInitialized(name);
    }

    // Create a copy of the current state
    public InitState copy() {
        InitState copy = new InitState(parent);
        copy.initialized.addAll(this.initialized);
        return copy;
    }

    // Merge two states - only keep variables initialized in both
    public static InitState merge(InitState a, InitState b) {
        // Both must have same parent
        InitState result = new InitState(a.parent);

        // Only add variables that are initialized in both states
        for (String var : a.initialized) {
            if (b.initialized.contains(var)) {
                result.initialized.add(var);
            }
        }

        return result;
    }
}

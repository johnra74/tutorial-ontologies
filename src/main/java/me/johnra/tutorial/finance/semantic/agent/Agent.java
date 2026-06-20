package me.johnra.tutorial.finance.semantic.agent;

@FunctionalInterface
public interface Agent<I, O> {
    O process(I input);
}

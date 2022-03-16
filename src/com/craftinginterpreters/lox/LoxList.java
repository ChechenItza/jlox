package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

public class LoxList extends LoxInstance {
    private final List<Object> elements;

    LoxList() {
        super(null);
        elements = new ArrayList<>();
    }

    @Override
    Object get(Token name) {
        if (name.lexeme.equals("get")) {
            return new LoxCallable() {
                @Override
                public int arity() {
                    return 1;
                }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    int index = (int)(double)arguments.get(0);
                    return elements.get(index);
                }
            };
        } else if (name.lexeme.equals("set")) {
            return new LoxCallable() {
                @Override
                public int arity() {
                    return 2;
                }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    int index = (int)(double)arguments.get(0);
                    Object value = arguments.get(1);
                    return elements.set(index, value);
                }
            };
        } else if (name.lexeme.equals("length")) {
            return (double) elements.size();
        } else if (name.lexeme.equals("push")) {
            return new LoxCallable() {
                @Override
                public int arity() {
                    return 1;
                }

                @Override
                public Object call(Interpreter interpreter,
                                   List<Object> arguments) {
                    Object value = arguments.get(0);
                    return elements.add(value);
                }
            };
        }

        throw new RuntimeError(name, // [hidden]
                "Undefined property '" + name.lexeme + "'.");
    }

    @Override
    void set(Token name, Object value) {
        throw new RuntimeError(name, "Can't add properties to lists.");
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i != 0) buffer.append(", ");
            buffer.append(elements.get(i));
        }
        buffer.append("]");
        return buffer.toString();
    }
}

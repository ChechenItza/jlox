package com.craftinginterpreters.lox;

import java.util.List;

class LoxAnonFunction implements LoxCallable {
    private final Expr.AnonFunc expression;
    private final Environment closure;
    LoxAnonFunction(Expr.AnonFunc expression, Environment closure) {
        this.expression = expression;
        this.closure = closure;
    }

    @Override
    public Object call(Interpreter interpreter,
                       List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < expression.params.size(); i++) {
            environment.define(expression.params.get(i).lexeme,
                    arguments.get(i));
        }

        try {
            interpreter.executeBlock(expression.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return expression.params.size();
    }

    @Override
    public String toString() {
        return "<anonymous fn>";
    }
}
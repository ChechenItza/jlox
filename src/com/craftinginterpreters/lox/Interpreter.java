package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
        globals.define("input", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return new java.util.Scanner(System.in).nextLine();
            }
        });
        globals.define("compareStr", new LoxCallable() {
           @Override
           public int arity() { return 2; }

           @Override
           public Object call(Interpreter interpreter,
                              List<Object> arguments) {
               return ((String)arguments.get(0)).compareTo((String)arguments.get(1));
           }
        });
        globals.define("equalsStr", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return ((String)arguments.get(0)).equals((String)arguments.get(1));
            }
        });
        globals.define("charAt", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return ((String)arguments.get(0)).charAt((int)(double)arguments.get(1));
            }
        });
        globals.define("substring", new LoxCallable() {
            @Override
            public int arity() { return 3; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return ((String)arguments.get(0)).substring((int)(double)arguments.get(1),
                                                            (int)(double)arguments.get(2));

            }
        });
        globals.define("lengthStr", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return ((String)arguments.get(0)).length();
            }
        });
        globals.define("abs", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return java.lang.Math.abs((double)arguments.get(0));
            }
        });
        globals.define("round", new LoxCallable() {
            @Override
            public int arity() { return 1; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return java.lang.Math.round((double)arguments.get(0));
            }
        });
        globals.define("min", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return java.lang.Math.min((double)arguments.get(0), (double)arguments.get(1));
            }
        });
        globals.define("max", new LoxCallable() {
            @Override
            public int arity() { return 2; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return java.lang.Math.max((double)arguments.get(0), (double)arguments.get(1));
            }
        });
        globals.define("random", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter,
                               List<Object> arguments) {
                return java.lang.Math.random();
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        
      Object superclass = null;
      if (stmt.superclass != null) {
          superclass = evaluate(stmt.superclass);
          if (!(superclass instanceof LoxClass)) {
          throw new RuntimeError(stmt.superclass.name,
              "Superclass must be a class.");
          }
      }

      environment.define(stmt.name.lexeme, null);

      if (stmt.superclass != null) { 
        environment = new Environment(environment);
        environment.define("super", superclass);
      }

      Map<String, LoxFunction> methods = new HashMap<>();
      for (Stmt.Function method : stmt.methods) {
        LoxFunction function = new LoxFunction(method, environment,
        method.name.lexeme.equals("init"));
        methods.put(method.name.lexeme, function);
      }
  
      LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);

      if (superclass != null) {
        environment = environment.enclosing;
      }

      environment.assign(stmt.name, klass);
      return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt._if))) {
            execute(stmt._then);
        } else if (stmt._else != null) {
            execute(stmt._else);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (Break brk) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new Break();
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        throw new Return(value);
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assignAt(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable)callee;
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " +
                    function.arity() + " arguments but got " +
                    arguments.size() + ".");
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
      Object object = evaluate(expr.object);
      if (object instanceof LoxInstance) {
        return ((LoxInstance) object).get(expr.name);
      }
  
      throw new RuntimeError(expr.name,
          "Only instances have properties.");
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof String && right instanceof String)
                    return (String)left + (String)right;
                if (left instanceof Double && right instanceof Double)
                    return (Double)left + (Double)right;
                if (left instanceof String)
                    return (String)left + stringify(right);
                if (right instanceof String)
                    return stringify(left) + (String)right;

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double)right == 0.0)
                    throw new RuntimeError(expr.operator, "Can not divide by zero");

                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
        }

        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookUpVariable(expr.name, expr);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        return null;
    }

    @Override
    public Object visitListExpr(Expr.List exprList) {
        Object right = null;
        for (Expr expr : exprList.exprs) {
            right = evaluate(expr);
        }

        return right;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object cond = evaluate(expr._if);

        if (isTruthy(cond)) return evaluate(expr._then);
        return evaluate(expr._else);
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else if (expr.operator.type == TokenType.AND) {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
      Object object = evaluate(expr.object);
  
      if (!(object instanceof LoxInstance)) { 
        throw new RuntimeError(expr.name,
                               "Only instances have fields.");
      }
  
      Object value = evaluate(expr.value);
      ((LoxInstance)object).set(expr.name, value);
      return value;
    }

    @Override
    public Object visitSuperExpr(Expr.Super expr) {
      int distance = locals.get(expr);

      LoxClass superclass = (LoxClass)environment.getAt(
          distance, "super");

      LoxInstance object = (LoxInstance)environment.getAt(
          distance - 1, "this");

      LoxFunction method = superclass.findMethod(expr.method.lexeme);
      if (method == null) {
        throw new RuntimeError(expr.method,
            "Undefined property '" + expr.method.lexeme + "'.");
      }
      return method.bind(object);
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
      return lookUpVariable(expr.keyword, expr);
    }

    @Override
    public Object visitAnonFuncExpr(Expr.AnonFunc expr) {
        LoxAnonFunction func = new LoxAnonFunction(expr, environment);
        return func;
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        } else {
            return globals.get(name);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null) return false;

        return left.equals(right);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }
}
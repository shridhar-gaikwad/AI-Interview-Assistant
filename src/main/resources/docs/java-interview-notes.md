# Java Interview Notes

## Polymorphism
Polymorphism lets one interface represent different underlying types. In Java it
comes in two forms: compile-time polymorphism (method overloading) and runtime
polymorphism (method overriding via dynamic dispatch on the object's actual type).

## Encapsulation
Encapsulation bundles data (fields) and the methods that operate on it into a
single class, and hides internal state behind access modifiers (private fields
with public getters/setters). It reduces coupling and protects invariants.

## Inheritance
Inheritance lets a class acquire fields and methods from a parent class using the
`extends` keyword, promoting reuse. Java supports single class inheritance but a
class can implement multiple interfaces.

## Abstraction
Abstraction exposes only essential behaviour and hides implementation detail.
In Java it is achieved with abstract classes and interfaces, letting callers
depend on "what" a type does rather than "how" it does it.

## Checked vs Unchecked Exceptions
Checked exceptions (subclasses of Exception, excluding RuntimeException) must be
declared or handled at compile time. Unchecked exceptions (subclasses of
RuntimeException) represent programming errors and are not checked by the compiler.

## HashMap vs Hashtable
HashMap is not synchronized and allows one null key and multiple null values,
making it faster for single-threaded use. Hashtable is synchronized and does not
allow null keys or values. For concurrency prefer ConcurrentHashMap.

## The `final` keyword
A final variable cannot be reassigned, a final method cannot be overridden, and a
final class cannot be extended. It communicates intent and enables some compiler
and JVM optimizations.

## Garbage Collection
Java automatically reclaims memory occupied by objects that are no longer
reachable. The developer cannot force collection; System.gc() is only a hint to
the JVM. Generational GC divides the heap into young and old generations.

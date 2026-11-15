---
title: Strings and immutability
difficulty: BEGINNER
minutes: 25
summary: Why String can never change, how the string pool works, and what that means for ==, memory, security and performance.
related: [stringbuilder, equals-hashcode, hashmap-internals, jvm-memory]
---

## 1. What is this concept?

`java.lang.String` represents a sequence of characters, and it is **immutable**: once a `String` object is created, its contents can never change. Every method that *looks* like it modifies a string — `toUpperCase()`, `replace()`, `concat()`, `trim()` — actually returns a **new** `String` object and leaves the original untouched.

```java
String s = "java";
s.toUpperCase();          // creates "JAVA" … and throws it away
System.out.println(s);    // java
s = s.toUpperCase();      // the *variable* now points to a new object
System.out.println(s);    // JAVA
```

The key distinction: the **object** is immutable; a **variable** (reference) can be re-pointed at a different object (unless it is `final`).

## 2. Why was it introduced?

Strings are everywhere: class names, file paths, URLs, SQL, HTTP headers, map keys, passwords. Java's designers made them immutable so that a string, once handed to someone, **can be trusted not to change behind their back**. This single decision enables:

- **Security** — `new FileInputStream(path)` checks permissions on `path`; if `path` could be mutated after the check, an attacker could swap it for `/etc/shadow` (a *time-of-check-to-time-of-use* bug). Class loading by name relies on the same guarantee.
- **Thread safety for free** — immutable objects can be shared between threads with no locking.
- **The string pool** — identical literals can share one object, because nobody can modify it.
- **Hash caching** — the hash code can be computed once and reused, making strings fast `HashMap` keys.

## 3. What problem does it solve?

Without immutability every API receiving a string would need **defensive copies** ("copy it in case the caller changes it later"), strings could not be safely cached or shared, map keys could silently change their hash and get "lost" in a `HashMap`, and multi-threaded code would need locks around every string.

## 4. How did developers solve this problem before?

In **C**, a string is a mutable `char[]` terminated by `\0`. Any function holding a pointer can modify it; buffer overflows (`strcpy`, `gets`) were a classic security hole. Libraries made defensive copies (`strdup`) or relied on conventions like `const char*`, which the compiler cannot fully enforce. **C++** `std::string` is mutable too, so sharing it safely needs copies or `const` discipline. Java moved the guarantee from *convention* into the *type itself*.

## 5. Understand the concept from first principles

How do you make an object that can never change? Four rules (the recipe for *any* immutable class):

1. Make the class `final` (no subclass can add mutable behaviour).
2. Make all fields `private final`.
3. Provide no setters / no methods that modify state.
4. Never let a reference to internal mutable data escape (copy arrays in and out).

`String` follows exactly that recipe:

```java
public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    private final byte[] value;   // the characters (Java 9+: bytes, see below)
    private final byte coder;     // LATIN1 or UTF16
    private int hash;             // cached hash code (0 = not computed yet)
    private boolean hashIsZero;   // Java 13+: distinguishes "computed and it's 0"
    ...
}
```

`hash` is not `final`, yet `String` is still immutable: the field is a **cache** of a value that is fully determined by `value`. Computing it twice gives the same result, so a benign race is harmless.

## 6. Internal working and implementation details

### Compact strings (Java 9, JEP 254)
Before Java 9, `value` was a `char[]` (2 bytes per character). Most strings in real applications are Latin-1 (English letters, digits, JSON keys). Java 9 changed `value` to a `byte[]` plus a `coder` flag: **1 byte per char** if every char fits in Latin-1, otherwise UTF-16. This roughly halves memory for typical strings — invisible to your code.

### The string pool (string constant pool / intern pool)
- Every string **literal** in your code (`"hello"`) is stored in a JVM-wide pool. All occurrences of the same literal — even across classes — refer to **one** object.
- Since **Java 7** the pool lives in the regular **heap** (it used to be in PermGen), so pooled strings can be garbage-collected and the pool can grow.
- `new String("hello")` **always** creates a new heap object (the literal `"hello"` itself is still in the pool).
- `s.intern()` returns the pooled instance equal to `s`, adding it if absent.
- **Compile-time constants are folded**: `"hel" + "lo"` is computed by `javac` and becomes the literal `"hello"`. So is `A + "lo"` if `A` is a `final` variable initialised with a constant. Concatenation involving non-constant variables happens **at run time** and creates a new object.

```
 Heap
 ┌──────────────────────────────────────────────┐
 │  String pool:  "hello" ◀── a, b, d           │
 │                                              │
 │  String object "hello" (separate) ◀── c      │
 │  String object "hello" (runtime concat) ◀── f│
 └──────────────────────────────────────────────┘
```

### Hash code caching
`hashCode()` computes `s[0]*31^(n-1) + s[1]*31^(n-2) + … + s[n-1]` once and stores it in `hash`. Strings are therefore cheap to use repeatedly as `HashMap` keys.

### How `+` concatenation compiles
- Before Java 9: `a + b + c` compiled to `new StringBuilder().append(a).append(b).append(c).toString()`.
- Java 9+ (JEP 280): compiled to an `invokedynamic` call (`StringConcatFactory.makeConcatWithConstants`) — the JVM picks an optimal strategy at run time.
- Either way, `+=` **inside a loop** creates a new string per iteration → O(n²) copying. Use `StringBuilder` there.

### `substring` no longer shares the array
Before Java 7u6, `substring()` shared the parent's `char[]`, so a 10-character substring of a 10 MB string kept 10 MB alive (a memory leak). It now **copies** the relevant range.

## 7. Syntax and practical Java code examples

**`==` vs `equals`:**

```java
String a = "hello";
String b = "hel" + "lo";           // constant-folded → same pooled object
String c = new String("hello");    // forced new object
String d = c.intern();             // pooled instance
String e = "hel";
String f = e + "lo";               // runtime concatenation → new object

System.out.println(a == b);        // true
System.out.println(a == c);        // false
System.out.println(a == d);        // true
System.out.println(a == f);        // false
System.out.println(a.equals(f));   // true  ← always compare content with equals()
```

**Building strings efficiently:**

```java
// ❌ O(n²): each += copies the whole string so far
String csv = "";
for (String id : ids) csv += id + ",";

// ✅ O(n)
StringBuilder sb = new StringBuilder(ids.size() * 8);
for (String id : ids) sb.append(id).append(',');

// ✅ Even simpler for joining
String joined = String.join(",", ids);
String viaStream = ids.stream().collect(java.util.stream.Collectors.joining(","));
```

**Writing your own immutable value class** (same principles, modern syntax):

```java
public final class Money {
    private final long cents;
    private final String currency;

    public Money(long cents, String currency) {
        this.cents = cents;
        this.currency = java.util.Objects.requireNonNull(currency);
    }

    public Money plus(Money other) {           // returns a new object, never mutates
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("Currency mismatch");
        return new Money(cents + other.cents, currency);
    }
}

// Java 16+: records are shallowly immutable value carriers
public record MoneyRecord(long cents, String currency) { }
```

**Useful modern String API:**

```java
"  hi  ".strip();          // "hi"   (Unicode-aware, Java 11)
"   ".isBlank();           // true   (Java 11)
"ab".repeat(3);            // "ababab" (Java 11)
"a\nb\nc".lines().count(); // 3      (Java 11)
String json = """
        {"name": "Ada"}
        """;               // text block (Java 15)
```

## 8. Real-world backend development use cases

- **Map keys and cache keys:** `Map<String, User>`, Redis keys, Spring cache keys — safe precisely because strings can't change after insertion.
- **Configuration and headers:** Spring's `Environment`, HTTP header names, JWT claims are passed around freely without copying.
- **Logging:** use parameterised logging — `log.debug("User {} logged in", id)` — instead of `"User " + id + …`, so no string is built when DEBUG is off.
- **SQL:** never build SQL by concatenating user input (SQL injection); use bind parameters (`PreparedStatement`, JPA parameters).
- **Secrets:** APIs like `KeyStore` and `PBEKeySpec` use `char[]` for passwords because a `String` cannot be wiped and may linger in memory (and heap dumps) until garbage-collected.

## 9. Common mistakes and misconceptions

- ❌ Comparing strings with `==`. It compares **references**. It sometimes "works" with literals (pooling), which hides the bug until a value arrives from a request, file or database.
- ❌ Calling `s.trim()` / `s.replace(...)` and ignoring the result.
- ❌ `+=` in loops building large strings.
- ❌ Believing `new String("x")` is "safer" — it just wastes an object.
- ❌ Calling `intern()` on unbounded user input to "save memory" — it can create a huge pool and contention. Prefer G1's string deduplication (`-XX:+UseStringDeduplication`) or no action at all.
- ❌ *"`final String s` makes the string immutable."* The string was already immutable; `final` only stops the **variable** being reassigned.
- ❌ `str.equals("literal")` when `str` may be `null` → NPE. Prefer `"literal".equals(str)` or `Objects.equals(a, b)`.

## 10. Performance considerations and best practices

- Use `StringBuilder` (single-threaded) for building strings in loops; pre-size it when you know the length. `StringBuffer` is the synchronised legacy version — rarely needed.
- Simple one-line `a + b + c` concatenation is fine; the compiler/JVM optimises it.
- Use parameterised logging; avoid `String.format` in hot paths (it parses the format each time).
- Compact strings already save memory for Latin-1 text; avoid manual `intern()`.
- For many identical runtime strings (e.g. status codes from a DB), consider enums or a small canonical map rather than `intern()`.
- Prefer `isEmpty()` / `isBlank()` over `length() == 0` / `trim().isEmpty()` for clarity.

## 11. Interview questions and answers

**Q1. Why is String immutable in Java?**
Security (values checked by APIs such as file access and class loading can't change afterwards), thread safety without synchronisation, enabling the string pool (shared literals), and caching the hash code for fast use as map keys.

**Q2. How many objects does `String s = new String("abc");` create?**
Up to two: the literal `"abc"` in the string pool (if not already there, created when the class's constant is resolved) and one new `String` object on the heap referenced by `s`.

**Q3. Where is the string pool located?**
In the heap since Java 7 (previously PermGen). Pooled strings can be garbage-collected when no longer referenced.

**Q4. What does `intern()` do?**
Returns the canonical pooled `String` equal to this one, adding it to the pool if it isn't there. After interning, equal strings are `==`.

**Q5. Difference between `String`, `StringBuilder` and `StringBuffer`?**
`String` is immutable. `StringBuilder` is a mutable, non-synchronised buffer — fastest for building strings in one thread. `StringBuffer` is the older, synchronised (thread-safe, slower) equivalent.

**Q6. How would you design your own immutable class?**
`final` class, `private final` fields, no setters, validate in the constructor, defensively copy mutable inputs and outputs (arrays, lists, dates), and return new instances from "modifying" methods. Records give this structure out of the box (shallowly).

**Q7. Why are passwords often stored in `char[]` instead of `String`?**
A `char[]` can be overwritten (`Arrays.fill(pw, '\0')`) as soon as it's used. A `String` is immutable and stays in memory until GC — possibly visible in heap dumps.

## 12. Related concepts and dependencies

- **StringBuilder and StringBuffer** — the mutable counterparts.
- **equals() and hashCode()** — why string equality is content-based and hashes are cached.
- **HashMap internals** — why immutable keys matter.
- **JVM memory** — heap, the string pool and garbage collection.

## 13. Summary and revision notes

- `String` is **final** with **private final** data and no mutators → immutable. Methods return **new** strings.
- Benefits: security, thread safety, string pool, cached `hashCode`.
- Literals are pooled (heap since Java 7); `new String(..)` always creates a new object; `intern()` returns the pooled one.
- Compile-time constants are folded (`"a" + "b"` ≡ `"ab"`); runtime concatenation creates new objects.
- Always compare with `equals()`, never `==`.
- Loops → `StringBuilder`; joining → `String.join` / `Collectors.joining`.
- Java 9 compact strings: `byte[]` + coder, 1 byte per Latin-1 char.

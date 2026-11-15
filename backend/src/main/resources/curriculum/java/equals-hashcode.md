---
title: equals() and hashCode()
difficulty: INTERMEDIATE
minutes: 25
summary: The contract that every HashMap, HashSet, cache and JPA entity silently depends on — and the bugs you get when you break it.
related: [hashmap-internals, string-immutability, set-implementations, comparable-comparator, records]
---

## 1. What is this concept?

Every Java object inherits two methods from `java.lang.Object`:

- `boolean equals(Object other)` — *are these two objects **logically** the same?*
- `int hashCode()` — *a number summarising the object, used to find it quickly in hash-based collections.*

By default both are based on **identity**: `equals` is `==` (same object in memory) and `hashCode` is an identity hash. You override them when your class has **value semantics** — two different objects with the same data should count as equal (two `Email("a@b.com")` objects, two `Money(100, "EUR")` objects).

The rule that ties them together:

> **If `a.equals(b)` is true, then `a.hashCode() == b.hashCode()` must be true.**
> (The reverse is not required: different objects may share a hash code — a *collision*.)

## 2. Why was it introduced?

Collections need a way to answer "is this element already here?" and "find me the value for this key". Comparing by memory address is useless for values that are created independently (a key parsed from an HTTP request is never the same *object* as the key stored in your map). Java lets each class define its own notion of equality, and — for speed — a hash code that lets `HashMap`/`HashSet` jump straight to a small group of candidates instead of comparing against every element.

## 3. What problem does it solve?

- **Lookup by value**: `map.get(new UserId(42))` finds the entry stored under a different `UserId(42)` object.
- **De-duplication**: `HashSet` and `Stream.distinct()` keep one of each *logical* value.
- **O(1) average lookups**: the hash narrows the search to one bucket; `equals` confirms the match.
- **Testing**: `assertEquals(expected, actual)` compares DTOs by value.

## 4. How did developers solve this problem before?

In C you'd write a comparison function (`int cmp(const void*, const void*)`) and a hash function and pass them explicitly to every container (`qsort`, hand-written hash tables). Nothing guaranteed the hash and comparison agreed. Java moved both functions **into the object** so every collection uses them consistently — but the *agreement* between them is still your responsibility: that is the contract.

## 5. Understand the concept from first principles

Imagine 1,000,000 objects in a list. Finding one by value means up to 1,000,000 `equals` calls. Now imagine 1,000,000 numbered drawers: compute a number from the object (its **hash**), open only that drawer, and compare against the few objects inside. That's a hash table.

For this to work, **the same logical value must always land in the same drawer** — otherwise you'd look in drawer 17 for something stored in drawer 902. Hence:

1. Equal objects → equal hash codes (same drawer). *Mandatory.*
2. Hash code must not change while the object is in the table. *So build it only from fields that don't change — ideally immutable ones.*
3. Unequal objects should *usually* get different hash codes (fewer objects per drawer → faster). *Quality, not correctness.*

### The `equals` contract (from the `Object` Javadoc)

For non-null references `x`, `y`, `z`:

| Property | Meaning |
|---|---|
| Reflexive | `x.equals(x)` is true |
| Symmetric | `x.equals(y)` ⇔ `y.equals(x)` |
| Transitive | `x.equals(y)` and `y.equals(z)` ⇒ `x.equals(z)` |
| Consistent | repeated calls give the same answer if nothing used in the comparison changed |
| Null | `x.equals(null)` is false (never throws) |

### The `hashCode` contract
- Consistent during one execution of the application (if fields used by `equals` don't change).
- Equal objects ⇒ equal hash codes.
- Unequal objects *may* collide, but fewer collisions = better performance.

## 6. Internal working and implementation details

**Default `Object.equals`** is literally `return this == obj;`.

**Default `Object.hashCode`** returns an *identity hash code*. In HotSpot it is **not** the memory address (objects move during GC); it is generated (by default from a thread-local pseudo-random generator) the first time it's requested and stored in the object's header, so it stays stable for the object's life.

**How `HashMap.get(key)` uses both:**

```
1. h = key.hashCode()                      → pick the bucket
2. for each entry in that bucket:
      if entry.hash == h                   → cheap int compare first
         && (entry.key == key || key.equals(entry.key))  → then equals
            return entry.value
3. return null
```

So:
- Override `equals` but not `hashCode` → equal objects land in **different buckets** → `get` returns `null`, `HashSet` keeps duplicates.
- Override `hashCode` but not `equals` → same bucket, but identity `equals` says they differ → duplicates again.
- A key whose hash-relevant fields **change after insertion** → it now hashes to a different bucket than where it's stored → unreachable entry (a memory leak).

**`instanceof` vs `getClass()`** in `equals`:
- `getClass() != o.getClass()` — strict: a subclass instance is never equal to a parent instance. Safe for symmetry.
- `o instanceof Point p` — allows subclasses (e.g. Hibernate proxies) to be equal; safe **only** if subclasses don't add state that participates in equality — make the class or `equals` `final`.
- It is impossible to extend an instantiable class, add a value field, and keep the contract (Effective Java, Item 10) → **prefer composition** over inheritance for value types.

**Records (Java 16+)** generate `equals`, `hashCode` and `toString` from all components automatically.

## 7. Syntax and practical Java code examples

**Classic hand-written implementation:**

```java
import java.util.Objects;

public final class Employee {
    private final String employeeId;   // identity of the employee
    private final String name;
    private final int departmentId;

    public Employee(String employeeId, String name, int departmentId) {
        this.employeeId = Objects.requireNonNull(employeeId);
        this.name = name;
        this.departmentId = departmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                       // fast path, reflexive
        if (o == null || getClass() != o.getClass()) return false;
        Employee other = (Employee) o;
        return departmentId == other.departmentId
                && employeeId.equals(other.employeeId)
                && Objects.equals(name, other.name);     // null-safe
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, name, departmentId); // same fields as equals
    }
}
```

**Same thing with a record:**

```java
public record EmployeeRecord(String employeeId, String name, int departmentId) { }
// equals/hashCode/toString generated from all three components
```

**The classic bug — overriding only `equals`:**

```java
class Point {
    final int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    @Override public boolean equals(Object o) {
        return o instanceof Point p && p.x == x && p.y == y;
    }
    // hashCode NOT overridden
}

Set<Point> set = new HashSet<>();
set.add(new Point(1, 2));
System.out.println(set.contains(new Point(1, 2)));   // false (almost certainly)
```

**Mutable key bug:**

```java
class Tag {
    String name;
    Tag(String name) { this.name = name; }
    @Override public boolean equals(Object o) { return o instanceof Tag t && t.name.equals(name); }
    @Override public int hashCode() { return name.hashCode(); }
}

Set<Tag> tags = new HashSet<>();
Tag t = new Tag("java");
tags.add(t);
t.name = "spring";                      // mutate after insertion
System.out.println(tags.contains(t));   // false — looks in the "spring" bucket
System.out.println(tags.size());        // 1   — but it's still in there, unreachable
```

**Overload instead of override (no `@Override`):**

```java
public boolean equals(Point other) { ... }  // ❌ overloads equals(Object); collections never call it
```

## 8. Real-world backend development use cases

- **DTOs and value objects** (`Money`, `EmailAddress`, `OrderId`) — use records or proper `equals`/`hashCode` so they work as map keys, in sets and in test assertions.
- **Caching:** Spring `@Cacheable` builds keys from method arguments; your argument types need correct `equals`/`hashCode` or every call is a cache miss.
- **De-duplicating** API results: `stream().distinct()`, `Collectors.toSet()`.
- **JPA/Hibernate entities** are a special case:
  - Generated IDs are `null` until persisted, so an id-based `hashCode` would **change** after `save()` — breaking any `HashSet` the entity was already in.
  - Hibernate may hand you **proxy subclasses**, so `getClass()` comparisons can fail; use `instanceof` with care.
  - A common safe pattern: equality on a natural/business key, or on the id with a **constant** `hashCode()` (e.g. `getClass().hashCode()`) and `equals` returning false while id is null.
  - Avoid Lombok `@Data` / `@EqualsAndHashCode` on entities (includes lazy associations, may trigger loading or infinite recursion).

## 9. Common mistakes and misconceptions

- ❌ Overriding `equals` without `hashCode` (or vice versa).
- ❌ Writing `equals(MyType other)` — an **overload**. Always use `@Override` so the compiler catches it.
- ❌ Using **mutable** fields in `hashCode` for objects stored in hash collections.
- ❌ Comparing `double`/`float` fields with `==` — use `Double.compare(a, b) == 0` (handles `NaN` and `-0.0`), which is what records do.
- ❌ Comparing arrays with `equals` (identity!) — use `Arrays.equals` / `Arrays.hashCode`.
- ❌ Breaking symmetry, e.g. a `CaseInsensitiveString` that equals a plain `String` — `String.equals` won't return true the other way.
- ❌ Assuming equal hash codes mean equal objects — collisions are normal (`"Aa".hashCode() == "BB".hashCode()`).
- ❌ `BigDecimal` surprise: `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` is **false** (scale differs) while `compareTo` returns 0 — a `HashSet` keeps both, a `TreeSet` keeps one.

## 10. Performance considerations and best practices

- Check `this == o` first (cheap), then type, then the **cheapest / most discriminating fields first**.
- A good `hashCode` spreads values well; `Objects.hash(...)` is convenient but allocates a varargs array — in very hot paths write `31 * a.hashCode() + b` by hand (or rely on records, which are optimised).
- Never return a constant hash for frequently-hashed general-purpose objects (everything lands in one bucket → O(n) or O(log n) lookups). The JPA constant-hash trick is acceptable because entity sets are small.
- Prefer **immutable** value objects (records) so hash codes can't change.
- Keep `equals` consistent with `compareTo` when the type is also `Comparable` (or document why not), otherwise `HashSet` and `TreeSet` disagree.

## 11. Interview questions and answers

**Q1. What is the contract between `equals` and `hashCode`?**
If two objects are equal by `equals`, they must return the same `hashCode`. Unequal objects may share a hash code. `hashCode` must be consistent while the fields used in `equals` don't change.

**Q2. What happens if you override `equals` but not `hashCode`?**
Equal objects usually get different identity hash codes, so hash-based collections put them in different buckets: `contains`/`get` fail and sets keep duplicates.

**Q3. Can two unequal objects have the same hash code?**
Yes — that's a collision and it's legal. `HashMap` handles it by chaining entries in the same bucket (and treeifying long chains). Too many collisions only hurt performance.

**Q4. Why shouldn't hash keys be mutable?**
If a field used in `hashCode` changes after insertion, the entry stays in the old bucket while lookups go to the new one, making it unreachable — a silent bug and memory leak.

**Q5. `getClass()` or `instanceof` in `equals`?**
`getClass()` is strict and guarantees symmetry across a class hierarchy. `instanceof` allows subclass instances (useful for proxies such as Hibernate's) but is only correct if subclasses don't add equality-relevant state — mark `equals` final or the class final.

**Q6. How do you implement equals/hashCode for JPA entities?**
Avoid letting `hashCode` change across the entity's lifecycle: use a natural business key, or compare by id with `equals` false for unsaved (null-id) entities and a constant `hashCode`. Use `instanceof` / Hibernate's class-unproxying because of proxies, and avoid including lazy associations.

**Q7. What do records give you?**
`equals`, `hashCode` and `toString` derived from all components, with correct handling of floating-point and null components — ideal for DTOs and value objects.

## 12. Related concepts and dependencies

- **HashMap internals** — exactly how buckets, hashes and `equals` are used.
- **Set implementations** — `HashSet` is a `HashMap` under the hood; `TreeSet` uses `compareTo` instead.
- **Comparable and Comparator** — the ordering counterpart of equality.
- **Records** — automatic value semantics.

## 13. Summary and revision notes

- Default: `equals` = identity, `hashCode` = identity hash.
- **equal ⇒ same hash**; same hash ⇏ equal.
- `equals` must be reflexive, symmetric, transitive, consistent and return false for null.
- Always override both, together, from the **same fields**, with `@Override`.
- Hash keys should be immutable; mutating a key strands its entry.
- Use `Objects.equals`/`Objects.hash`, `Double.compare`, `Arrays.equals`; prefer **records** for value types.
- JPA entities: beware null ids, proxies and lazy fields.

---
title: HashMap internal implementation
difficulty: ADVANCED
minutes: 35
summary: Buckets, hash spreading, collisions, resizing and treeification — exactly what happens inside put() and get().
related: [equals-hashcode, map-overview, concurrent-collections, string-immutability]
---

## 1. What is this concept?

`java.util.HashMap<K, V>` stores key → value pairs and gives **average O(1)** `put`, `get` and `remove`. Internally it is an **array of buckets** (`Node<K,V>[] table`). Each key's `hashCode()` decides its bucket; keys that land in the same bucket are kept in a **linked list**, which turns into a **red-black tree** when it gets long.

Key facts at a glance:

| Property | Value |
|---|---|
| Default initial capacity | 16 buckets (always a power of two) |
| Default load factor | 0.75 |
| Resize threshold | capacity × load factor (16 × 0.75 = 12 entries) |
| Resize | doubles the capacity |
| Treeify a bucket | when it reaches 8 nodes **and** capacity ≥ 64 (otherwise resize instead) |
| Untreeify | when a tree bin shrinks to 6 nodes (during resize split) |
| `null` key | allowed once (stored in bucket 0) · `null` values allowed |
| Ordering | none guaranteed · not thread-safe |

## 2. Why was it introduced?

Searching an unsorted list is O(n); a sorted tree is O(log n). Applications constantly need "find the thing for this key" — sessions by id, users by email, caches, counters — and want it in **constant time** regardless of size. The hash table is the classic data structure for that, and `HashMap` (Java 1.2, Collections Framework) is Java's general-purpose implementation, replacing the older synchronised `Hashtable`.

## 3. What problem does it solve?

Fast key-based lookup with no ordering requirement:

- O(1) average for insert, lookup and delete.
- Accepts any key type that implements `equals`/`hashCode` correctly.
- Grows automatically as you add entries.
- Unlike `Hashtable`: no global lock overhead for single-threaded use, allows a `null` key and `null` values.

## 4. How did developers solve this problem before?

- **Linear search** in arrays/lists — O(n).
- **Sorted arrays + binary search** or **balanced trees** (Java's `TreeMap`) — O(log n), and keys must be comparable.
- **`Hashtable`** (Java 1.0) — a hash table with every method `synchronized`; safe but slow under contention, no null keys/values. Today: `HashMap` for single-threaded code, `ConcurrentHashMap` for concurrent code.
- In **Java 7 and earlier**, `HashMap` buckets were *only* linked lists with **head insertion**; Java 8 switched to tail insertion and added treeification (see below).

## 5. Understand the concept from first principles

**Step 1 — turn a key into an array index.** An array gives O(1) access by index, so we need `index = f(key)` in `[0, capacity)`. `hashCode()` gives an `int`; we reduce it to an index.

**Step 2 — reduce cheaply.** `hash % capacity` works, but modulo is slow-ish and negative hashes are awkward. If capacity is a **power of two**, `hash & (capacity - 1)` keeps the low bits and is a single CPU instruction. With capacity 16 (`0b10000`), `capacity - 1 = 0b01111`, so the index is the lowest 4 bits.

**Step 3 — handle collisions.** Different keys can produce the same index. HashMap uses **separate chaining**: each bucket holds a small list of entries; on lookup we walk it, comparing with `equals`.

**Step 4 — keep chains short.** If the table fills up, chains grow and lookups degrade. So when `size > capacity × loadFactor`, the table **doubles** and every entry is redistributed. A load factor of 0.75 is the trade-off between memory (empty buckets) and speed (chain length).

**Step 5 — defend against bad hashes.** If many keys collide anyway (poor `hashCode`, or an attacker crafting colliding keys), a long list makes lookups O(n). Java 8 converts long chains into **red-black trees** → O(log n) worst case per bucket.

## 6. Internal working and implementation details

### The data structures (OpenJDK, simplified)

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;     // cached spread hash
    final K key;
    V value;
    Node<K,V> next;     // next node in the same bucket
}

transient Node<K,V>[] table;   // lazily allocated on first put
transient int size;
int threshold;                 // next size at which to resize
final float loadFactor;
transient int modCount;        // structural modifications (for fail-fast iterators)
```

### Hash spreading

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

Because the index uses only the **low** bits, keys whose hash codes differ only in high bits would all collide. XOR-ing the top 16 bits into the bottom 16 lets high bits influence the index — cheap insurance against weak `hashCode` implementations. A `null` key always gets hash 0 → bucket 0.

### `put(key, value)` step by step

```
1. if table is null → resize() to allocate it (capacity 16 by default)
2. hash = hash(key);  i = (n - 1) & hash
3. if table[i] is empty → place a new Node there
4. else walk the bucket:
     - node with same hash AND (same reference OR equals) → replace value, return old value
     - bucket is a TreeNode → putTreeVal (red-black tree insert)
     - reached the end → append new Node at the TAIL
       if chain length reaches TREEIFY_THRESHOLD (8) → treeifyBin()
          (treeifyBin resizes instead if capacity < MIN_TREEIFY_CAPACITY (64))
5. modCount++;  if (++size > threshold) resize()
```

### `get(key)`

```
hash = hash(key); first = table[(n - 1) & hash]
check first node → if tree, search the tree → else walk the list
match = same hash && (== || equals)
```

`hash` is compared first because comparing ints is far cheaper than calling `equals`.

### Resizing (`resize()`)

- New capacity = old × 2; new threshold = old × 2.
- Because capacity is a power of two, each entry either **stays at index `i`** or **moves to `i + oldCap`**. Which one depends on a single bit: `(hash & oldCap) == 0`. No re-hashing needed — the bucket splits into a "lo" list and a "hi" list, **preserving relative order**.
- Tree bins are split similarly; if a half drops to ≤ 6 nodes (`UNTREEIFY_THRESHOLD`) it becomes a list again.
- Resizing is O(n) — expensive for big maps, which is why presizing matters.

```
 old capacity 16:  hashes 5 (0b00101) and 21 (0b10101) both → index 5
 new capacity 32:  5 & 16 == 0  → stays at 5
                   21 & 16 != 0 → moves to 5 + 16 = 21
```

### Treeification (Java 8+)

- A bucket becomes a red-black tree of `TreeNode`s when it reaches **8** nodes and the table has at least **64** buckets.
- Tree nodes are ordered by hash, then — if keys are `Comparable` and of the same class — by `compareTo`, then by a tie-breaker (`System.identityHashCode`).
- With a sensible `hashCode`, chains of 8 are statistically very rare (the Javadoc cites a Poisson distribution); trees are a safety net against pathological collisions and hash-flooding attacks.

### Java 7 vs Java 8

| | Java 7 | Java 8+ |
|---|---|---|
| Bucket structure | linked list only | list → red-black tree |
| Insertion | **head** of list | **tail** of list |
| Worst-case lookup | O(n) | O(log n) per bucket |
| Concurrent resize misuse | could create a **cycle** → infinite loop on `get` | no cycle (still unsafe: lost updates, corrupted state) |

### Iterators are fail-fast
Iterators remember `modCount`; if the map is structurally modified other than through the iterator, the next call throws `ConcurrentModificationException`. This is best-effort bug detection, **not** a thread-safety mechanism.

## 7. Syntax and practical Java code examples

**Everyday API (Java 8+ methods avoid get-then-put races and boilerplate):**

```java
Map<String, Integer> wordCount = new HashMap<>();
for (String w : words) {
    wordCount.merge(w, 1, Integer::sum);          // insert 1 or add 1
}

Map<String, List<Order>> byCustomer = new HashMap<>();
for (Order o : orders) {
    byCustomer.computeIfAbsent(o.customerId(), k -> new ArrayList<>()).add(o);
}

int count = wordCount.getOrDefault("java", 0);
wordCount.putIfAbsent("spring", 0);

for (Map.Entry<String, Integer> e : wordCount.entrySet()) {   // iterate entries, not keySet + get
    System.out.println(e.getKey() + "=" + e.getValue());
}
```

**Presizing correctly:**

```java
// Want to store 1,000 entries without any resize:
Map<String, User> a = new HashMap<>(1000);           // capacity 1024, threshold 768 → resizes once!
Map<String, User> b = new HashMap<>((int) (1000 / 0.75f) + 1);   // capacity 2048 → no resize
Map<String, User> c = HashMap.newHashMap(1000);      // Java 19+: does that math for you
```

**See the bucket index yourself:**

```java
static int indexFor(Object key, int capacity) {
    int h = key.hashCode();
    int spread = h ^ (h >>> 16);
    return spread & (capacity - 1);
}

System.out.println(indexFor("Aa", 16) == indexFor("BB", 16)); // true: "Aa" and "BB" have equal hashCodes
```

**Iterate and remove safely:**

```java
// ❌ ConcurrentModificationException
for (String k : map.keySet()) if (k.startsWith("tmp")) map.remove(k);

// ✅
map.keySet().removeIf(k -> k.startsWith("tmp"));
// or use iterator.remove()
```

**Thread-safe alternative:**

```java
Map<String, Long> hits = new ConcurrentHashMap<>();
hits.merge(path, 1L, Long::sum);   // atomic per key
```

## 8. Real-world backend development use cases

- **In-memory caches / lookup tables:** e.g. mapping currency codes to rates, feature flags, routing tables. Use `ConcurrentHashMap` if multiple request threads write to it.
- **Grouping and aggregation:** `Collectors.groupingBy` builds a `HashMap` under the hood.
- **Request-scoped data:** JSON objects deserialised to `Map<String, Object>`, headers, query parameters.
- **Avoiding N+1 queries:** load related rows in one query, index them in a `HashMap<Long, Entity>` by id, then join in memory.
- **LRU caches:** `LinkedHashMap` (a `HashMap` + doubly linked list) with `accessOrder = true` and `removeEldestEntry` gives a simple LRU cache.
- **Security:** treeification limits damage from **hash-flooding** DoS attacks where an attacker sends many keys (e.g. JSON field names or form parameters) designed to collide.

## 9. Common mistakes and misconceptions

- ❌ Using `HashMap` from **multiple threads** with writes — lost updates, corrupted structure; in Java 7 even infinite loops. Use `ConcurrentHashMap` (or `Collections.synchronizedMap` for simple cases).
- ❌ Relying on **iteration order** — it can change between runs, Java versions and after resizes. Use `LinkedHashMap` (insertion order) or `TreeMap` (sorted).
- ❌ **Mutable keys** whose `hashCode` changes after insertion — entries become unreachable.
- ❌ Bad `hashCode` (e.g. constant) → all entries in one bucket → O(log n) at best, O(n) for non-comparable keys.
- ❌ `map.get(k) == null` to test presence — the value itself may be `null`. Use `containsKey`.
- ❌ `new HashMap<>(n)` thinking it holds `n` entries without resizing — capacity ≠ number of entries (load factor!).
- ❌ Believing a 1 → 8 chain "becomes a tree" in a tiny map — below capacity 64 the map **resizes** instead.
- ❌ `ConcurrentHashMap` does **not** allow `null` keys or values (ambiguity under concurrency).

## 10. Performance considerations and best practices

- **Presize** maps whose final size you know (`HashMap.newHashMap(n)` on Java 19+), avoiding repeated O(n) resizes.
- Keep the default **load factor 0.75** unless you've measured a reason not to.
- Use **immutable keys** with good `hashCode` (String, Integer, enums, records).
- For enum keys, `EnumMap` is faster and more compact (array indexed by ordinal).
- Prefer `merge`, `compute`, `computeIfAbsent` over `get`-then-`put` — clearer, one lookup, and atomic in `ConcurrentHashMap`.
- Iterate `entrySet()` rather than `keySet()` + `get()` (avoids a second lookup per entry).
- Very large maps of primitives: boxing costs memory; consider specialised libraries (e.g. fastutil) only if profiling says so.

## 11. Interview questions and answers

**Q1. How does `HashMap` work internally?**
It's an array of buckets. `put` computes `hash = h ^ (h >>> 16)` from `key.hashCode()`, picks the bucket with `(n - 1) & hash`, and stores a node there. Collisions are chained in a linked list (tail insertion); a bucket with ≥ 8 nodes becomes a red-black tree if capacity ≥ 64. `get` finds the bucket the same way and compares hash then `equals`. When size exceeds capacity × 0.75 the table doubles and entries are split between index `i` and `i + oldCap`.

**Q2. Why is the capacity always a power of two?**
So the index can be computed with a fast bitwise AND, `(n - 1) & hash`, instead of modulo, and so resizing can split each bucket using one bit (`hash & oldCap`) without recomputing hashes.

**Q3. Why does `hash()` do `h ^ (h >>> 16)`?**
Only the low bits choose the bucket. Mixing the high 16 bits into the low 16 reduces collisions for hash codes that differ only in their upper bits.

**Q4. What changed in Java 8?**
Treeification of long buckets (O(log n) worst case instead of O(n)), tail insertion instead of head insertion (no cycles on concurrent resize — though it's still not thread-safe), and new default methods such as `merge`, `compute`, `getOrDefault`.

**Q5. What is the load factor and why 0.75?**
The fraction of capacity that may be filled before resizing. 0.75 balances memory (fewer empty buckets) against time (shorter chains). Higher saves memory but increases collisions; lower wastes memory.

**Q6. Can HashMap have a `null` key? `Hashtable`? `ConcurrentHashMap`?**
`HashMap`: one `null` key (bucket 0) and any number of `null` values. `Hashtable` and `ConcurrentHashMap`: no `null` keys or values.

**Q7. What happens if two keys have the same hash code?**
They land in the same bucket and are chained; lookups compare with `equals` to find the right one. Correctness is preserved; only performance suffers if it happens a lot.

**Q8. Why is HashMap not thread-safe and what do you use instead?**
Concurrent `put`s can overwrite each other, and a resize racing with other operations can lose entries or corrupt the table. Use `ConcurrentHashMap`, which uses CAS for empty bins and fine-grained locking per bin, with atomic `compute`/`merge`.

## 12. Related concepts and dependencies

- **equals() and hashCode()** — the contract `HashMap` depends on. Learn it first.
- **Map implementations** — `LinkedHashMap`, `TreeMap`, `EnumMap`, `WeakHashMap`, `IdentityHashMap`.
- **Concurrent collections** — how `ConcurrentHashMap` achieves thread safety.
- **Strings and immutability** — why `String` is the ideal key.

## 13. Summary and revision notes

- Array of buckets; index = `(n - 1) & (h ^ (h >>> 16))`.
- Default capacity **16**, load factor **0.75**, threshold **12**; resize = **double**, entries split to `i` or `i + oldCap`.
- Collisions: linked list (**tail** insertion) → **red-black tree** at **8** nodes if capacity ≥ **64**; back to list at **6**.
- Lookup compares `hash` first, then `==`/`equals`.
- One `null` key allowed (bucket 0); no ordering; **not thread-safe**; fail-fast iterators.
- Presize (`HashMap.newHashMap(n)`), immutable keys, `merge`/`computeIfAbsent`, `ConcurrentHashMap` for concurrency, `LinkedHashMap` for order.

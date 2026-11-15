---
title: JDK, JRE and JVM
difficulty: BEGINNER
minutes: 20
summary: How a .java file becomes running machine code — and which piece of the Java platform does what.
related: [jvm-architecture, class-loading, garbage-collection, jvm-memory]
---

## 1. What is this concept?

Java is not one program — it is a **platform** made of three nested layers:

| Layer | Stands for | Contains | Who needs it |
|---|---|---|---|
| **JVM** | Java Virtual Machine | The engine that loads and executes bytecode | Anything that *runs* Java |
| **JRE** | Java Runtime Environment | JVM + the standard class library (`java.lang`, `java.util`, …) | Anything that runs Java apps |
| **JDK** | Java Development Kit | JRE + development tools (`javac`, `jar`, `javadoc`, `jshell`, `jdb`, `jlink`, `jcmd`, …) | Developers and build servers |

```
┌──────────────────────── JDK ────────────────────────┐
│  javac  jar  javadoc  jshell  jlink  jcmd  jdb ...  │
│  ┌───────────────────── JRE ─────────────────────┐  │
│  │  Class library: java.lang, java.util, java.io │  │
│  │  ┌─────────────────── JVM ─────────────────┐  │  │
│  │  │ Class loader · Bytecode verifier        │  │  │
│  │  │ Interpreter · JIT compiler · GC         │  │  │
│  │  └─────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

> **One-line answer for interviews:** the JDK is for *building* Java programs, the JRE is for *running* them, and the JVM is the part of the JRE that actually *executes* bytecode.

## 2. Why was it introduced?

In the early 1990s, C and C++ programs were compiled straight to machine code for one CPU and one operating system. Shipping the same program to Windows, Solaris and Mac meant compiling (and often rewriting) it for each target.

Sun Microsystems wanted code that could run on set-top boxes, browsers and servers alike. Their answer (Java 1.0, 1996) was to compile to an **intermediate, platform-neutral instruction set — bytecode** — and to write one *virtual machine* per platform that understands it. That is the famous promise: **"Write once, run anywhere."**

## 3. What problem does it solve?

1. **Portability** — the same `.class` / `.jar` runs on any OS/CPU that has a JVM.
2. **Safety** — bytecode is *verified* before it runs (no forging pointers, no jumping into the middle of an instruction, type-safe operand stack). Memory is managed by a garbage collector, eliminating whole classes of C bugs (use-after-free, double free).
3. **Performance without giving up portability** — the JVM profiles your code while it runs and compiles hot paths to optimised native code (JIT), often using information a static compiler never has.
4. **Separation of concerns** — developers need compilers and tools (JDK); production machines only need a runtime.

## 4. How did developers solve this before?

- **Native compilation per platform** (C/C++): fast, but one binary per OS/CPU, `#ifdef` jungles, manual memory management.
- **Pure interpreters** (early BASIC, shell scripts): portable, but slow because every line is re-parsed on every execution.
- **Earlier virtual machines** existed — UCSD Pascal's *p-code* (1970s) and Smalltalk's VM — and Java borrowed the idea. Java's contribution was combining a VM with a verifier, a GC, a huge standard library and (later) world-class JIT compilers, and making it mainstream.

## 5. Understand it from first principles

A CPU only understands its own machine code (x86-64, ARM64, …). So *something* must eventually translate your program into that. There are three fundamental strategies:

1. **Translate everything ahead of time** (AOT) → fast startup, not portable.
2. **Translate each instruction every time you run it** (interpretation) → portable, slow.
3. **Ship a portable intermediate form and translate at run time, smartly** → Java's choice.

Java splits the journey into two compilers:

```
 Hello.java ──javac──▶ Hello.class (bytecode) ──JVM──▶ interpreter / JIT ──▶ machine code
   (source)          compile time, any OS              run time, on the target machine
```

Bytecode is a compact instruction set for a **stack machine**. `int c = a + b;` becomes roughly:

```
iload_1   // push local variable 1 (a) onto the operand stack
iload_2   // push b
iadd      // pop two ints, push their sum
istore_3  // pop result into local variable 3 (c)
```

Because bytecode does not mention registers or OS calls, the same instructions are valid everywhere — each JVM maps them to its own hardware.

## 6. Internal working and implementation details

What happens when you type `java -cp app.jar com.example.Main`:

1. **The launcher starts the JVM** (a native program — e.g. `java.exe` + `jvm.dll` / `libjvm.so`).
2. **Class loading** — the *bootstrap* loader loads core classes (`java.lang.Object`, `String`, …); the *platform* loader loads other JDK modules; the *application* loader loads your classes from the classpath. Loading is lazy: a class is loaded the first time it is actually needed.
3. **Linking** — *verification* (is this bytecode well-formed and type-safe?), *preparation* (allocate static fields with default values), *resolution* (turn symbolic references like `java/util/List` into direct references, often lazily).
4. **Initialization** — static initialisers and static field assignments run, once, in a thread-safe way.
5. **Execution** — `main(String[])` starts on the main thread. Code is first **interpreted**. The JVM counts how often methods and loops run; hot code is compiled by the **JIT**:
   - **C1** (client compiler): compiles quickly with light optimisations.
   - **C2** (server compiler): compiles slowly with aggressive optimisations — inlining, escape analysis, loop unrolling, dead-code elimination.
   - **Tiered compilation** (default since Java 8) uses both: interpret → C1 with profiling → C2 for the hottest code. If an optimistic assumption breaks (e.g. a second subclass appears), the JVM *deoptimises* back to the interpreter.
6. **Memory management** — objects live on the **heap**, managed by a garbage collector (G1 is the default on server-class machines since Java 9). Each thread has its own **stack** of frames for method calls. Class metadata lives in **Metaspace** (native memory, replaced PermGen in Java 8).

**Implementations.** "The JVM" is a specification. **HotSpot** (in OpenJDK) is the reference implementation; others include **OpenJ9** (Eclipse) and **GraalVM** (which can also compile Java ahead-of-time to a native image). Vendors such as Eclipse Temurin, Amazon Corretto, Microsoft and Oracle ship builds of OpenJDK.

**Where did the JRE go?** Since **Java 11**, Oracle and most vendors stopped shipping a separate JRE download. You either install a full JDK, or use **`jlink`** to build a *custom, trimmed runtime* containing only the modules your app needs — ideal for small Docker images. The *concept* of a JRE (JVM + libraries) is still how you should think about it.

## 7. Syntax and practical Java code examples

**Compile and run by hand:**

```java
// Hello.java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Running on " + System.getProperty("java.vm.name")
                + " " + Runtime.version());
    }
}
```

```bash
javac Hello.java          # JDK tool: produces Hello.class (bytecode)
java Hello                # JVM: loads, verifies, runs Hello.class
javap -c Hello            # JDK tool: disassemble to see the bytecode
java Hello.java           # Java 11+: compile in memory and run a single-file program
```

**Inspect the runtime you are on:**

```java
public class RuntimeInfo {
    public static void main(String[] args) {
        Runtime rt = Runtime.getRuntime();
        System.out.println("Java version : " + Runtime.version());
        System.out.println("VM           : " + System.getProperty("java.vm.name"));
        System.out.println("Vendor       : " + System.getProperty("java.vendor"));
        System.out.println("CPUs         : " + rt.availableProcessors());
        System.out.println("Max heap MB  : " + rt.maxMemory() / (1024 * 1024));
    }
}
```

**See the JIT at work** — run a hot loop with `-XX:+PrintCompilation` and watch methods get compiled:

```java
public class Hot {
    static long square(long x) { return x * x; }

    public static void main(String[] args) {
        long sum = 0;
        for (int i = 0; i < 50_000_000; i++) sum += square(i);
        System.out.println(sum);
    }
}
```

```bash
java -XX:+PrintCompilation Hot | grep square
```

**Build a minimal runtime for a container:**

```bash
jlink --add-modules java.base,java.logging,java.sql \
      --strip-debug --no-header-files --no-man-pages \
      --output /opt/myruntime
```

## 8. Real-world backend development use cases

- **Docker images:** build stage uses a JDK image (`eclipse-temurin:21-jdk`) to compile with Maven; runtime stage uses a JRE-only or `jlink`ed image — smaller, fewer tools for an attacker. (This project's backend `Dockerfile` does exactly this.)
- **Choosing a Java version:** production uses an **LTS** release (8, 11, 17, 21, 25). Spring Boot 3 requires Java 17+.
- **JVM flags in production:** heap sizing (`-Xmx`, or `-XX:MaxRAMPercentage` in containers), GC selection (`-XX:+UseG1GC`, `-XX:+UseZGC`), diagnostics (`-XX:+HeapDumpOnOutOfMemoryError`).
- **Diagnosing live services** with JDK tools: `jcmd <pid> Thread.print` (thread dump), `jcmd <pid> GC.heap_info`, Java Flight Recorder, `jstat`.
- **Warm-up:** a freshly started service is slower until the JIT has compiled hot paths — relevant for autoscaling, load tests and serverless cold starts (one reason GraalVM native images and CDS/AppCDS exist).

## 9. Common mistakes and misconceptions

- ❌ *"Java is interpreted, so it's slow."* Hot code is JIT-compiled to optimised machine code; long-running Java services are typically very fast. Startup and warm-up are the real costs.
- ❌ *"The JVM is platform independent."* The **bytecode** is platform independent. The JVM itself is a native program **built separately for each platform**.
- ❌ *"I need the JRE on my dev machine and the JDK on the server."* It's the opposite: develop with a JDK; servers need only a runtime.
- ❌ Confusing **compile-time** errors (`javac` rejects your source) with **run-time** errors (`NoClassDefFoundError`, `ClassNotFoundException`, `UnsupportedClassVersionError`).
- ❌ Compiling with a newer JDK and running on an older JVM → `UnsupportedClassVersionError` (*class file version 65.0* = Java 21). Use `--release <N>` when targeting older runtimes.
- ❌ Benchmarking with a simple `System.nanoTime()` loop — you measure interpretation + JIT compilation, not steady state. Use **JMH**.

## 10. Performance considerations and best practices

- Prefer an **LTS** JDK from a maintained distribution and keep it patched.
- In containers, size the heap relative to the container limit (`-XX:MaxRAMPercentage=75`) instead of hard-coding `-Xmx` to the host's RAM. Modern JVMs are container-aware.
- Let the JIT do its job: write clear code; avoid premature micro-optimisation. Small methods are fine — the JIT inlines them.
- Warm up before measuring; use **JMH** for micro-benchmarks and profilers (async-profiler, JFR) for real services.
- Reduce startup time with **Class Data Sharing (CDS/AppCDS)**, fewer dependencies, or native images where startup really matters.
- Use `jlink` / slim runtime images to reduce image size and attack surface.

## 11. Interview questions and answers

**Q1. What is the difference between JDK, JRE and JVM?**
The JVM executes bytecode. The JRE is the JVM plus the standard class library — everything needed to *run* Java. The JDK is the JRE plus development tools such as `javac`, `jar`, `javadoc` and `jshell` — everything needed to *build* Java.

**Q2. Is Java compiled or interpreted?**
Both. `javac` compiles source to bytecode ahead of time. At run time the JVM first interprets bytecode, then JIT-compiles frequently executed code to native machine code.

**Q3. Why is Java platform independent but the JVM is not?**
Bytecode targets an abstract machine, so one `.class` works everywhere. The JVM must translate that bytecode into a specific CPU's instructions and use a specific OS's APIs, so each platform needs its own JVM build.

**Q4. What is the JIT compiler and why does it matter?**
A just-in-time compiler inside the JVM that compiles hot bytecode into optimised machine code while the program runs. It can use runtime profiling (which branches are taken, which types actually appear) to optimise more aggressively than a static compiler — at the cost of warm-up time.

**Q5. What happened to the JRE after Java 11?**
Vendors stopped shipping a separate JRE installer. You use a JDK, or create a custom runtime with `jlink` that contains only the modules your application needs.

**Q6. What does `UnsupportedClassVersionError` mean?**
The class was compiled for a newer Java version than the JVM running it. Fix it by running on a newer JVM or compiling with `--release` targeting the older version.

## 12. Related concepts and dependencies

- **Next:** *JVM architecture* — the class loader subsystem, runtime data areas and execution engine in depth.
- **Class loading** — delegation model and why `ClassNotFoundException` happens.
- **JVM memory** and **garbage collection** — heap, stacks, Metaspace and how objects are reclaimed.

## 13. Summary and revision notes

- **JVM** = executes bytecode · **JRE** = JVM + libraries · **JDK** = JRE + dev tools.
- `javac` → bytecode (`.class`) → JVM loads, links (verifies), initialises, then interprets + JIT-compiles.
- Portability comes from **bytecode**; the **JVM itself is platform specific**.
- Tiered JIT: interpreter → C1 → C2; hot code becomes optimised native code; deoptimisation if assumptions break.
- No separate JRE since Java 11 — use a JDK or a `jlink` custom runtime.
- Production: LTS versions, container-aware heap sizing, JDK tools (`jcmd`, JFR) for diagnosis.

# Magnum `DbCodec` NULL handling — analysis and PR sketch

Notes collected while refactoring `DatabaseCodecs.scala` to use `biMap` and
hitting a NULL-related NPE. Written up here so the analysis isn't lost and so
it can seed a potential PR/issue against
[AugustNagro/magnum](https://github.com/AugustNagro/magnum).

## The problem we hit

We refactored the codec for `java.net.URI` to derive from `DbCodec[String]`:

```scala
given DbCodec[URI] =
  DbCodec[String].biMap(URI.create, _.toString)

given [A](using DbCodec[Option[A]]): DbCodec[Maybe[A]] =
  DbCodec[Option[A]].biMap(Maybe.fromOption, _.toOption)
```

All tests failed with:

```
java.lang.NullPointerException:
  Cannot invoke "String.length()" because "this.input" is null
    at java.net.URI.<init>
    at java.net.URI.create
```

Root cause: Magnum's auto-derived `OptionCodec[A]` reads like this:

```scala
def readSingle(rs: ResultSet, pos: Int): Option[A] =
  val a = inner.readSingle(rs, pos)   // called UNCONDITIONALLY
  if rs.wasNull then None else Some(a)
```

It calls the inner codec **first**, then checks `wasNull()` afterward. On a
NULL `TEXT` column:

1. `rs.getString(pos)` returns `null`.
2. `DbCodec[String].readSingle` returns `null: String` without throwing.
3. `biMap` wraps that with `URI.create`, producing `URI.create(null)` → NPE.
4. `OptionCodec` never gets to check `wasNull` — the NPE has already aborted
   the read.

Our short-term fix was a null-tolerant reader:

```scala
given DbCodec[URI] =
  DbCodec[String].biMap(
    s => if s == null then null else URI.create(s),
    _.toString,
  )
```

It works, but it's a leaky abstraction: the inner codec has to know it might
be called on a NULL column, which is exactly the knowledge `OptionCodec` is
supposed to own.

## The deeper issue: silent corruption in combine codecs

The `biMap[URI]` case fails loudly (NPE at decode time). The derived-product
case fails **silently**, which is much worse.

Magnum derives a `DbCodec[Person]` for
`case class Person(id: UUID, name: String, email: String)` by zipping the
field codecs. Its `readSingle` is roughly:

```scala
def readSingle(rs, pos) =
  val id    = uuidCodec.readSingle(rs, pos)
  val name  = stringCodec.readSingle(rs, pos + 1)
  val email = stringCodec.readSingle(rs, pos + 2)
  Person(id, name, email)
```

Now imagine the schema has `email TEXT NULL` by accident, or a `LEFT JOIN`
produces a NULL email. `stringCodec.readSingle` returns `null`, the combine
codec happily stuffs it into `Person`, and user code receives a `Person`
whose `email: String` is actually `null`. Every call site that does
`person.email.toLowerCase` becomes a time bomb. **The type system said
non-null, the value is null, and nothing in between complained.**

This is strictly worse than the `biMap` case because:

1. `biMap` typically NPEs immediately at the lifted function — loud failure,
   easy to diagnose.
2. The combine case *silently* constructs an invalid object. The NPE happens
   later, somewhere else, in code that looks obviously correct.

Critically: **`wasNull` can't save you here**, because `wasNull` only
remembers the most recent getter call. By the time you check it after
constructing the `Person`, it only reflects the last column read. You'd have
to check `wasNull` after every single field read and manually abort — which
no derived combine codec does today.

So the current design has a silent-corruption hole at exactly the place where
it matters most: derived product codecs over schemas with any nullable
columns that weren't modeled as `Option` in Scala.

## The contract is ambiguous

`DbCodec.readSingle` today is doing two jobs in one method:

1. Read a value at this position.
2. By the way, the value might be SQL NULL, in which case return something
   harmless that survives until `OptionCodec` checks `wasNull()`.

Job #2 is invisible in the type signature. It forces:

- Every `biMap` user to defensively null-check.
- Primitive codecs to pick a sentinel (`null` for reference types, `0` for
  `Int`, etc.) that must not trip up downstream transforms.
- Composite codecs to be robust against partially-null reads even when the
  overall column group is declared `NOT NULL`.

## Design alternatives considered

### Option 1 — `OptionCodec` checks the column first

Minimal, one-line fix inside Magnum, no public API change:

```scala
def readSingle(rs: ResultSet, pos: Int): Option[A] =
  // Peek first. getObject is the one accessor that returns null on NULL
  // for every SQL type without throwing.
  if rs.getObject(pos) == null then None
  else Some(inner.readSingle(rs, pos))
```

- **Pros:** tiny diff, fixes every downstream `biMap` codec for free,
  allocation-free, inner codec never sees NULL.
- **Cons:** double `ResultSet` access per column (cheap but nonzero).
  Does **not** fix the combine-codec silent-corruption problem.

### Option 2 — Split the contract into two methods

```scala
trait DbCodec[A]:
  /** Read assuming the column(s) are non-null. May throw on NULL. */
  def readNonNull(rs: ResultSet, pos: Int): A

  /** Read, returning None on NULL. Default for single-column codecs
    * uses wasNull; multi-column codecs override. */
  def readNullable(rs: ResultSet, pos: Int): Option[A] = ...
```

`OptionCodec[A]` calls `readNullable`. `biMap` lifts `readNonNull` only.

- **Pros:** null-handling responsibility is explicit in the type.
  Multi-column codecs can implement `readNullable` properly.
- **Cons:** bigger API change; source-breaks anyone implementing `DbCodec`
  directly; still has to define "nullable" semantics for multi-column reads.

### Option 3 — throw-and-catch on primitives

```scala
trait DbCodec[A]:
  def readSingle(rs: ResultSet, pos: Int): A  // throws NullValueException on NULL

given [A](using inner: DbCodec[A]): DbCodec[Option[A]] with
  def readSingle(rs, pos) =
    try Some(inner.readSingle(rs, pos))
    catch case _: NullValueException => None
```

- **Pros:** contract is crisp (`readSingle` returns `A`, never null). `biMap`
  is trivially correct. No double ResultSet access.
- **Cons:**
  - Exceptions as control flow. Mitigable with a singleton
    `NullValueException extends NoStackTrace`, collapsing cost to ~10–50ns
    per NULL. Still nonzero, paid forever on every nullable read.
  - Need to distinguish "NULL in DB" from "real NPE in user code." If
    `OptionCodec` catches too broadly, real bugs become silent `None`s. Every
    primitive codec must remember to throw the *specific* `NullValueException`
    type — a contract that's easy to get wrong, which is the very thing we
    were trying to escape.
  - Every primitive codec has to actively check and throw. That pushes the
    `wasNull` dance into every codec instead of centralizing it.
  - Doesn't fix the multi-column `wasNull` trap.

### Option 4 — `Either`-like sum type for read results

```scala
enum ReadResult[+A]:
  case Value(a: A)
  case Null

// biMap
def biMap[B](to: A => B, from: B => A): DbCodec[B] = new:
  def readSingle(rs, pos) = self.readSingle(rs, pos) match
    case ReadResult.Value(a) => ReadResult.Value(to(a))
    case ReadResult.Null     => ReadResult.Null
```

- **Pros:** no exceptions; pure data flow; NULL is a first-class case in the
  return type. Composes cleanly. Could extend with `ReadResult.Error` to
  unify decode error reporting.
- **Cons:** allocation on every read (`Value(a)` wrapper). For a 20-column ×
  10k-row query that's 200k extra allocations. Escape analysis sometimes
  eliminates these, but not reliably across a virtual call. Probably the
  real reason Magnum doesn't do it today — JDBC-layer code is hot.
- Bigger API break than Option 1/3.

### Option 5 — **Opaque type over `A | Null`** (recommended)

Take Option 4's clarity and erase the wrapper at runtime. There are two
defensible spellings, differing only in where they put the `<: AnyRef`
bound; the runtime behaviour is the same, the difference is how honest the
type signature is about allocation.

**Spelling A — bounded (honest about boxing):**

```scala
opaque type ReadResult[+A <: AnyRef] = A | Null
object ReadResult:
  inline def Value[A <: AnyRef](a: A): ReadResult[A] = a
  inline def Null: ReadResult[Null]                  = null
  extension [A <: AnyRef](r: ReadResult[A])
    inline def isNull: Boolean                  = r == null
    inline def getOrElse[B >: A](fallback: => B): B =
      if r == null then fallback else r.asInstanceOf[A]
    inline def map[B <: AnyRef](f: A => B): ReadResult[B] =
      if r == null then null else f(r.asInstanceOf[A])
```

Primitive columns are forced to spell their codec as
`DbCodec[java.lang.Integer]` (or `Long`, `Double`, …), with a thin `biMap`
wrapper for users who want Scala `Int`. The boxing is visible in the type.

**Spelling B — unbounded (ergonomic):**

```scala
opaque type ReadResult[+A] = A | Null
```

Primitive columns can be spelled `DbCodec[Int]` directly, and a codec
implementation looks natural:

```scala
val IntCodec: DbCodec[Int] = new:
  def readSingle(rs, pos): ReadResult[Int] =
    val i = rs.getInt(pos)
    if rs.wasNull then ReadResult.Null
    else ReadResult.Value(i)
```

#### Why both spellings box primitive columns equally

Scala 3's union types erase to their LUB. `Int | Null` has LUB
`java.lang.Object` — that's the only JVM type that can hold both "an int
value" and "the null reference" in a single slot. JVM `int` is 32 bits with
no null bit pattern; `null` is a reference; the two can't share a slot
unboxed.

So in Spelling B, when you write:

```scala
ReadResult.Value(i)   // i: Int
```

…the compiler must produce a value of erased type `Object`. To put an `Int`
into an `Object`-typed slot, the bytecode emits
`invokestatic java/lang/Integer.valueOf`. **That is a box**, inserted
silently into the bytecode even though your source says `Int`. On the read
side, `r.asInstanceOf[Int]` emits `checkcast java/lang/Integer` followed by
`invokevirtual intValue` — **an unbox** — per column read.

Spelling A and Spelling B produce the same bytecode for a primitive column.
The difference is entirely in what the user sees in the type signature:

- **Spelling A** says `DbCodec[java.lang.Integer]`. The box is in the type.
  Users of `Int` columns pay a visible `biMap` wrapper. Harder to claim
  "allocation-free" and be wrong.
- **Spelling B** says `DbCodec[Int]`. The box is hidden behind the opaque
  type's erasure. Source is ergonomic; bytecode is identical to A.

#### Combine codec under the new scheme

Either spelling, the derivation becomes:

```scala
def readSingle(rs, pos): ReadResult[Person] =
  val id    = uuidCodec.readSingle(rs, pos)
  val name  = stringCodec.readSingle(rs, pos + 1)
  val email = stringCodec.readSingle(rs, pos + 2)
  if id.isNull || name.isNull || email.isNull then ReadResult.Null
  else ReadResult.Value(Person(
    id.asInstanceOf[UUID],
    name.asInstanceOf[String],
    email.asInstanceOf[String],
  ))
```

Effects, independent of which spelling is chosen:

- A NULL in any non-`Option` field produces `ReadResult.Null` at the combine
  level. `OptionCodec[Person]` turns that into `None`; a non-optional
  top-level read turns it into a clean
  `DecodeError("unexpected NULL in non-nullable column ...")`. Either way,
  **`Person` is never constructed with a null field** — the type system's
  promise is kept.
- `biMap` gets the same protection for free — `to` is only called inside
  `map`, which short-circuits on `Null`.
- **Allocation on reference columns: zero.** At runtime `ReadResult[A]` *is*
  `A | Null`, i.e. either the underlying reference or the JVM null. `Value(a)`
  is identity; `Null` is `null`. The wrapper fully erases.
- **Allocation on primitive columns: one box per non-null read**, unavoidable
  regardless of spelling, for the erasure reasons above. `Integer.valueOf` has
  a cache for `-128..127` so small integers don't allocate, but anything
  outside the cache is a fresh heap allocation per row per primitive column.
  Today's `rs.getInt(pos) + wasNull` path has no such cost.

This is the "right" fix: it eliminates the silent-corruption hole and fixes
`biMap` ergonomics. The honest tradeoff is a small per-read box on primitive
columns, which wasn't there before.

## Open question — which spelling should the PR use?

Both spellings compile, both produce identical bytecode, both close the
silent-corruption hole. The choice is purely about what the *type* tells
users:

- **Spelling A (`A <: AnyRef`)**: boxing is visible in the type. Harder to
  accidentally claim "zero-cost" in a PR description. Uglier for primitive
  columns — every primitive codec needs a companion `biMap` unwrapper for
  users who want Scala `Int`/`Long`/`Double`.
- **Spelling B (unbounded)**: type signature matches user intuition
  (`DbCodec[Int]` reads as "Int column"). The box is hidden in erasure, so
  users may be surprised by allocation profile; needs a prominent
  documentation note about "primitive columns box at the codec boundary."
- **Escape hatch — separate hierarchy for primitives**: keep
  `DbCodec[A <: AnyRef]` with `ReadResult[A]` for reference columns, add a
  parallel `DbCodecPrim[A]` that returns unboxed `A` plus an explicit
  `isNull: Boolean`. Zero-allocation on primitives, ugliest API. Probably
  overkill unless benchmarks show the box is unacceptable.

My recommendation for the PR: **lead with Spelling B** because the
ergonomic win is real and primitive-column allocation is documentable, but
**present both in the PR description** and let August pick. The important
thing is that the PR is honest about the boxing regardless of which spelling
ships: *some per-read allocation happens on primitive columns that doesn't
happen today, independent of how the type is spelled.*

Decide after benchmarking. If the primitive-column regression is in the noise
for realistic workloads, Spelling B is clearly better. If it's painful, the
separate-hierarchy escape hatch becomes worth the ugliness.

## Caveats for the opaque-type design

1. **Primitive columns box (unavoidable).** Discussed above. Paid per
   non-null read, per primitive column. Cacheable for `-128..127` via
   `Integer.valueOf`, a real allocation otherwise. Worth benchmarking on a
   wide analytical query against `int`/`long`/`double` columns before the
   PR claims "no regression."

2. **Opaque-type variance with `Null`.** Scala 3 is picky about variance and
   `Null` bounds, especially in Spelling B where `A` can be a value type.
   Prototype both spellings in a scratch worksheet before committing to a
   signature — union types with variance have surprised me before.

3. **Migration path.** Source-breaking for anyone with a hand-written
   `DbCodec`. Soften by:
   - Keeping the current `def readSingle(rs, pos): A` as `@deprecated`.
   - Adding a new `def read(rs, pos): ReadResult[A]` with a default that
     calls the old method and checks `wasNull` for backward compatibility.
   - Having combine / `biMap` / `OptionCodec` call `read`, not `readSingle`.
   - Removing `readSingle` in a follow-up release.

4. **Multi-column `wasNull` is still a hand-written trap.** The new API
   *lets* you do the right thing, but a careless hand-written combine codec
   could still ignore `ReadResult.Null` from a sub-read. The **derivation
   macro** should bake in the "any-null → Null" policy so users who rely on
   Magnum's derivation get it automatically. Document the rule for manual
   implementors.

5. **Prior art.** Doobie splits `Get[A]` (single column) from `Read[A]`
   (row shape), with NULL handling centralized. Anorm, Slick, and sqlx
   (Rust) have each landed on similar splits. Referencing how peer libraries
   solved the same hole strengthens the PR considerably.

## Recommended rollout

- **Immediate (zero-risk) fix:** land Option 1 — `OptionCodec` peeks with
  `rs.getObject(pos) == null` before calling the inner codec. Eliminates the
  `biMap` footgun in one line with no API change. Allocation-free. Does not
  fix the combine-codec hole, but also doesn't block a later fix for it.

- **Proper fix:** Option 5 (opaque-type `ReadResult`), with the deprecation
  path above. Closes the silent-corruption hole in derived product codecs
  and cleans up the contract.

The two can ship in the same PR or in sequence; Option 1 first buys time.

## PR description checklist

When opening the PR / issue, lead with the **scary** case, not the
ergonomic one:

1. **Minimal repro of silent corruption** in a derived product codec: a
   `case class` with three `String` fields against a table where one column
   is accidentally nullable, showing that the derived codec silently
   produces an instance with a `null` field, and that the NPE surfaces
   pages of stack later in unrelated user code.
2. **Why `wasNull` can't fix it inside the current API** — the
   last-getter-call behaviour of `wasNull`.
3. **The opaque-type design**, with both spellings (bounded vs unbounded)
   presented as an open choice, and an explicit note that the wrapper fully
   erases on reference columns but *does* insert `Integer.valueOf`/`intValue`
   on primitive columns regardless of spelling — because `A | Null` has to
   erase to `java.lang.Object` on the JVM.
4. **Deprecation path** for `readSingle`.
5. **Benchmarks:** no regression on the hot read path for non-null reference
   columns; measured cost for primitive columns due to unavoidable boxing.
   Name the number — don't hand-wave. Include a wide-table case (e.g. 20
   columns × 10k rows, mix of `int`/`text`/`timestamp`) so the primitive-box
   cost is visible if it's going to be a problem.
6. **The `biMap[URI]` NPE** as a secondary motivating example.

Frame the PR around *silent corruption in derived product codecs* rather than
*`biMap` ergonomics*. The `biMap` thing is annoying; the combine thing is a
soundness hole. Lead with the soundness hole.

## Before opening the PR

Search Magnum's issue tracker for `null`, `Option`, `wasNull`. August may
have thought about this already. If he has an opinion or a reason the
current design is the way it is, find out before writing the code. If he
hasn't, open an issue first ("I'd like to propose X, here's the repro, is a
PR welcome?") — usually gets a better reception than a large unannounced PR.

# Mini-cours Spring Data JPA : 3 façons d’écrire des requêtes (avec exemples)

Ce document explique les 3 façons principales de faire des requêtes avec Spring Data JPA :
1) **Derived Queries (Keywords)**
2) **JPQL (`@Query`)**
3) **SQL natif (`nativeQuery = true`)**

---

## 0) Base : Entities + Repositories

### Exemple d’entités

```java
// User.java
@Entity
@Table(name = "users")
public class User {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nom;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private boolean active;

  // getters/setters...
}
```

```java
// Booking.java
@Entity
@Table(name = "bookings")
public class Booking {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private BigDecimal total;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  // getters/setters...
}
```

### Exemple de repository (CRUD auto)

```java
public interface UserRepository extends JpaRepository<User, Long> {
}
```

> `JpaRepository<T, ID>` fournit déjà : `save`, `findAll`, `findById`, `deleteById`, pagination et tri.

---

## 1) Derived Queries (Keywords)

Ici, **le nom de la méthode** détermine la requête. Pratique pour des filtres simples.

### A) Requêtes simples

```java
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  List<User> findByActiveTrue();

  List<User> findByNomContainingIgnoreCase(String keyword);

  List<User> findByNomStartingWith(String prefix);

  List<User> findByIdIn(List<Long> ids);

  List<User> findTop5ByActiveTrueOrderByIdDesc();
}
```

### B) AND / OR

```java
List<User> findByActiveTrueAndNomContainingIgnoreCase(String keyword);

List<User> findByActiveFalseOrEmailContainingIgnoreCase(String part);
```

### C) Pagination & tri

```java
Page<User> findByActiveTrue(Pageable pageable);
```

Exemple d’appel :

```java
Page<User> page = userRepo.findByActiveTrue(
  PageRequest.of(0, 10, Sort.by("nom").ascending())
);
```

✅ **À choisir quand** : requête courte et standard.

⚠️ **Limite** : devient vite illisible si la requête est complexe.

---

## 2) JPQL avec `@Query`

JPQL est une requête orientée **objets (entités)** : on parle de `User`, `Booking`, et de leurs champs.

### A) Exemple simple

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Query("select u from User u where lower(u.email) = lower(:email)")
  Optional<User> findByEmailJPQL(@Param("email") String email);
}
```

### B) JOIN (ex: bookings des users actifs avec total minimum)

```java
public interface BookingRepository extends JpaRepository<Booking, Long> {

  @Query("""
    select b from Booking b
    join b.user u
    where u.active = true and b.total > :min
  """)
  List<Booking> findBigBookingsFromActiveUsers(@Param("min") BigDecimal min);
}
```

### C) Projection DTO

DTO :

```java
public record BookingSummary(Long bookingId, String userEmail, BigDecimal total) {}
```

Repository :

```java
public interface BookingRepository extends JpaRepository<Booking, Long> {

  @Query("""
    select new tn.yourpkg.BookingSummary(b.id, u.email, b.total)
    from Booking b join b.user u
    where b.createdAt >= :from
  """)
  List<BookingSummary> findSummaries(@Param("from") LocalDateTime from);
}
```

### D) Paramètres optionnels (astuce fréquente)

```java
@Query("""
  select u from User u
  where (:active is null or u.active = :active)
    and (:q is null or lower(u.nom) like lower(concat('%', :q, '%')))
""")
Page<User> search(@Param("active") Boolean active,
                  @Param("q") String q,
                  Pageable pageable);
```

### E) Update/Delete (avec `@Modifying`)

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Modifying
  @Query("update User u set u.active = false where u.id = :id")
  int deactivate(@Param("id") Long id);
}
```

> En pratique, on appelle ça depuis un service `@Transactional`.

✅ **À choisir quand** : requêtes plus riches, JOIN, DTO, filtres optionnels, portable.

---

## 3) SQL natif (`nativeQuery = true`)

Ici tu écris du **SQL brut** (tables/colonnes). Utile quand tu as besoin de fonctions spécifiques DB ou d’une requête très optimisée.

### A) Retourner une entité

```java
public interface UserRepository extends JpaRepository<User, Long> {

  @Query(value = "select * from users where active = true order by id desc", nativeQuery = true)
  List<User> findActiveUsersNative();
}
```

### B) Projection interface

```java
public interface UserEmailOnly {
  Long getId();
  String getEmail();
}

public interface UserRepository extends JpaRepository<User, Long> {

  @Query(value = "select id, email from users where active = :active", nativeQuery = true)
  List<UserEmailOnly> findEmails(@Param("active") boolean active);
}
```

### C) Pagination en natif (souvent avec `countQuery`)

```java
@Query(
  value = "select * from users where active = :active",
  countQuery = "select count(*) from users where active = :active",
  nativeQuery = true
)
Page<User> findByActiveNative(@Param("active") boolean active, Pageable pageable);
```

### D) Update/Delete natif

```java
@Modifying
@Query(value = "update users set active = false where id = :id", nativeQuery = true)
int deactivateNative(@Param("id") Long id);
```

✅ **À choisir quand** : besoin spécifique DB (CTE, fonctions, perfs), ou requête SQL avancée.

⚠️ **Inconvénients** : moins portable + mapping parfois plus compliqué.

---

## Bonnes pratiques (sécurité)

- Utilise **toujours des paramètres** `:id`, `:email` (pas de concaténation de strings) → évite l’injection.
- Ordre conseillé : **Keywords** (simple) → **JPQL** (complexe portable) → **Native** (spécifique/perf).
- Pour `@Modifying`, exécuter dans un contexte `@Transactional` côté service.

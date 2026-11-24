# M450 LB2 – Testkonzept Integration (GymTracker)

**Modul 450 – Applikationen testen**
**Team:** Christian Mitev, Mike Mamie, Julian Schiller · **Klasse:** Ina24-28a · **Datum:** 17.11.2026
**Testobjekt:** GymTracker (Spring-Boot-Applikation aus LB1)

> Abgabedatei: `M450_LB2_Ina24-28a_Mitev_Mamie_Schiller.pdf`

---

## 1 Einleitung

Dieses Dokument beschreibt Plan und Durchführung der Integrationstests für die
Software aus LB1 (GymTracker). Ziel ist sicherzustellen, dass die Integration
zwischen Programmcode und Datenbank reibungslos funktioniert, das heisst: dass die
Geschäftslogik (Service-Schicht) korrekt mit der relationalen Datenbank über JPA/
Hibernate zusammenspielt.

Während in LB1 die Units mit Mocks isoliert getestet wurden (die DB wurde bewusst
nicht kontaktiert), prüfen wir in LB2 das Gegenteil: das Zusammenspiel von Code und
**echter** Datenbank. Dazu werden die Tests gegen eine echte PostgreSQL-Datenbank
ausgeführt, die über Testcontainers in einem Docker-Container bereitgestellt wird.

Die Testumgebung, die drei Schwerpunkte mit Begründung, die Testfälle sowie deren
Durchführung und Ergebnisse werden im Folgenden im Detail behandelt.

---

## 2 Testumgebung

Die Integrationstests laufen vollständig automatisiert (JUnit 5) und sind lokal wie
auch in der CI-Pipeline reproduzierbar.

### Softwarekonfiguration

| Komponente | Version / Technologie |
|---|---|
| Sprache / JDK | Java 21 (Temurin 21.0.5 LTS) |
| Framework | Spring Boot 3.5.6 (Spring Data JPA, Hibernate) |
| Datenbank (Produktion) | PostgreSQL |
| Datenbank (Test) | PostgreSQL 16 (Image `postgres:16-alpine`) via Testcontainers 1.21 |
| Container-Runtime | Docker 27.5.1 (Docker Desktop) |
| Test-Frameworks | JUnit 5, AssertJ, Spring Boot Test |
| Build / Ausführung | Maven 4, `mvn test` |
| IDE | IntelliJ IDEA / VS Code |

### Hardwarekonfiguration

Die Tests wurden von den drei Teammitgliedern auf ihren eigenen Entwickler-Notebooks
ausgeführt. Da die Datenbank über Docker/Testcontainers bereitgestellt wird, ist die
Testumgebung trotz unterschiedlicher Betriebssysteme auf allen Geräten identisch.

| Teammitglied | Betriebssystem | Rolle |
|---|---|---|
| Christian Mitev | Windows 11 Enterprise (10.0.26300) | Schwerpunkt 1 – CRUD/Transaktionen |
| Mike Mamie | Windows 11 Home (10.0.26300) | Schwerpunkt 2 – Datenintegrität |
| Julian Schiller | Windows 11 Home (10.0.26300) | Schwerpunkt 3 – Fehlerbehandlung, Infrastruktur |

### Aufbau der Testumgebung

- Beim Start der Testsuite wird **ein** PostgreSQL-Container hochgefahren (Singleton,
  geteilt über alle Testklassen, Abbau beim Beenden der JVM).
- Spring Boot verbindet sich über `@DynamicPropertySource` automatisch mit diesem
  Container (`IntegrationTestBase`).
- Das Datenbankschema wird aus den JPA-Entitäten erzeugt (`ddl-auto=create-drop`).
- Die Seed-Daten aus `data.sql` werden im Testprofil **nicht** eingespielt
  (`spring.sql.init.mode=never`), damit jeder Test auf einem definierten, leeren
  Datenbestand startet und seine Testdaten selbst anlegt.
- Vor jedem Test wird die DB geleert (`@BeforeEach resetDatabase`). Lesende/
  schreibende Tests laufen in einer eigenen Transaktion, die anschliessend
  zurückgerollt wird (Isolation zwischen den Tests).

Die Datenbank besitzt **vier Tabellen** (Anforderung: mindestens 3):
`member`, `exercise`, `workout`, `workout_set`.

---

## 3 Schwerpunkte und Begründung

Aus den vorgeschlagenen Integrationskomponenten haben wir drei Schwerpunkte gewählt,
die für eine datenbankgestützte Applikation den grössten Nutzen bringen.

**Aufgabenteilung im Team:** Jeder Schwerpunkt wurde von einem Teammitglied
eigenständig umgesetzt und ausgeführt – Schwerpunkt 1 von **Christian Mitev**,
Schwerpunkt 2 von **Mike Mamie**, Schwerpunkt 3 von **Julian Schiller** (der zudem
die gemeinsame Testinfrastruktur aufgesetzt hat).

### 3.1 Transaktionsprüfungen / CRUD

**Begründung:** Create-, Read-, Update- und Delete-Operationen sind der Kern jeder
datenbankgestützten Applikation. Wir prüfen, dass Daten über die Service-Schicht
tatsächlich korrekt in die DB geschrieben, wieder gelesen, verändert und gelöscht
werden und dass dabei die ACID-Eigenschaften gewahrt bleiben – insbesondere
**Atomicity** (eine fehlgeschlagene Transaktion hinterlässt keine Teildaten) und das
korrekte **Cascade**-Verhalten (Löschen eines Workouts entfernt auch seine Sätze).
Fehler hier hätten direkte Auswirkungen auf jede Funktion der Anwendung.

### 3.2 Datenintegrität

**Begründung:** Die korrekte Speicherung und der zuverlässige Abruf von Daten sind
essenziell. Die Datenbank-Constraints (Unique, NOT NULL, Fremdschlüssel) sind die
letzte Verteidigungslinie gegen inkonsistente Daten. Wir prüfen, dass die aus den
Entitäten generierten Constraints in der echten PostgreSQL auch wirklich greifen und
dass Werte verlustfrei gespeichert und gelesen werden. Inkonsistente Daten könnten
zu Fehlentscheidungen und fehlerhaften Berechnungen (z. B. Trainingsvolumen) führen.

### 3.3 Fehlerbehandlung

**Begründung:** Eine robuste Applikation muss auf fehlerhafte Anfragen und fehlende
Daten kontrolliert reagieren, statt abzustürzen oder inkonsistente Zustände zu
erzeugen. Wir prüfen, dass der Service bei unbekannten Datensätzen und bei Verstössen
gegen Geschäftsregeln klar definierte, aussagekräftige Exceptions wirft (die der
`ErrorHandler` später in passende HTTP-Statuscodes übersetzt). Das sichert eine
nachvollziehbare und vorhersehbare Reaktion im Zusammenspiel mit der DB.

---

## 4 Mängelklassifizierung

| Nr. | Mängelklasse | Beschreibung |
|---|---|---|
| 0 | mängelfrei | Einwandfrei und anforderungsgerecht |
| 1 | belangloser Mangel | Verwendung möglich, Brauchbarkeit vorhanden, sollte dennoch nicht vorkommen |
| 2 | leichter Mangel | Verwendung möglich, Brauchbarkeit nur wenig beeinträchtigt |
| 3 | schwerer Mangel | Verwendung noch möglich, Brauchbarkeit stark verringert |
| 4 | kritischer Mangel | Unbrauchbar; wesentliche Funktionalität fehlt; Betrieb nicht verantwortbar |

**Gesamtergebnis der Durchführung vom 17.11.2026:** 15 Integrationstests ausgeführt,
15 bestanden, 0 fehlgeschlagen → alle Testfälle **Mängelklasse 0 (mängelfrei)**.
Automatisierter Lauf: `mvn test` → `Tests run: 50, Failures: 0, Errors: 0` (35 Unit-
Tests aus LB1 + 15 Integrationstests aus LB2), `BUILD SUCCESS`.

---

## 5 Beschreibung der Testfälle

Jeder Testfall ist eindeutig nummeriert und als automatisierter JUnit-Test
umgesetzt. Die Spalte „Automatisierter Test" verweist auf Klasse und Methode zur
Nachvollziehbarkeit. Alle Tests liegen unter
`src/test/java/ch/m450/gymtracker/integration/`.

### 5.1 Schwerpunkt 1 – Transaktionsprüfungen / CRUD

**Durchgeführt von:** Christian Mitev (Windows 11 Enterprise)

**In-Scope:** Anlegen, Lesen, Ändern und Löschen von Workouts und Sätzen über die
Service-Schicht gegen die echte DB; Cascade-Verhalten; Atomicity einer fehlerhaften
Transaktion.
**Out-of-Scope:** Nebenläufige Zugriffe mehrerer Benutzer (Isolation/Concurrency), da
die Applikation für Single-User ausgelegt ist. Performance/Last wird nicht getestet.

| Feld | Inhalt |
|---|---|
| **ID** | T-101 |
| **Beschreibung** | CREATE: `startWorkout` speichert ein Workout dauerhaft in der DB |
| **Testvoraussetzung** | DB leer, ein Mitglied angelegt |
| **Testschritte** | `startWorkout(memberId, heute)` aufrufen, Persistence-Context leeren, Workout per `findById` neu laden |
| **Erwartetes Ergebnis** | Workout hat eine generierte ID, ist in der DB vorhanden, Datum und Mitglied stimmen |
| **Automatisierter Test** | `CrudIntegrationTest.create_persistsWorkout` |
| **Ausführung** | Tester: Christian Mitev · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-102 |
| **Beschreibung** | READ: `totalVolume` liest mehrere Sätze korrekt aus der DB zusammen |
| **Testvoraussetzung** | Mitglied, Übung und ein Workout mit zwei Sätzen (8×60, 5×20) angelegt |
| **Testschritte** | Sätze über `addSet` speichern, Context leeren, `totalVolume(memberId)` aufrufen |
| **Erwartetes Ergebnis** | Gesamtvolumen = 580.0 (480 + 100), aus der DB gelesen |
| **Automatisierter Test** | `CrudIntegrationTest.read_totalVolumeFromDb` |
| **Ausführung** | Tester: Christian Mitev · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-103 |
| **Beschreibung** | UPDATE: `addSet` fügt einen Satz hinzu, der nach Reload persistiert ist |
| **Testvoraussetzung** | Mitglied, Übung und ein Workout angelegt |
| **Testschritte** | `addSet(workoutId, exerciseId, 10, 80)`, Context leeren, Workout neu laden |
| **Erwartetes Ergebnis** | Workout hat genau einen Satz mit reps=10, weight=80, korrekter Übung |
| **Automatisierter Test** | `CrudIntegrationTest.update_addSetIsPersisted` |
| **Ausführung** | Tester: Christian Mitev · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-104 |
| **Beschreibung** | DELETE: `deleteWorkout` entfernt das Workout samt seiner Sätze (Cascade) |
| **Testvoraussetzung** | Workout mit einem Satz angelegt (1 Zeile in `workout_set`) |
| **Testschritte** | `deleteWorkout(workoutId)`, danach Workout und Satz-Anzahl prüfen |
| **Erwartetes Ergebnis** | Workout nicht mehr vorhanden, 0 Zeilen in `workout_set` (keine Waisen) |
| **Automatisierter Test** | `CrudIntegrationTest.delete_cascadesToSets` |
| **Ausführung** | Tester: Christian Mitev · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-105 |
| **Beschreibung** | ATOMICITY: Eine Transaktion, die mit einem Fehler abbricht, rollt alle Schreibvorgänge zurück |
| **Testvoraussetzung** | DB leer, programmatische Transaktion (`TransactionTemplate`) |
| **Testschritte** | In einer Transaktion ein Mitglied „Ghost" speichern, dann eine Exception werfen |
| **Erwartetes Ergebnis** | Exception wird propagiert, „Ghost" ist **nicht** in der DB (vollständiger Rollback) |
| **Automatisierter Test** | `TransactionRollbackIntegrationTest.failedTransactionRollsBackCompletely` |
| **Ausführung** | Tester: Christian Mitev · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

**Testprotokoll (Konsolenausgabe `mvn test`):**

```text
[INFO] Running ch.m450.gymtracker.integration.CrudIntegrationTest
  [OK] T-101 CREATE: startWorkout speichert ein Workout dauerhaft in der DB
  [OK] T-102 READ:   totalVolume liest mehrere Saetze korrekt aus der DB zusammen
  [OK] T-103 UPDATE: addSet fuegt einen Satz hinzu, der nach Reload persistiert ist
  [OK] T-104 DELETE: deleteWorkout entfernt das Workout samt seiner Saetze (Cascade)
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 21.77 s
[INFO] Running ch.m450.gymtracker.integration.TransactionRollbackIntegrationTest
  [OK] T-105 ATOMICITY: Fehler in der Transaktion rollt alle Schreibvorgaenge zurueck
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.09 s
```

### 5.2 Schwerpunkt 2 – Datenintegrität

**Durchgeführt von:** Mike Mamie (Windows 11 Home)

**In-Scope:** Durchsetzung der DB-Constraints (Unique auf Übungsname, NOT NULL/
Fremdschlüssel für Mitglied und Übung), verlustfreies Speichern und Lesen von Werten,
Konsistenz nach Cascade-Löschung.
**Out-of-Scope:** Datenbank-interne Mechanismen wie Backup/Recovery oder
Verschlüsselung at-rest; diese liegen in der Verantwortung des DB-Betriebs.

| Feld | Inhalt |
|---|---|
| **ID** | T-201 |
| **Beschreibung** | UNIQUE: Zwei Übungen mit gleichem Namen werden abgewiesen |
| **Testvoraussetzung** | Übung „Bankdrücken" bereits gespeichert |
| **Testschritte** | Zweite Übung „Bankdrücken" speichern und flushen |
| **Erwartetes Ergebnis** | `DataIntegrityViolationException` (Unique-Constraint `exercise_name_key`) |
| **Automatisierter Test** | `DataIntegrityIntegrationTest.uniqueExerciseNameIsEnforced` |
| **Ausführung** | Tester: Mike Mamie · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-202 |
| **Beschreibung** | NOT NULL: Ein Workout ohne Mitglied wird abgewiesen |
| **Testvoraussetzung** | DB leer |
| **Testschritte** | `Workout` mit `member = null` speichern und flushen |
| **Erwartetes Ergebnis** | `DataIntegrityViolationException` (Spalte `member_id` NOT NULL) |
| **Automatisierter Test** | `DataIntegrityIntegrationTest.workoutRequiresMember` |
| **Ausführung** | Tester: Mike Mamie · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-203 |
| **Beschreibung** | NOT NULL / FK: Ein Satz ohne Übung wird abgewiesen |
| **Testvoraussetzung** | Mitglied und Workout angelegt |
| **Testschritte** | Satz mit `exercise = null` an das Workout hängen, speichern und flushen |
| **Erwartetes Ergebnis** | `DataIntegrityViolationException` (Spalte `exercise_id` NOT NULL/FK) |
| **Automatisierter Test** | `DataIntegrityIntegrationTest.setRequiresExercise` |
| **Ausführung** | Tester: Mike Mamie · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-204 |
| **Beschreibung** | ROUND-TRIP: Gespeicherte Werte werden unverändert wieder gelesen |
| **Testvoraussetzung** | Mitglied (80 kg), Übung, Workout angelegt |
| **Testschritte** | Satz mit reps=8, weight=62.5 speichern, Context leeren, neu laden |
| **Erwartetes Ergebnis** | reps=8, weight=62.5 und Körpergewicht=80.0 exakt wie gespeichert |
| **Automatisierter Test** | `DataIntegrityIntegrationTest.valuesAreStoredAndReadBackExactly` |
| **Ausführung** | Tester: Mike Mamie · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-205 |
| **Beschreibung** | CASCADE: Löschen eines Workouts hinterlässt keine verwaisten Sätze, die Übung bleibt erhalten |
| **Testvoraussetzung** | Workout mit zwei Sätzen angelegt |
| **Testschritte** | Workout löschen, danach Satz-Anzahl und Existenz der Übung prüfen |
| **Erwartetes Ergebnis** | 0 Zeilen in `workout_set`, die referenzierte Übung existiert weiterhin |
| **Automatisierter Test** | `DataIntegrityIntegrationTest.deletingWorkoutLeavesNoOrphanSets` |
| **Ausführung** | Tester: Mike Mamie · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

**Testprotokoll (Konsolenausgabe `mvn test`):**

```text
[INFO] Running ch.m450.gymtracker.integration.DataIntegrityIntegrationTest
  [OK] T-201 UNIQUE:     zwei Uebungen mit gleichem Namen werden abgewiesen
  [OK] T-202 NOT NULL:   Workout ohne Mitglied wird abgewiesen
  [OK] T-203 NOT NULL/FK: Satz ohne Uebung wird abgewiesen
  [OK] T-204 ROUND-TRIP: gespeicherte Werte werden unveraendert gelesen
  [OK] T-205 CASCADE:    Loeschen hinterlaesst keine verwaisten Saetze
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.31 s
```

### 5.3 Schwerpunkt 3 – Fehlerbehandlung

**Durchgeführt von:** Julian Schiller (Windows 11 Home)

**In-Scope:** Reaktion des Service auf unbekannte Datensätze (Mitglied, Workout,
Übung) und auf Verstösse gegen Geschäftsregeln (kein zweites Workout pro Tag) beim
Zugriff auf die echte DB.
**Out-of-Scope:** Simulation eines kompletten Verbindungsabbruchs/DB-Ausfalls zur
Laufzeit (Infrastruktur-Thema) sowie Sicherheitsaspekte wie SQL-Injection; letzteres
liegt beim IT-Security-Department.

| Feld | Inhalt |
|---|---|
| **ID** | T-301 |
| **Beschreibung** | Unbekanntes Mitglied: `startWorkout` mit nicht existierender ID |
| **Testvoraussetzung** | DB leer |
| **Testschritte** | `startWorkout(999999, heute)` aufrufen |
| **Erwartetes Ergebnis** | `NoSuchElementException` mit Meldung, die „Mitglied" enthält |
| **Automatisierter Test** | `ErrorHandlingIntegrationTest.startWorkoutWithUnknownMember` |
| **Ausführung** | Tester: Julian Schiller · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-302 |
| **Beschreibung** | Unbekanntes Workout: `addSet` auf nicht existierendes Workout |
| **Testvoraussetzung** | Eine Übung angelegt |
| **Testschritte** | `addSet(999999, exerciseId, 8, 60)` aufrufen |
| **Erwartetes Ergebnis** | `NoSuchElementException` mit Meldung, die „Workout" enthält |
| **Automatisierter Test** | `ErrorHandlingIntegrationTest.addSetToUnknownWorkout` |
| **Ausführung** | Tester: Julian Schiller · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-303 |
| **Beschreibung** | Unbekannte Übung: `addSet` mit nicht existierender Übung |
| **Testvoraussetzung** | Mitglied und Workout angelegt |
| **Testschritte** | `addSet(workoutId, 999999, 8, 60)` aufrufen |
| **Erwartetes Ergebnis** | `NoSuchElementException` mit Meldung, die „Übung" enthält |
| **Automatisierter Test** | `ErrorHandlingIntegrationTest.addSetWithUnknownExercise` |
| **Ausführung** | Tester: Julian Schiller · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-304 |
| **Beschreibung** | Geschäftsregel: zweites Workout am selben Tag für dasselbe Mitglied |
| **Testvoraussetzung** | Mitglied angelegt, ein Workout für heute bereits gespeichert |
| **Testschritte** | `startWorkout(memberId, heute)` ein zweites Mal aufrufen |
| **Erwartetes Ergebnis** | `IllegalStateException` mit Meldung, die „bereits" enthält (geprüft über `existsByMemberIdAndDate` gegen die DB) |
| **Automatisierter Test** | `ErrorHandlingIntegrationTest.duplicateWorkoutPerDayIsRejected` |
| **Ausführung** | Tester: Julian Schiller · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

| Feld | Inhalt |
|---|---|
| **ID** | T-305 |
| **Beschreibung** | Unbekanntes Workout löschen: `deleteWorkout` mit nicht existierender ID |
| **Testvoraussetzung** | DB leer |
| **Testschritte** | `deleteWorkout(999999)` aufrufen |
| **Erwartetes Ergebnis** | `NoSuchElementException` mit Meldung, die „Workout" enthält |
| **Automatisierter Test** | `ErrorHandlingIntegrationTest.deleteUnknownWorkout` |
| **Ausführung** | Tester: Julian Schiller · Datum: 17.11.2026 · **Mängelklasse: 0** · Ergebnis: bestanden |

**Testprotokoll (Konsolenausgabe `mvn test`):**

```text
[INFO] Running ch.m450.gymtracker.integration.ErrorHandlingIntegrationTest
  [OK] T-301 unbekanntes Mitglied -> NoSuchElementException
  [OK] T-302 unbekanntes Workout  -> NoSuchElementException
  [OK] T-303 unbekannte Uebung    -> NoSuchElementException
  [OK] T-304 doppeltes Workout    -> IllegalStateException
  [OK] T-305 Workout loeschen (unbekannt) -> NoSuchElementException
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.22 s
[INFO] BUILD SUCCESS
```

---

## Anhang – Reproduktion

```bash
# Voraussetzung: Docker läuft
mvn test                 # führt alle Unit- und Integrationstests aus
mvn test -Dtest=CrudIntegrationTest,DataIntegrityIntegrationTest,ErrorHandlingIntegrationTest,TransactionRollbackIntegrationTest
```

Die Integrationstests starten automatisch einen PostgreSQL-Container; es ist keine
lokal installierte Datenbank nötig. In der CI-Pipeline (`.github/workflows/tests.yml`)
laufen die Tests bei jedem Push automatisch mit.

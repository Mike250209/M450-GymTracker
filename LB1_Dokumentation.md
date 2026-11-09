# M450 LB1 – Unit-Testing (GymTracker)

Dokumentation zum Fachgespräch. Projekt: Spring-Boot-GymTracker mit PostgreSQL-Anbindung
(JPA-Repositories). Getestet werden die beiden Service-Klassen mit Geschäftslogik.

## Überblick Ergebnis

| Kennzahl | Wert |
|---|---|
| Unit-Tests gesamt | 36, alle grün |
| Mutanten generiert | 63 |
| Mutanten getötet | 63 (100 % Kill-Rate) |
| Line Coverage (mutierte Klassen) | 100 % |
| Mutation-Threshold im Build | 100 % (Build bricht sonst ab) |

Befehle:
```
mvn test                                                   # alle Unit-Tests
mvn test-compile org.pitest:pitest-maven:mutationCoverage  # Mutation-Report
```
PIT-Report danach unter `target/pit-reports/index.html`.

## Baustein 1 – Unit-Test mit Mocking

Datei: `src/test/java/.../WorkoutServiceTest.java`

Getestete Unit: `WorkoutService`. Die Klasse hat vier Abhängigkeiten
(`WorkoutRepository`, `MemberRepository`, `ExerciseRepository`, `TrainingCalculator`).
Alle vier werden mit **Mockito** gemockt (`@Mock` + `@InjectMocks`), die echte
Datenbank wird nie kontaktiert.

Vollständig getestete Methoden und Zweige:
- `startWorkout`: gültig, Datum heute (Grenzfall), null-Datum, Zukunftsdatum,
  Mitglied unbekannt, Workout am Datum existiert bereits.
- `addSet`: gültig (Satz wird hinzugefügt und gespeichert), Validierung schlägt fehl
  (DB wird nicht angefasst), Workout unbekannt, Übung unbekannt.
- `totalVolume`: Summe über mehrere Workouts, leere Workout-Liste → 0.
- `averageVolumePerWorkout`: siehe Baustein 4.
- `deleteWorkout`: vorhanden (löscht), fehlt (wirft, löscht nicht).

Neben Rückgabewerten wird mit `verify(...)` geprüft, dass bei Fehlerfällen **keine**
DB-Aufrufe erfolgen (z. B. kein `save`, wenn die Validierung fehlschlägt).

## Baustein 2 – Unit-Test für Logik

Datei: `src/test/java/.../TrainingCalculatorTest.java`

Getestete Unit: `TrainingCalculator` (reine Berechnung, keine Abhängigkeiten).
Getestet werden alle Methoden inklusive Grenzwerte:
- `validateSet`: gültige Grenzen (1 und 100 Wdh.; 0 und 500 kg) sowie je ein
  ungültiger Wert knapp ausserhalb (0 Wdh., 101 Wdh., -0.01 kg, 500.01 kg).
- `oneRepMax`: Sonderfall 1 Wiederholung (= Gewicht), Epley-Formel mit Rundung
  (`5/100` → 116.67), glatter Wert (110.0), Validierung der Eingabe.
- `volume`: null → 0, leere Liste → 0, Summe `reps × weight` über mehrere Sätze.
- `strengthLevel`: Körpergewicht 0 und negativ (Fehler), sowie die vier Einstufungen
  an den exakten Grenzen (Verhältnis 0.5, 1.0, 1.49, 1.5).

## Baustein 3 – Automatisierte Code-Mutation (PIT)

Tool: **PITest** (`pitest-maven` 1.20.3 mit JUnit-5-Plugin), konfiguriert in `pom.xml`
auf die beiden Units `WorkoutService` und `TrainingCalculator`, Mutator-Gruppe
`STRONGER`, `mutationThreshold = 100`.

Ergebnis: 63 Mutanten, alle getötet (100 %). Der Report zeigt jede Mutation pro
Codezeile mit Status KILLED.

**Welche Mutatoren PIT genutzt hat** (aus dem Report ableitbar):
- *Conditionals Boundary* – `changed conditional boundary` (z. B. `<` ↔ `<=`).
  Deshalb testen wir die Grenzen exakt (reps = 1 und 100, Verhältnis = 1.0 und 1.5).
- *Negate Conditionals / Remove Conditionals* – `removed conditional – replaced
  equality/comparison check with true/false`. Deshalb braucht jeder `if`-Zweig
  einen True- und einen False-Testfall.
- *Math* – `Replaced double addition with subtraction`, `division with multiplication`,
  `multiplication with division`. Deshalb prüfen wir konkrete Rechenergebnisse
  (Volumen 600, 1RM 116.67, Durchschnitt 33.33 / 225.0).
- *Primitive Returns* – `replaced double return with 0.0d`. Deshalb assertieren wir
  immer den genauen Rückgabewert, nie nur „nicht null".

Zwei zunächst überlebende Mutanten (äquivalente Mutanten) wurden sauber eliminiert:
- `TrainingCalculator.volume`: redundante `isEmpty()`-Prüfung entfernt (die Schleife
  verarbeitet leere Listen bereits korrekt).
- `WorkoutService.averageVolumePerWorkout`: Rundung von `Math.round` auf `BigDecimal`
  umgestellt. Dadurch wird der Leer-Guard beweisbar nötig (sonst Division durch null),
  der entsprechende Mutant ist jetzt tötbar.

## Baustein 4 – TDD (statistischer Wert aus DB-Daten)

Neue Funktion: `WorkoutService.averageVolumePerWorkout(memberId)` – durchschnittliches
Trainingsvolumen pro Workout eines Mitglieds, berechnet aus den Workout-Daten der
Datenbank (per Repository).

Red → Green → Refactor (tatsächlich durchlaufen):
1. **Red**: Zuerst drei Tests für die noch nicht existierende Methode geschrieben.
   `mvn test` → `method averageVolumePerWorkout(long) is undefined` (3 Fehler).
2. **Green**: Minimal so viel Code implementiert, dass die Tests grün sind
   (DB-Volumen summieren, durch Anzahl teilen, runden).
3. **Refactor**: Rundung auf `BigDecimal` umgestellt (sauberer + 100 % Mutation-Kill).

Exakt verifizierte Fälle: drei Workouts mit Volumen 40/30/30 → Durchschnitt 33.33
(prüft Rundung), zwei Workouts 300/150 → 225.0, keine Workouts → 0.

## Automatisierungsgrad (3 Punkte)

`.github/workflows/tests.yml` führt bei **jedem Git-Push** automatisch aus:
`mvn test` und anschliessend die PIT-Mutation-Coverage. Der PIT-Report wird als
Artefakt hochgeladen. Das entspricht Stufe „Git-Push / CLI-push Pipeline" (3 Punkte).

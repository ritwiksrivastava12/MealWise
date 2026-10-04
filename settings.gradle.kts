// MealWise builds independently: backend/ (Spring Boot) and android/ (AGP) each have their own
// Gradle build with wrapper checked in by the maintainer via `gradle wrapper`.
// There is intentionally no root composite build (AGP composite includes are fragile in CI).

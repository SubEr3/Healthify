# Workout & Meal Tracker

A **JavaFX** desktop application to log your workouts, meals, and weight progress. Uses **SQLite** for local storage, offers CRUD operations, date-range filtering, daily calorie summaries, weight charting, and target-weight goals.

---

## Features

* **Workout Tracker**

  * Log exercises (date, name, sets, reps)
  * Edit, delete, and refresh entries
  * Date-range filtering

* **Meal Tracker**

  * Log meals (date, type, food item, calories)
  * Daily calories summary with green/red highlighting
  * Edit, delete, and refresh entries
  * Date-range filtering

* **Weight Logger**

  * Log daily weight
  * Line chart visualization of weight progress
  * Date-range filtering
  * Set and display a target weight goal

* **Persistent Settings**

  * Store and retrieve user preferences (e.g., target weight)

---

## Technology Stack

* **Language:** Java 24
* **UI Framework:** JavaFX 24
* **Database:** SQLite via `sqlite-jdbc` 3.49.1
* **Build & IDE:** No build tool required; configured for VS Code Java extensions

---

## Project Structure

```
Workout Program/
├─ lib/                      # JDBC & JavaFX jars
├─ src/
│  ├─ Main.java             # Application launcher
│  ├─ database/
│  │   └─ DBManager.java    # SQLite connection & schema
│  ├─ models/
│  │   ├─ Workout.java
│  │   ├─ Meal.java
│  │   ├─ WeightEntry.java
│  │   └─ DailyCalories.java
│  ├─ controllers/
│  │   ├─ WorkoutController.java
│  │   ├─ MealController.java
│  │   ├─ WeightController.java
│  │   └─ MainController.java
│  └─ views/
│      ├─ MainView.fxml
│      ├─ WorkoutView.fxml
│      ├─ MealView.fxml
│      └─ WeightView.fxml
├─ .vscode/
│  ├─ settings.json         # Java runtime & project settings
│  └─ launch.json           # VS Code launch configurations
└─ README.md
```

---

## Prerequisites

1. **Java 24 JDK** installed
2. **JavaFX 24 SDK**
3. **SQLite JDBC** (`sqlite-jdbc-3.49.1.0.jar`)
4. **VS Code** with Java Extension Pack (if using VS Code)

---

## Setup & Run

1. **Clone the repository**

   ```bash
   git clone <repo-url>
   cd Workout\ Program
   ```

2. **Configure VS Code**

   * Ensure `.vscode/settings.json` points to your JDK-24 path and sets `sourceCompatibility` to "15" or higher.
   * Ensure `.vscode/launch.json` includes:

     ```json
     {
       "mainClass": "src.Main",
       "vmArgs": "--module-path \"C:/path/to/javafx-sdk-24/lib\" --add-modules javafx.controls,javafx.fxml",
       "classPaths": ["lib/sqlite-jdbc-3.49.1.0.jar"]
     }
     ```

3. **Run**

   * In VS Code: Press **F5** on `Main.java`.
   * Or from command line:

     ```bash
     javac -cp "lib/*;src" src/Main.java
     java -cp "lib/*;src" --module-path "C:/path/to/javafx-sdk-24/lib" --add-modules javafx.controls,javafx.fxml src.Main
     ```

---

## Usage

* **Workouts Tab:** Log and track exercise details.
* **Meals Tab:** Log meals, view daily calorie totals, and set calorie goals.
* **Weight Tab:** Log weight, view progress chart, and set a target weight.

Feel free to explore the filtering, editing, and goal‐setting features in each tab!

---

## Future Improvements

* Export/import to CSV/JSON
* Notifications/reminders for logging
* UI theming & mobile adaptation

---


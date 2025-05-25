package src.models;

public class Meal {
    private final int id;
    private final String date;
    private final String type;
    private final String food;
    private final int calories;

    public Meal(int id, String date, String type, String food, int calories) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.food = food;
        this.calories = calories;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getType() {
        return type;
    }

    public String getFood() {
        return food;
    }

    public int getCalories() {
        return calories;
    }
}
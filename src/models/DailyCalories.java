package src.models;

public class DailyCalories {
    private final String date;
    private final int total;

    public DailyCalories(String date, int total) {
        this.date = date;
        this.total = total;
    }

    public String getDate() {
        return date;
    }

    public int getTotal() {
        return total;
    }
}

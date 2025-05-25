package src.models;

public class Workout {
    private final int id;
    private final String date;
    private final String exercise;
    private final int sets;
    private final int reps;

    public Workout(int id, String date, String exercise, int sets, int reps) {
        this.id       = id;
        this.date     = date;
        this.exercise = exercise;
        this.sets     = sets;
        this.reps     = reps;
    }

    public int    getId()       { return id; }
    public String getDate()     { return date; }
    public String getExercise() { return exercise; }
    public int    getSets()     { return sets; }
    public int    getReps()     { return reps; }
}

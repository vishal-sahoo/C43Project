package project;

public class Amenity {
    private String description;
    private String category;

    public Amenity(String description, String category) {
        this.description = description;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }
}

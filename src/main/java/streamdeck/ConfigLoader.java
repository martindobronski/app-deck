package streamdeck;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static List<List<ButtonConfig>> load(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {
            Type multiType = new TypeToken<List<List<ButtonConfig>>>() {}.getType();
            List<List<ButtonConfig>> pages = gson.fromJson(reader, multiType);
            if (pages != null && !pages.isEmpty()) return pages;
        } catch (Exception ignored) {}

        try (FileReader reader = new FileReader(path)) {
            Type flatType = new TypeToken<List<ButtonConfig>>() {}.getType();
            List<ButtonConfig> flat = gson.fromJson(reader, flatType);
            if (flat != null && !flat.isEmpty()) {
                List<List<ButtonConfig>> pages = new ArrayList<>();
                for (int i = 0; i < flat.size(); i += 13) {
                    pages.add(new ArrayList<>(flat.subList(i, Math.min(i + 13, flat.size()))));
                }
                return pages;
            }
        }

        List<List<ButtonConfig>> empty = new ArrayList<>();
        empty.add(new ArrayList<>());
        return empty;
    }

    public static void save(String path, List<List<ButtonConfig>> pages) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            gson.toJson(pages, writer);
        }
    }
}

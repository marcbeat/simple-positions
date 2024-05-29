package com.gmail.marc.login;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Collections;

public class JsonWriter {
    private String jsonFileName = SimplePositions.MODID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type POSITION_LIST_TYPE = new TypeToken<List<PositionData>>() {}.getType();

    public JsonWriter(String worldName) {
        if (worldName != null && !worldName.equals(""))
            jsonFileName = SimplePositions.MODID + "." + worldName.replace(" ", "-") + ".json";
    }

    public List<PositionData> readPositionsFromJson() {
        File jsonFile = getJsonFile();
        String filePath = jsonFile.getAbsolutePath();
        // SimplePositions.LOGGER.debug("Loading data file from path: " + filePath);

        if (!jsonFile.exists()) {
            // Json file doesn't exist yes -> create it
            List<PositionData> emptyList = Collections.<PositionData> emptyList();
            writePositionsToJson(emptyList);
            SimplePositions.LOGGER.debug("Position data file doesn't exist: Creating new.");
            return emptyList;
        }
        try (FileReader reader = new FileReader(filePath)) {
            List<PositionData> posList = GSON.fromJson(reader, POSITION_LIST_TYPE);
            SimplePositions.LOGGER.debug("Loaded positions from JSON.");
            return posList;
        } catch (JsonIOException | JsonSyntaxException | IOException e) {
            e.printStackTrace();
            SimplePositions.LOGGER.error("Error loading positions from JSON: {}", e.getMessage());
            return null;
        }
    }

    public void writePositionsToJson(List<PositionData> positions) {
        String filePath = getJsonFile().getAbsolutePath();
        // SimplePositions.LOGGER.debug("Saving data file to path: " + filePath);
        try (FileWriter writer = new FileWriter(filePath)) {
            GSON.toJson(positions, writer);
            SimplePositions.LOGGER.debug("Saved positions to JSON.");
        } catch (JsonIOException | IOException e) {
            e.printStackTrace();
            SimplePositions.LOGGER.error("Error saving positions to JSON: {}", e.getMessage());
        }
    }

    public File getJsonFile() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        // Path modConfigDir = configDir.resolve(SimplePositions.MODID);
        // try {
        //     // Create the subdirectory if it doesn't exist
        //     if (Files.notExists(modConfigDir)) {
        //         Files.createDirectories(modConfigDir);
        //         SimplePositions.LOGGER.debug("Created config directory '{}'", modConfigDir.toAbsolutePath());
        //     } else {
        //         SimplePositions.LOGGER.debug("Config directory '{}' already exists.", modConfigDir.toAbsolutePath());
        //     }
        // } catch (IOException e) {
        //     SimplePositions.LOGGER.error("Couldn't create or find config directory:\n{}", e.getMessage());
        // }
        File jsonFile = new File(configDir.toFile(), jsonFileName);
        return jsonFile;
    }
}
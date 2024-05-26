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

public class JsonUtil {
    private static final String JSON_FILE_NAME = "positions.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type POSITION_LIST_TYPE = new TypeToken<List<PositionData>>() {}.getType();

    public static List<PositionData> readPositionsFromJson() {
        File jsonFile = getJsonFile();
        String filePath = jsonFile.getAbsolutePath();
        if (!jsonFile.exists()) {
            // Json file doesn't exist yes -> create it
            List<PositionData> emptyList = Collections.<PositionData> emptyList();
            writePositionsToJson(emptyList);
            SimplePositions.LOGGER.debug("SimplePositions position data file doesn't exist: Creating new.");
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

    public static void writePositionsToJson(List<PositionData> positions) {
        String filePath = getJsonFile().getAbsolutePath();
        try (FileWriter writer = new FileWriter(filePath)) {
            GSON.toJson(positions, writer);
            SimplePositions.LOGGER.debug("Saved positions to JSON.");
        } catch (JsonIOException | IOException e) {
            e.printStackTrace();
            SimplePositions.LOGGER.error("Error saving positions to JSON: {}", e.getMessage());
        }
    }

    public static File getJsonFile() {
        Path jsonPath = FMLPaths.CONFIGDIR.get();
        File jsonFile = new File(jsonPath.toFile(), JSON_FILE_NAME);
        return jsonFile;
    }
}
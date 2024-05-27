package com.gmail.marc.login;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PositionData {
    private UUID id;
    private String name;
    private String dim; // [world, nether, end]
    private String list;
    private int x;
    private int y;
    private int z;

    public PositionData(String name, String dim, String list, int x, int y, int z) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.dim = dim;
        this.list = list;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Getters and setters
    // public UUID getId() {
    //     return id;
    // }

    public String getName() {
        return name;
    }

    // Get fully qualified name -> <dim>.<list>.<name>
    public String getFQN() {
        return genFQN(dim, list, name);
    }

    public static String genFQN(String dim, String list, String name) {
        return dim + "." + list + "." + name;
    }

    public String getDim() {
        return dim;
    }

    public void setDim(String dim) {
        this.dim = dim;
    }

    public String getList() {
        return list;
    }

    public int[] getPos() {
        int[] array = {x,y,z};
        return array;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;

    }
    public int getZ() {
        return z;

    }

    public void setPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Util funtions
    public static String getNameFromFqn(String fqn) {
        String[] parts = splitFqn(fqn);

        if (parts.length == 3)
            return parts[2];
        else
            return "";
    }
    public static String getListFromFqn(String fqn) {
        String[] parts = splitFqn(fqn);
        if (parts.length == 3)
            return parts[1];
        else
            return "";
    }
    public static String getDimFromFqn(String fqn) {
        String[] parts = splitFqn(fqn);
        if (parts.length == 3)
            return parts[0];
        else
            return "";
    }

    public static boolean checkFqn(String fqn) {
        Pattern pattern = Pattern.compile("^\\b(world|nether|end)\\b\\.[\\w\\d-_]+\\.[\\w\\d-_]+$"); // Needs to be 3 combinations of letters or digits or any of (+-_) divided by "."
        Matcher matcher = pattern.matcher(fqn);
        return matcher.find();
    }

    public static boolean checkAllowedChars(String str) {
        Pattern pattern = Pattern.compile("^[\\w\\d-_]+$");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    public static boolean checkDim(String dim) {
        Set<String> allowedDims = new HashSet<>(Arrays.asList("world", "nether", "end"));
        return allowedDims.contains(dim);
    }

    public static String[] splitFqn(String fqn) {
        return fqn.split("\\.");
    }
}

package com.gmail.marc.login;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class PositionDataList {
    private List<PositionData> positions;

    public PositionDataList() {
        loadList();
    }

    private void loadList() {
        positions = JsonUtil.readPositionsFromJson();
    }

    public void saveList() {
        CompletableFuture.runAsync(() -> {
            JsonUtil.writePositionsToJson(positions);
        });
    }

    public boolean add(PositionData position) {
        if (checkDuplicateEntry(position)) {
            positions.add(position);
            saveList(); // Write list to disk
            return true;
        }
        else
            return false;
    }

    public List<PositionData> get(String query) {
        String[] parts = query.split(".");
        switch (parts.length) {
            case 3: {
                String dim = parts[0];
                String list = parts[1];
                String name = parts[2];
                List<PositionData> filteredList = positions;
                if (dim != "*")
                    filteredList = filterByDim(dim, filteredList);
                if (list != "*")
                    filteredList = filterByList(list, filteredList);
                if (name != "*")
                    filteredList = filterByName(name, filteredList);
                return filteredList;
            }
            case 2: {
                String list = parts[0];
                String name = parts[1];
                List<PositionData> filteredList = positions;
                if (list != "*")
                    filteredList = filterByList(list, filteredList);
                if (name != "*")
                    filteredList = filterByName(name, filteredList);
                return filteredList;
            }
            default: {
                String name = parts[0];
                if (name != "*")
                    return filterByName(name);
                return positions;
            }
        }
    }

    public int remove(String fqn) {
        if (!PositionData.checkAllowedChars(fqn)) return 2; // If fqn is malformed, return 2 (error)
        
        String dim = PositionData.getDimFromFqn(fqn);
        String list = PositionData.getListFromFqn(fqn);
        String name = PositionData.getNameFromFqn(fqn);
        PositionData position = filterByDimListName(dim, list, name);
        if (position == null) return 0; // If no position with name found -> return 0
        // else remove and return 1 -> success
        positions.remove(position);
        saveList(); // Write list to disk
        return 1;
    }

    // public int remove(String name, String list, String dim) {
    //     PositionData pos = filterByDimListName(dim, list, name);
    //     if (pos == null) return 0; // If no position with name found -> return false
    //     positions.remove(pos); // else remove and return true
    //     return 1;
    // }

    public int update(String fqn, int x, int y, int z) {
        if (!PositionData.checkAllowedChars(fqn)) return 2; // If fqn is malformed, return 2 (error)

        String dim = PositionData.getDimFromFqn(fqn);
        String list = PositionData.getListFromFqn(fqn);
        String name = PositionData.getNameFromFqn(fqn);
        PositionData position = filterByDimListName(dim, list, name);
        if (position == null) return 0; // If no position with name found -> return 0
        // else update position with new coordinates and return 1 -> success
        position.setPos(x, y, z);
        saveList(); // Write list to disk
        return 1;
    }


    public List<String> getLists() {
        List<String> uniqueLists = positions.stream()
        .map(PositionData::getList)
        .distinct()
        .collect(Collectors.toList());
        return uniqueLists;
    }

    // Util functions
    private boolean checkDuplicateEntry(PositionData position) {
        return filterByDimListName(position.getDim(), position.getList(), position.getName()) == null;
    }

    
    private List<PositionData> filterByName(String name) {
        List<PositionData> filteredList = positions.stream()
        .filter(pos -> pos.getName().equals(name))
        .collect(Collectors.toList());

        return filteredList;
    }

    private List<PositionData> filterByName(String name, List<PositionData> customList) {
        List<PositionData> filteredList = customList.stream()
        .filter(pos -> pos.getName().equals(name))
        .collect(Collectors.toList());

        return filteredList;
    }

    private List<PositionData> filterByDim(String dim) {
        List<PositionData> filteredList = positions.stream()
        .filter(pos -> pos.getDim().equals(dim))
        .collect(Collectors.toList());

        return filteredList;
    }

    private List<PositionData> filterByDim(String dim, List<PositionData> customList) {
        List<PositionData> filteredList = customList.stream()
        .filter(pos -> pos.getDim().equals(dim))
        .collect(Collectors.toList());

        return filteredList;
    }

    private List<PositionData> filterByList(String list) {
        List<PositionData> filteredList = positions.stream()
        .filter(pos -> pos.getList().equals(list))
        .collect(Collectors.toList());

        return filteredList;
    }

    private List<PositionData> filterByList(String list, List<PositionData> customList) {
        List<PositionData> filteredList = customList.stream()
        .filter(pos -> pos.getList().equals(list))
        .collect(Collectors.toList());

        return filteredList;
    }

    private PositionData filterByDimListName(String dim, String list, String name) {
        List<PositionData> filteredList = positions.stream()
        .filter(pos -> pos.getName().equals(name) &&
                       pos.getList().equals(list) &&
                       pos.getDim().equals(dim))
        .collect(Collectors.toList());

        if (filteredList.size() == 1) return filteredList.get(0);
        else return null;
    }
}

package com.gmail.marc.login;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class PositionDataList {
    private List<PositionData> positions;
    public static final String QUERY_WILDCARD = "+";

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

    public int add(PositionData position) {
        if (checkDuplicateEntry(position)) {
            positions.add(position);
            saveList(); // Write list to disk
        }
        return positions.indexOf(position);
    }

    // public List<PositionData> get(String query) {
    //     String[] parts = PositionData.splitFqn(query);
    //     switch (parts.length) {
    //         case 3: {
    //             String dim = parts[0];
    //             String list = parts[1];
    //             String name = parts[2];
    //             List<PositionData> filteredList = positions;
    //             if (!dim.equals(QUERY_WILDCARD))
    //                 filteredList = filterByDim(dim, filteredList);
    //             if (!list.equals(QUERY_WILDCARD))
    //                 filteredList = filterByList(list, filteredList);
    //             if (!name.equals(QUERY_WILDCARD))
    //                 filteredList = filterByName(name, filteredList);
    //             return filteredList;
    //         }
    //         case 2: {
    //             String list = parts[0];
    //             String name = parts[1];
    //             List<PositionData> filteredList = positions;
    //             if (!list.equals(QUERY_WILDCARD))
    //                 filteredList = filterByList(list, filteredList);
    //             if (!name.equals(QUERY_WILDCARD))
    //                 filteredList = filterByName(name, filteredList);
    //             return filteredList;
    //         }
    //         default: {
    //             String name = parts[0];
    //             if (!name.equals(QUERY_WILDCARD))
    //                 return filterByName(name);
    //             return positions;
    //         }
    //     }
    // }

    public List<PositionData> get(String dim, String list, String name) {
        List<PositionData> filteredList = positions;
        if (!dim.equals(QUERY_WILDCARD))
            filteredList = filterByDim(dim, filteredList);
        if (!list.equals(QUERY_WILDCARD))
            filteredList = filterByList(list, filteredList);
        if (!name.equals(QUERY_WILDCARD))
            filteredList = filterByName(name, filteredList);

        return filteredList;
    }

    // public int remove(String fqn) {
    //     if (!PositionData.checkAllowedChars(fqn)) return 2; // If fqn is malformed, return 2 (error)
        
    //     String dim = PositionData.getDimFromFqn(fqn);
    //     String list = PositionData.getListFromFqn(fqn);
    //     String name = PositionData.getNameFromFqn(fqn);
    //     PositionData position = filterByDimListName(dim, list, name);
    //     if (position == null) return 0; // If no position with name found -> return 0
    //     // else remove and return 1 -> success
    //     positions.remove(position);
    //     saveList(); // Write list to disk
    //     return 1;
    // }

    public PositionData remove(String dim, String list, String name) {
        PositionData pos = filterByDimListName(dim, list, name);
        if (pos == null) return null; // If no position with name found -> return false
        positions.remove(pos); // else remove and return true
        saveList(); // Write list to disk
        return pos;
    }

    public PositionData update(String dim, String list, String name, int x, int y, int z) {
        PositionData position = filterByDimListName(dim, list, name);
        if (position == null) return null; // If no position with name found -> return null
        // else update position with new coordinates and return new PositionData -> success
        position.setPos(x, y, z);
        saveList(); // Write list to disk
        return position;
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

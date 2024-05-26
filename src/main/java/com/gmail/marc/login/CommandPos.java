package com.gmail.marc.login;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class CommandPos {

    private PositionDataList pdList;

    // Bootstrap colors (boosted lightness)
    // https://getbootstrap.com/docs/5.0/customize/color/
    private static final TextColor COLOR_GREEN = TextColor.fromRgb(0x6BCF97);
    private static final TextColor COLOR_BLUE = TextColor.fromRgb(0x73A1FF);
    private static final TextColor COLOR_RED = TextColor.fromRgb(0xF64F59);
    private static final TextColor COLOR_YELLOW = TextColor.fromRgb(0xFFD025);
    private static final TextColor COLOR_WHITE = TextColor.fromRgb(0xFFFFFF);
    private static final TextColor COLOR_INDIGO = TextColor.fromRgb(0xA48CFF);
    private static final TextColor COLOR_TEAL = TextColor.fromRgb(0x20C997);
    private static final TextColor COLOR_PINK = TextColor.fromRgb(0xD63384);
    private static final TextColor COLOR_CYAN = TextColor.fromRgb(0x0DCAF0);
    

    public CommandPos(PositionDataList pdList) {
        this.pdList = pdList;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pos")
            .then(Commands.argument("action", StringArgumentType.word())
                .suggests((context, builder) -> builder.suggest("get")
                                                .suggest("set")
                                                .suggest("settarget")
                                                .suggest("rem")
                                                .suggest("update")
                                                .suggest("lists")
                                                .suggest("dims")
                                                .suggest("help")
                                                .buildFuture())
                .executes(context -> {
                    String action = StringArgumentType.getString(context, "action");
                    return executeBaseCommand(context.getSource(), action, null);
                })
                .then(Commands.argument("query", StringArgumentType.word())
                    .executes(context -> {
                        String action = StringArgumentType.getString(context, "action");
                        String query = StringArgumentType.getString(context, "query");
                        return executeBaseCommand(context.getSource(), action, query);
                    })
                )
            )
        );
    }

    private int executeBaseCommand(CommandSourceStack source, String action, String query) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return 0;
        }

        if (action.equals("dims")) {
            // Send dims to player in chat
            player.sendSystemMessage(
                Component.literal("Possible dimensions:\n")
                .append(" ⊳ ")
                .append(getColoredString("world\n", COLOR_INDIGO))
                .append(" ⊳ ")
                .append(getColoredString("nether\n", COLOR_INDIGO))
                .append(" ⊳ ")
                .append(getColoredString("end\n", COLOR_INDIGO))); // Possible dimensions: 'world', 'nether', 'end'.
            return 1;
        }
        else if (action.equals("lists")) {
            // Send existing lists to player in chat
            List<String> allLists = pdList.getLists();
            // String listNames = allLists.stream().collect(Collectors.joining("\n- "));
            // player.sendSystemMessage(Component.literal("Saved lists:\n- " + listNames));
            player.sendSystemMessage(genListsMsg(allLists));
            return 1;
        }
        else if (action.equals("get")) {
            // Send saved positions to player in chat according to search query
            String name;
            String list;
            String dim;
            // Store current positions of player
            if (PositionData.checkFqn(query)) {
                name = PositionData.getNameFromFqn(query);
                list = PositionData.getListFromFqn(query);
                dim = PositionData.getDimFromFqn(query);
                // SimplePositions.LOGGER.debug("fqn: {}, name: {}, list: {}, dim: {}", query, name, list, dim);
            }
            else {
                String[] parts = PositionData.splitFqn(query);
                if (parts.length == 3) {
                    // Query e.g. = foo.bar.baz
                    if (PositionData.checkDim(parts[0]) || parts[0].equals(PositionDataList.QUERY_WILDCARD)) {
                        dim = parts[0];
                    }
                    else {
                        player.sendSystemMessage(genDimInvalidMsg());
                        return 0;
                    }
                    if (PositionData.checkAllowedChars(parts[1]) || parts[1].equals(PositionDataList.QUERY_WILDCARD)) {
                        list = parts[1];
                    }
                    else {
                        player.sendSystemMessage(genListInvalidMsg());
                        return 0;
                    }
                    if (PositionData.checkAllowedChars(parts[2]) || parts[2].equals(PositionDataList.QUERY_WILDCARD)) {
                        name = parts[2];
                    }
                    else {
                        player.sendSystemMessage(genNameInvalidMsg());
                        return 0;
                    }
                }
                else if (parts.length == 2) {
                    // Query e.g. = bar.baz
                    if (PositionData.checkAllowedChars(parts[0]) || parts[0].equals(PositionDataList.QUERY_WILDCARD)) {
                        list = parts[0];
                    }
                    else {
                        player.sendSystemMessage(genListInvalidMsg());
                        return 0;
                    }
                    if (PositionData.checkAllowedChars(parts[1]) || parts[1].equals(PositionDataList.QUERY_WILDCARD)) {
                        name = parts[1];
                    }
                    else {
                        player.sendSystemMessage(genNameInvalidMsg());
                        return 0;
                    }
                    dim = getPlayerDim(player);
                }
                else if (parts.length == 1) {
                    // Query e.g. = baz
                    if (PositionData.checkAllowedChars(parts[0]) || parts[0].equals(PositionDataList.QUERY_WILDCARD)) {
                        name = parts[0];
                    }
                    else {
                        player.sendSystemMessage(genNameInvalidMsg());
                        return 0;
                    }
                    dim = getPlayerDim(player);
                    list = "default";
                }
                else {
                    player.sendSystemMessage(genNameInvalidMsg());
                    return 0;
                }
            }

            List<PositionData> savedPositions = pdList.get(dim, list, name);
            if (savedPositions.size() < 1) {
                player.sendSystemMessage(
                    Component.literal("No position found matching the query '")
                    .append(getColoredString(query, COLOR_YELLOW))
                    .append("'.")); // No position found matching the query '%s'.
            }
            else if (savedPositions.size() == 1) {
                PositionData pos = savedPositions.get(0);
                MutableComponent msg = Component.literal("X: ")
                    .append(getColoredString(Integer.toString(pos.getX()), COLOR_BLUE))
                    .append(", Y: ")
                    .append(getColoredString(Integer.toString(pos.getY()), COLOR_BLUE))
                    .append(", Z: ")
                    .append(getColoredString(Integer.toString(pos.getZ()), COLOR_BLUE))
                    .append(", Dim: ")
                    .append(getColoredString(pos.getDim(), COLOR_INDIGO));
                if (dim.equals(getPlayerDim(player))) {// Add distance if in same dim
                    msg.append(", Distance: ")
                    .append(
                        getColoredString(
                            Integer.toString(
                                getManhattanDistance(player.blockPosition(), new BlockPos(pos.getX(), pos.getY(), pos.getZ()))
                            ) + " blocks", COLOR_CYAN));
                }
                player.sendSystemMessage(msg);
            }
            else {
                // String positionNames = savedPositions.stream().map(PositionData::getFQN).collect(Collectors.joining(", "));
                MutableComponent msg = Component.literal("Positions found (")
                    .append(getColoredString(Integer.toString(savedPositions.size()), COLOR_BLUE))
                    .append(getColoredString("):\n", COLOR_WHITE))
                    .append(genPositionMsg(savedPositions));
                player.sendSystemMessage(msg);
            }
            return 1;
        }
        else if (action.equals("set") || action.equals("settarget")) {
            String name;
            String list;
            String dim;
            // Store current positions of player
            if (PositionData.checkFqn(query)) {
                name = PositionData.getNameFromFqn(query);
                list = PositionData.getListFromFqn(query);
                dim = PositionData.getDimFromFqn(query);
                // SimplePositions.LOGGER.debug("fqn: {}, name: {}, list: {}, dim: {}", query, name, list, dim);
            }
            else {
                String[] parts = PositionData.splitFqn(query);
                if (parts.length == 3) {
                    // Query e.g. = foo.bar.baz
                    if (PositionData.checkDim(parts[0])) {
                        dim = parts[0];
                    }
                    else {
                        player.sendSystemMessage(genDimInvalidMsg());
                        return 0;
                    }
                    if (PositionData.checkAllowedChars(parts[1])) {
                        list = parts[1];
                    }
                    else {
                        player.sendSystemMessage(genListInvalidMsg());
                        return 0;
                    }
                    if (PositionData.checkAllowedChars(parts[2])) {
                        name = parts[2];
                    }
                    else {
                        player.sendSystemMessage(genNameInvalidMsg());
                        return 0;
                    }
                }
                else if (parts.length == 2) {
                    // Query e.g. = bar.baz
                    if (PositionData.checkAllowedChars(parts[0])) {
                        list = parts[0];
                    }
                    else {
                        player.sendSystemMessage(genListInvalidMsg());
                        return 0;
                    }
                    if (PositionData.checkAllowedChars(parts[1])) {
                        name = parts[1];
                    }
                    else {
                        player.sendSystemMessage(genNameInvalidMsg());
                        return 0;
                    }
                    dim = getPlayerDim(player);
                }
                else if (parts.length == 1) {
                    // Query e.g. = baz
                    if (PositionData.checkAllowedChars(parts[0])) {
                        name = parts[0];
                    }
                    else {
                        player.sendSystemMessage(genNameInvalidMsg());
                        return 0;
                    }
                    dim = getPlayerDim(player);
                    list = "default";
                }
                else {
                    player.sendSystemMessage(genNameInvalidMsg());
                    return 0;
                }
            }
            
            BlockPos blockPos;
            if (action.equals("settarget")) {
                // Action is "settarget" -> get position of the block, the player looks at
                blockPos = getTargetedBlock(player);
                if (blockPos == null) {
                    player.sendSystemMessage(
                        Component.literal("Couldn't determine which block you are looking at, make sure it is ")
                        .append(getColoredString("max. 20 blocks", COLOR_RED))
                        .append(" away.")); // Couldn't determine which block you are targeting, make sure it is max. 20 blocks away.
                    return 0;
                } 
            }
            else {
                // Action is "set" -> get position of the block, the player stands in
                blockPos = player.blockPosition();
            }
            PositionData pos = new PositionData(name, dim, list, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (pdList.add(pos)) {
                player.sendSystemMessage(
                    Component.literal("Position (X: ")
                    .append(getColoredString(Integer.toString(pos.getX()), COLOR_BLUE))
                    .append(", Y: ")
                    .append(getColoredString(Integer.toString(pos.getY()), COLOR_BLUE))
                    .append(", Z: ")
                    .append(getColoredString(Integer.toString(pos.getZ()), COLOR_BLUE))
                    .append(") saved as: ")
                    .append(getColoredString(pos.getFQN(), COLOR_GREEN))); // Position (X: %s, Y: %s, Z: %s) saved as: %s
            }
            else {
                player.sendSystemMessage(
                    Component.literal("Position with name ")
                    .append(getColoredString(pos.getFQN(), COLOR_YELLOW))
                    .append(" already saved, please choose different name.")); // Position with name %s already saved, please choose different name.
            }
            return 1;
        }
        else if (action.equals("rem")) {
            // TODO
            return 1;
        }
        else if (action.equals("update")) {
            // TODO
            return 1;
        }
        else if (action.equals("help")) {
            // TODO
            return 1;
        }
        else {
            player.sendSystemMessage(
                Component.literal("Command ")
                .append(getColoredString(action, COLOR_YELLOW))
                .append(" unknown, see ")
                .append(getColoredString("/pos help", COLOR_BLUE))
                .append(" for info.")); // Command %s unknown, see /pos help for info.
            return 0;
        }
    }

    private static int getManhattanDistance(BlockPos pos1, BlockPos pos2) {
        return pos1.distManhattan(pos2);
    }

    private static MutableComponent genPositionMsg(List<PositionData> positionDataList) {
        // Step 1: Group by 'dim' then by 'list'
        Map<String, Map<String, List<String>>> groupedData = positionDataList.stream()
                .collect(Collectors.groupingBy(
                        PositionData::getDim,
                        Collectors.groupingBy(
                                PositionData::getList,
                                Collectors.mapping(PositionData::getName, Collectors.toList())
                        )
                ));

        // Step 2: Sort and build the message
        MutableComponent msg = Component.literal("");

        groupedData.keySet().stream().sorted().forEach(dim -> {
            msg.append(getColoredString(dim + ":\n", COLOR_INDIGO));
            groupedData.get(dim).keySet().stream().sorted().forEach(list -> {
                msg.append(getColoredString("|  ", COLOR_INDIGO))
                   .append(getColoredString(list + ":\n", COLOR_TEAL));
                groupedData.get(dim).get(list).stream().sorted().forEach(name -> {
                    msg.append(getColoredString("|  ", COLOR_INDIGO))
                       .append(getColoredString("|  ", COLOR_TEAL))
                       .append(getColoredString("⊳ " + name + "\n", COLOR_PINK));
                });
            });
        });

        return msg;
    }

    private static MutableComponent genListsMsg(List<String> stringList) {
        MutableComponent msg = Component.literal("Saved lists (")
            .append(getColoredString(Integer.toString(stringList.size()), COLOR_BLUE))
            .append("):\n");

        stringList.stream().sorted().forEach(list -> {
            msg.append(" ⊳ ")
               .append(getColoredString(list + "\n", COLOR_TEAL));
        });

        return msg;
    }

    private static MutableComponent genNameInvalidMsg() {
        return genArgInvalidMsg("Name");
    }

    private static MutableComponent genListInvalidMsg() {
        return genArgInvalidMsg("List");
    }

    private static MutableComponent genArgInvalidMsg(String arg) {
        return Component.literal("")
            .append(getColoredString(arg, COLOR_RED))
            .append(" invalid, please use only letters ")
            .append(getColoredString("a-z", COLOR_BLUE))
            .append(", digits ")
            .append(getColoredString("0-9", COLOR_BLUE))
            .append(", symbols ")
            .append(getColoredString("- _", COLOR_BLUE))
            .append("."); // <Name|List> invalid, please use only a-z,0-9,-_.
    }

    private static MutableComponent genDimInvalidMsg() {
        return Component.literal("")
        .append(getColoredString("Dim", COLOR_RED))
        .append(" invalid, please use only ")
        .append(getColoredString("world", COLOR_INDIGO))
        .append(", ")
        .append(getColoredString("nether", COLOR_INDIGO))
        .append(" or ")
        .append(getColoredString("end", COLOR_INDIGO))
        .append("."); // Dim invalid, please use only world, nether or end
    }
    

    private static MutableComponent getColoredString(String str, TextColor color) {
        return Component.literal(str).withStyle(Style.EMPTY.withColor(color));
    }

    private static String getPlayerDim(ServerPlayer player) {
        // Check which dimension the player is in
        ResourceKey<Level> dimensionKey = player.level().dimension();
        if (dimensionKey == Level.OVERWORLD) {
            return "world";
        } else if (dimensionKey == Level.NETHER) {
            return "nether";
        } else if (dimensionKey == Level.END) {
            return "end";
        } else {
            return "world"; // Unknown dimension
        }
    }

    private static BlockPos getTargetedBlock(ServerPlayer player) {
        // Define the distance for ray tracing
        double maxDistance = 20.0D;

        // Get the player's eye position and look direction
        Vec3 eyePosition = player.getEyePosition(1.0F);
        Vec3 lookDirection = player.getViewVector(1.0F).scale(maxDistance);
        Vec3 traceEnd = eyePosition.add(lookDirection);

        // Perform the ray trace
        BlockHitResult blockHitResult = player.level().clip(new ClipContext(
            eyePosition,
            traceEnd,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.SOURCE_ONLY,
            player
        ));

        // Check if the ray trace hit a block
        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            // Return the position of the targeted block
            return blockHitResult.getBlockPos();
        }

        // Return null if no block was targeted
        return null;
    }
}

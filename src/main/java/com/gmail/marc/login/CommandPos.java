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
                                                .suggest("rem")
                                                .suggest("update")
                                                .suggest("lists")
                                                .suggest("help")
                                                .buildFuture())
                .executes(context -> {
                    String action = StringArgumentType.getString(context, "action");
                    return executeBaseCommand(context.getSource(), action, null, null);
                })
                .then(Commands.argument("query", StringArgumentType.string())
                    .executes(context -> {
                        String action = StringArgumentType.getString(context, "action");
                        String query = StringArgumentType.getString(context, "query");
                        return executeBaseCommand(context.getSource(), action, query, null);
                    })
                    .then(Commands.argument("target", StringArgumentType.word())
                        .executes(context -> {
                            String action = StringArgumentType.getString(context, "action");
                            String query = StringArgumentType.getString(context, "query");
                            String target = StringArgumentType.getString(context, "target");
                            return executeBaseCommand(context.getSource(), action, query, target);
                        })
                    )
                )
            )
        );
    }

    private int executeBaseCommand(CommandSourceStack source, String action, String query, String target) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return 0;
        }

        // Normalize arguments
        if (action != null)
            action = action.toLowerCase();
        if (query != null)
            query = query.toLowerCase();
        if (target != null)
            target = target.toLowerCase();
        else target = "player"; // Set target to player (instead of "target")

        if (action.equals("lists")) {
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
                    Component.literal("No position found matching ")
                    .append(getColoredString(String.format("%s.%s.%s", dim, list, name), COLOR_YELLOW))
                    .append(".")); // No position found matching the query '%s'.
            }
            else if (savedPositions.size() == 1) {
                PositionData pos = savedPositions.get(0);
                MutableComponent msg = Component.literal("")
                    .append(getColoredString(pos.getName(), COLOR_PINK))
                    .append(" X: ")
                    .append(getColoredString(Integer.toString(pos.getX()), COLOR_BLUE))
                    .append(", Y: ")
                    .append(getColoredString(Integer.toString(pos.getY()), COLOR_BLUE))
                    .append(", Z: ")
                    .append(getColoredString(Integer.toString(pos.getZ()), COLOR_BLUE))
                    .append(", Dim: ")
                    .append(getColoredString(pos.getDim(), COLOR_INDIGO));
                if (dim.equals(getPlayerDim(player))) {// Add distance if in same dim
                    msg.append(", ")
                    .append(
                        getColoredString(
                            Integer.toString(
                                getManhattanDistance(player.blockPosition(), new BlockPos(pos.getX(), pos.getY(), pos.getZ()))
                            ) + " blocks", COLOR_CYAN))
                    .append(" away");
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
        else if (action.equals("set")) {
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
            if (target != null && (target.equals("target") || target.equals("t"))) {
                // Target is set -> get position of the block, the player looks at
                blockPos = getTargetedBlock(player);
                if (blockPos == null) {
                    player.sendSystemMessage(
                        getErrorMsgPrefix()
                        .append("Couldn't determine which block you are looking at, make sure it is ")
                        .append(getColoredString("max. 20 blocks", COLOR_RED))
                        .append(" away.")); // Couldn't determine which block you are targeting, make sure it is max. 20 blocks away.
                    return 0;
                } 
            }
            else {
                // Target is  not set -> get position of the block, the player stands in
                blockPos = player.blockPosition();
            }
            PositionData pos = new PositionData(name, dim, list, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            int index = pdList.add(pos); // If not added, then -1 else postitive integer
            if (index > -1) {
                player.sendSystemMessage(
                    getSuccessMsgPrefix()
                    .append("Coordinates (X: ")
                    .append(getColoredString(Integer.toString(pos.getX()), COLOR_BLUE))
                    .append(", Y: ")
                    .append(getColoredString(Integer.toString(pos.getY()), COLOR_BLUE))
                    .append(", Z: ")
                    .append(getColoredString(Integer.toString(pos.getZ()), COLOR_BLUE))
                    .append(") saved as ")
                    .append(getColoredString(pos.getFQN(), COLOR_GREEN))
                    .append(".")); // Position (X: %s, Y: %s, Z: %s) saved as: %s
            }
            else {
                player.sendSystemMessage(
                    getErrorMsgPrefix()
                    .append("Position ")
                    .append(getColoredString(pos.getFQN(), COLOR_YELLOW))
                    .append(" already saved, please choose different name.")); // Position with name %s already saved, please choose different name.
            }
            return 1;
        }
        else if (action.equals("rem")) {
            // Action is 'rem' -> Remove PositionData from list according to FQN
            String name;
            String list;
            String dim;
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
            PositionData removedPos = pdList.remove(dim, list, name);
            if (removedPos != null) {
                player.sendSystemMessage(
                    getSuccessMsgPrefix()
                    .append("Position ")
                    .append(getColoredString(removedPos.getFQN(), COLOR_BLUE))
                    .append(" removed."));
            }
            else {
                player.sendSystemMessage(
                    getErrorMsgPrefix()
                    .append("Position ")
                    .append(getColoredString(PositionData.genFQN(dim, list, name), COLOR_YELLOW))
                    .append(" not found, please choose different name.")); // Position with name %s not found please choose different name.
            }
            return 1;
        }
        else if (action.equals("update")) {
            // Action is 'update' -> update old position with new coordinates
            String name;
            String list;
            String dim;
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
            if (target != null && (target.equals("target") || target.equals("t"))) {
                // Target is set -> get position of the block, the player looks at
                blockPos = getTargetedBlock(player);
                if (blockPos == null) {
                    player.sendSystemMessage(
                        getErrorMsgPrefix()
                        .append("Couldn't determine which block you are looking at, make sure it is ")
                        .append(getColoredString("max. 20 blocks", COLOR_RED))
                        .append(" away.")); // Couldn't determine which block you are targeting, make sure it is max. 20 blocks away.
                    return 0;
                } 
            }
            else {
                // Target is  not set -> get position of the block, the player stands in
                blockPos = player.blockPosition();
            }
            PositionData updatedPos = pdList.update(dim, list, name, blockPos.getX(), blockPos.getY(), blockPos.getZ()); // If not updated, then null else PositionData
            if (updatedPos != null) {
                player.sendSystemMessage(
                    getSuccessMsgPrefix()
                    .append("Coordinates (X: ")
                    .append(getColoredString(Integer.toString(updatedPos.getX()), COLOR_BLUE))
                    .append(", Y: ")
                    .append(getColoredString(Integer.toString(updatedPos.getY()), COLOR_BLUE))
                    .append(", Z: ")
                    .append(getColoredString(Integer.toString(updatedPos.getZ()), COLOR_BLUE))
                    .append(") updated for ")
                    .append(getColoredString(updatedPos.getFQN(), COLOR_GREEN))
                    .append(".")); // Position (X: %s, Y: %s, Z: %s) updated for: %s
            }
            else {
                player.sendSystemMessage(
                    getErrorMsgPrefix()
                    .append("Position '")
                    .append(getColoredString(PositionData.genFQN(dim, list, name), COLOR_YELLOW))
                    .append("' not found, please choose different name.")); // Position with name %s not found please choose different name.
            }
            return 1;
        }
        else if (action.equals("help")) {
            if (query == null || query.equals("1")) {
                MutableComponent msg = Component.literal("SimplePositions HELP (1/3):\n" + // 
                                                         "Help overview:\n" + //
                                                         "  1: Help overview (this page)\n" + //
                                                         "  2: Available commands\n" + //
                                                         "  3: Additional help and glossary\n" + //
                                                         "Open pages with ")
                                        .append(getColoredString("/pos help <page num>", COLOR_YELLOW))
                                        .append(" .");
                player.sendSystemMessage(msg); 
            }
            else if (query.equals("2")) {
                // Show general help
                MutableComponent msg = Component.literal("SimplePositions HELP (2/3):\nAvailable commands:\n");
                // "set" help
                msg.append(getColoredString("/pos set", COLOR_YELLOW))
                    .append(" - Save player position. ")
                    .append(getColoredString("/pos help set", COLOR_BLUE))
                    .append(" for details.\n");
                // "settarget" help
                msg.append(getColoredString("/pos settarget", COLOR_YELLOW))
                    .append("  Save player targeted position. ")
                    .append(getColoredString("/pos help settarget", COLOR_BLUE))
                    .append(" for details.\n");
                // "get" help
                msg.append(getColoredString("/pos get", COLOR_YELLOW))
                    .append(" - Get saved position. ")
                    .append(getColoredString("/pos help get", COLOR_BLUE))
                    .append(" for details.\n");
                // "rem" help
                msg.append(getColoredString("/pos rem", COLOR_YELLOW))
                    .append(" - Remove saved position. ")
                    .append(getColoredString("/pos help rem", COLOR_BLUE))
                    .append(" for details.\n");
                // "update" help
                msg.append(getColoredString("/pos update", COLOR_YELLOW))
                    .append(" - Update coordinates of saved position. ")
                    .append(getColoredString("/pos help update", COLOR_BLUE))
                    .append(" for details.\n");
                // "lists" help
                msg.append(getColoredString("/pos lists", COLOR_YELLOW))
                    .append(" - Show all created lists. ")
                    .append(getColoredString("/pos help lists", COLOR_BLUE))
                    .append(" for details.\n");
                // "help" help
                msg.append(getColoredString("/pos help", COLOR_YELLOW))
                    .append(" - Show help texts.\n");

                player.sendSystemMessage(msg); 
            }
            else if (query.equals("3")) {
                MutableComponent msg = Component.literal("SimplePositions HELP (3/3):\nAdditional help:\n");
                // "help dim" help
                msg.append(getColoredString("/pos help dim", COLOR_YELLOW))
                    .append(" - Explanation of dimensions.\n");
                // "help list" help
                msg.append(getColoredString("/pos help list", COLOR_YELLOW))
                    .append(" - Explanation of lists.\n");
                // "help name" help
                msg.append(getColoredString("/pos help name", COLOR_YELLOW))
                    .append(" - Explanation of names.\n");
                
                player.sendSystemMessage(msg); 
            }
            else if (query.equals("dim")) {
                // Send dim explanation to player in chat
                player.sendSystemMessage(
                    Component.literal("Saved positions are categorized using dims.\n" + //
                                      "Each position is assigned to the dimension it was saved in or to.\n" + //
                                      "Within a dimension, lists must be unique.\n" + //
                                      "Available dimensions:\n")
                    .append(" ⊳ ")
                    .append(getColoredString("world\n", COLOR_INDIGO))
                    .append(" ⊳ ")
                    .append(getColoredString("nether\n", COLOR_INDIGO))
                    .append(" ⊳ ")
                    .append(getColoredString("end\n", COLOR_INDIGO))); // Possible dimensions: 'world', 'nether', 'end'.
            }
            else if (query.equals("list")) {
                // Send list explanation to player in chat
                player.sendSystemMessage(
                    Component.literal("Saved positions are categorized using lists.\n" + //
                                      "Each position is assigned to a list.\n" + //
                                      "Within a list, position names must be unique.\n" + //
                                      "If no list is specified, the position is saved to the 'default' list."));
            }
            else if (query.equals("name") || query.equals("fqn")) {
                // Send list explanation to player in chat
                player.sendSystemMessage(
                    Component.literal("Saved positions are identified by their FQN.\n" + // 
                                      "Each position has a unique combination (FQN) out of their dim, list and name.\n" + //
                                      "Within a list, position names must be unique.\n" + //
                                      "FQNs can be set as '<dim>.<list>.<name>'."));
            }
            else if (query.equals("set")) {
                // Display help for "set" command 
                player.sendSystemMessage(
                    Component.literal("Help for command 'set':\n" + // 
                                      "Save your current position\nUsage: ")
                        .append(getColoredString("/pos set [<dim>.][<list>.]<name> [target|t]\n", COLOR_BLUE))
                        .append("Example 1: ")
                        .append(getColoredString("/pos set world.camps.hunting\n", COLOR_GREEN))
                        .append("Example 2: ")
                        .append(getColoredString("/pos set \"spawner.zombie-%\"\n", COLOR_GREEN))
                        .append("Attributes:\n" + //
                                "dim    (opt.) : Specify dimension. If omitted, current dim of player is used.\n" + //
                                "list   (opt.) : Specify list. If omitted, ")
                                
                        .append(getColoredString("default", COLOR_TEAL))
                        .append(" list is used.\n" + //
                                "name          : Specify name of position. A ")
                        .append(getColoredString("%", COLOR_YELLOW))
                        .append(" at the end automatically counts up.\n" + //
                                "target (opt.) : A ")
                        .append(getColoredString("t", COLOR_YELLOW))
                        .append(" or ")
                        .append(getColoredString("target", COLOR_YELLOW))
                        .append(" at the end saves the pos you're looking at."));
            }
            else if (query.equals("get")) {
                // Display help for "get" command 
                player.sendSystemMessage(
                    Component.literal("Help for command 'get':\n" + // 
                                      "Retrieve a saved position\nUsage: ")
                        .append(getColoredString("/pos get [<dim>.][<list>.]<name>\n", COLOR_BLUE))
                        .append("Examples:\n")
                        .append(getColoredString("/pos get world.camps.hunting\n", COLOR_GREEN))
                        .append(getColoredString("/pos get nether.+.portal\n", COLOR_GREEN))
                        .append(getColoredString("/pos get mansion\n", COLOR_GREEN))
                        .append("Attributes:\n" + //
                                "dim  (optional): Specify dimension. If omitted, current dim of player is used. Use '+' as a wildcard.\n" + //
                                "list (optional): Specify list. If omitted, 'default' list is used. Use '+' as a wildcard.\n" + //
                                "name           : Specify name of position. Use '+' as a wildcard."));
            }
            return 1;
        }
        else {
            player.sendSystemMessage(
                getErrorMsgPrefix()
                .append("Command ")
                .append(getColoredString(action, COLOR_YELLOW))
                .append(" unknown, see ")
                .append(getColoredString("/pos help", COLOR_BLUE))
                .append(" for info.")); // Command %s unknown, see /pos help for info.
            return 0;
        }
    }

    private static MutableComponent getErrorMsgPrefix() {
        return Component.literal("") // Start with white text
            .append(getColoredString("✘ ", COLOR_RED));
    }
    private static MutableComponent getSuccessMsgPrefix() {
        return Component.literal("") // Start with white text
            .append(getColoredString("✔ ", COLOR_GREEN));
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
        return getErrorMsgPrefix()
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
        return getErrorMsgPrefix()
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

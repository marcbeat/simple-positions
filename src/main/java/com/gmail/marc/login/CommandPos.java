package com.gmail.marc.login;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public class CommandPos {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pos")
            .then(Commands.argument("action", StringArgumentType.word())
                .suggests((context, builder) -> builder.suggest("get").suggest("set").suggest("rem").suggest("update").suggest("lists").suggest("dims").buildFuture())
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

    private static int executeBaseCommand(CommandSourceStack source, String action, String query) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return 0;
        }

        if (action == "dims") {
            // Send dims to player in chat
            player.sendSystemMessage(Component.literal("Possible dimensions: 'world', 'nether', 'end'."));
            return 1;
        }
        else if (action == "lists") {
            // Send existing lists to player in chat
            String allLists = pdList.getLists().
            player.sendSystemMessage(Component.literal("Showing all lists."));
            return 1;
        }
        else if (action == "get") {
            // Send saved positions to player in chat according to search query
            List<PositionData> savedPositions = pdList.get(fqn);
            if (savedPositions.size() < 1)
                player.sendSystemMessage(Component.literal(String.format("No position found matching the query '{}'.", query)));
            else if (savedPositions.size() == 1) {
                PositionData pos = savedPositions.get(0);
                player.sendSystemMessage(Component.literal(String.format("X: {}, Y: {}, Z: {}, Dim: {}", pos.getX(), pos.getY(), pos.getZ(), pos.getDim())));
            }
            else {
                String positionNames = savedPositions.stream().map(PositionData::getFQN).collect(Collectors.joining(", "));
                player.sendSystemMessage(Component.literal(String.format("> 1 positions found: ", positionNames)));
            }
            return 1;
        }
        else if (action == "set") {
            String name;
            String list;
            String dim;
            // Store current positions of player
            if (PositionData.checkFqn(query)) {
                name = PositionData.getNameFromFqn(query);
                list = PositionData.getListFromFqn(query);
                dim = PositionData.getDimFromFqn(query);
            }
            else {
                String[] parts = query.split(".");
                switch (parts.length) {
                    case 3: { // Query e.g. = foo.bar.baz
                        if (PositionData.checkDim(parts[0])) {
                            dim = parts[0];
                        }
                        else {
                            player.sendSystemMessage(Component.literal("Dim invalid, please only use 'world','nether' or 'end'."));
                            return 1;
                        }
                        if (PositionData.checkAllowedChars(parts[1])) {
                            list = parts[1];
                        }
                        else {
                            player.sendSystemMessage(Component.literal("List invalid, please use only a-z,0-9,+-_."));
                            return 1;
                        }
                        if (PositionData.checkAllowedChars(parts[2])) {
                            name = parts[2];
                        }
                        else {
                            player.sendSystemMessage(Component.literal("Name invalid, please use only a-z,0-9,+-_."));
                            return 1;
                        }
                    }
                    case 2: { // Query e.g. = bar.baz
                        if (PositionData.checkAllowedChars(parts[0])) {
                            list = parts[0];
                        }
                        else {
                            player.sendSystemMessage(Component.literal("List malformed, please use only a-z,0-9,+-_."));
                            return 1;
                        }
                        if (PositionData.checkAllowedChars(parts[1])) {
                            name = parts[1];
                        }
                        else {
                            player.sendSystemMessage(Component.literal("Name malformed, please use only a-z,0-9,+-_."));
                            return 1;
                        }
                        dim = getPlayerDim(player);
                    }
                    case 1: { // Query e.g. = baz
                        if (PositionData.checkAllowedChars(parts[0])) {
                            name = parts[0];
                        }
                        else {
                            player.sendSystemMessage(Component.literal("Name malformed, please use only a-z,0-9,+-_."));
                            return 1;
                        }
                        dim = getPlayerDim(player);
                        list = "default";
                    }
                    default: {
                        player.sendSystemMessage(Component.literal("Name malformed, please use only a-z,0-9,+-_."));
                        return 1;
                    }
                }
            }
            BlockPos playerPosBelow = player.blockPosition().below();
            PositionData pos = new PositionData(name,dim,list, playerPosBelow.getX(), playerPosBelow.getY(), playerPosBelow.getZ());
            if (pdList.add(pos))
                player.sendSystemMessage(Component.literal(String.format("Position (X: {}, Y: {}, Z: {}) saved as: {}", pos.getX(), pos.getY(), pos.getZ(), pos.getFQN())));
            else
                player.sendSystemMessage(Component.literal(String.format("Position with name {} already saved, please choose different name.", pos.getFQN())));
            
            return 1;
        }
        else if (action == "rem") {
            // TODO
        }
        else if (action == "update") {
            // TODO
        }


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
}

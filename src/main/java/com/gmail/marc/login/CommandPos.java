package com.gmail.marc.login;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import it.unimi.dsi.fastutil.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class CommandPos {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pos")
            .then(Commands.argument("action", StringArgumentType.word())
                .suggests((context, builder) -> builder.suggest("get").suggest("set").suggest("lists").suggest("dims").buildFuture())
                .executes(context -> {
                    String action = StringArgumentType.getString(context, "action");
                    return executeBaseCommand(context.getSource(), action, null, null, null);
                })
                .then(Commands.argument("arg1", StringArgumentType.word())
                    .suggests((context, builder) -> builder.suggest("dim=").suggest("list=").buildFuture())
                    .executes(context -> {
                        String action = StringArgumentType.getString(context, "action");
                        String arg1 = StringArgumentType.getString(context, "arg1");
                        String dim = null;
                        String list = null;
                        String name = null;
                        if (arg1.startsWith("dim="))
                            dim = arg1;
                        else if (arg1.startsWith("list="))
                            list = arg1;
                        else
                            name = arg1;
                        return executeBaseCommand(context.getSource(), action, name, dim, list);
                    })
                )
                .then(Commands.argument("arg2", StringArgumentType.string())
                    .suggests((context, builder) -> builder.suggest("dim=").suggest("list=").buildFuture())
                    .executes(context -> {
                        String action = StringArgumentType.getString(context, "action");
                        String arg1 = StringArgumentType.getString(context, "arg1");
                        String arg2 = StringArgumentType.getString(context, "arg2");
                        String dim = null;
                        String list = null;
                        String name = null;

                        if (arg1.startsWith("dim="))
                            dim = arg1;
                        else if (arg1.startsWith("list="))
                            list = arg1;
                        else
                            name = arg1;
                        
                        if (arg2.startsWith("dim="))
                            dim = arg2;
                        else if (arg2.startsWith("list="))
                            list = arg2;
                        else
                            name = arg2;

                        return executeBaseCommand(context.getSource(), action, name, dim, list);
                    })
                )
                .then(Commands.argument("arg3", StringArgumentType.string())
                    .suggests((context, builder) -> builder.suggest("dim=").suggest("list=").buildFuture())
                    .executes(context -> {
                        String action = StringArgumentType.getString(context, "action");
                        String arg1 = StringArgumentType.getString(context, "arg1");
                        String arg2 = StringArgumentType.getString(context, "arg2");
                        String arg3 = StringArgumentType.getString(context, "arg3");
                        String dim = null;
                        String list = null;
                        String name = null;

                        if (arg1.startsWith("dim="))
                            dim = arg1;
                        else if (arg1.startsWith("list="))
                            list = arg1;
                        else
                            name = arg1;
                        
                        if (arg2.startsWith("dim="))
                            dim = arg2;
                        else if (arg2.startsWith("list="))
                            list = arg2;
                        else
                            name = arg2;

                        if (arg3.startsWith("dim="))
                            dim = arg3;
                        else if (arg3.startsWith("list="))
                            list = arg3;
                        else
                            name = arg3;

                        return executeBaseCommand(context.getSource(), action, name, dim, list);
                    })
                )
            )
        );
    }

    private static int executeBaseCommand(CommandSourceStack source, String action, String name, String dimArg, String listArg) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
            return 0;
        }

        if (action == "dims") {
            // Send dims to player in chat
            player.sendSystemMessage(Component.literal("Possible dimensions: world, nether, end. Use with dim=<dimension>."));
            return 1;
        }
        else if (action == "dims") {
            // Send existing lists to player in chat
            player.sendSystemMessage(Component.literal("Showing all lists."));
            return 1;
        }

        // Get dim from dimArg
        String dim = null;
        if (dimArg == null)
            dim = getPlayerDim(player);
        else {
            dim = dimArg.split("=")[1];
        }
        ArrayList<String> allowedDims = new ArrayList<>();
        allowedDims.add("world");
        allowedDims.add("nether");
        allowedDims.add("end");

        if (allowedDims.contains(dim)) {
            player.sendSystemMessage(Component.literal("Dimension: " + dim));
            // Add your logic here
        }
        else {
            player.sendSystemMessage(Component.literal("Dimension '" + dim + "' unknown. Use '/pos dims' to get a list of all allowed dims."));
            return 0;
        }

        String list = "default";
        if (listArg != null)
            list = listArg.split("=")[1];
        player.sendSystemMessage(Component.literal("List: " + list));


        if (name == null) {
            player.sendSystemMessage(Component.literal("Name not specified, please add a name to your position."));
            return 0;
        }

        switch (action) {
            
            case "get":
                player.sendSystemMessage(Component.literal("Get command executed"));
                // Add your logic here
                break;
            case "set":
                player.sendSystemMessage(Component.literal("Set command executed"));
                // Add your logic here
                break;
            default:
                player.sendSystemMessage(Component.literal("Unknown action: " + action));
                return 0;
        }

        return 1;
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

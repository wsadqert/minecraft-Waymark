package com.wsadqert.waymark;

import java.util.*;
import static java.util.Map.entry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.wsadqert.waymark.WaymarkStorage.Waymark;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public class WaymarkCommand {
	private enum Color {
		WHITE (0xFFFFFF),
		RED (0xFF0000),
		GREEN (0x00FF00),
		GRAY (0x888888),
		CYAN (0x00FFFF);

		public int value;
		Color(int value){
			this.value = value;
		}
	}
	private static Map<String, String> levelsMapping = Map.ofEntries(
		entry("overworld", "Overworld"),
		entry("the_nether", "Nether"),
		entry("the_end", "The End")
	);

	public static MutableComponent coloredComponent(MutableComponent component, Color color) {
		return component.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color.value)));
	}

	public static void sendMessage(CommandContext<CommandSourceStack> ctx, Component component) {
		ctx.getSource()
				.sendSuccess(() -> coloredComponent(Component.literal("[Waymark] "), Color.GRAY) // gray prefix
						.append(component), false);
	}

	public static void sendMessageSaved(CommandContext<CommandSourceStack> ctx, String name, BlockPos pos, ResourceKey<Level> dimension) {
		sendMessage(ctx, Component.literal("Waymark '")
			.append(coloredComponent(Component.literal(name), Color.GREEN))
			.append(Component.literal("' saved at "))
			.append(coloredComponent(Component.literal("" + pos.getX() + " " + pos.getY() + " " + pos.getZ()), Color.CYAN))
			.append(Component.literal(" in "))
			.append(coloredComponent(Component.literal(levelsMapping.get(dimension.location().getPath())), Color.WHITE))
		);
	}

	private static MutableComponent addWaymarkToListComponent(MutableComponent component, Waymark wm) {
		MutableComponent componentModified = component
				.append(Component.literal("- "))
				.append(coloredComponent(Component.literal(wm.name), Color.GREEN))
				.append(": ")
				.append(coloredComponent(Component.literal("" + wm.pos.getX() + " " + wm.pos.getY() + " " + wm.pos.getZ()), Color.CYAN))
				.append(" in ")
				.append(coloredComponent(Component.literal("" + levelsMapping.get(wm.dimension.getPath())), Color.WHITE))
				.append("\n");
		
		return componentModified;
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("waymark")
				.then(Commands.literal("create")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(ctx -> {
									String name = StringArgumentType.getString(ctx, "name");
									BlockPos pos = ctx.getSource().getPlayerOrException().blockPosition();
									ResourceKey<Level> dimension = ctx.getSource().getLevel().dimension();

									WaymarkStorage.save(name, pos, dimension);
									sendMessageSaved(ctx, name, pos, dimension);
									
									return 1;
								})
								.then(Commands.argument("pos", BlockPosArgument.blockPos())
										.executes(ctx -> {
											String name = StringArgumentType.getString(ctx, "name");
											BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
											ResourceKey<Level> dimension = ctx.getSource().getLevel().dimension();

											WaymarkStorage.save(name, pos, dimension);
											sendMessageSaved(ctx, name, pos, dimension);

											return 1;
										})
										.then(Commands.argument("dimension", ResourceLocationArgument.id())
												.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[] {
															"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
														}, builder))
												.executes(ctx -> {
													String name = StringArgumentType.getString(ctx, "name");
													BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
													ResourceLocation dimId = ResourceLocationArgument.getId(ctx, "dimension");
													ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimId);

													WaymarkStorage.save(name, pos, dimension);
													sendMessageSaved(ctx, name, pos, dimension);

													return 1;
												})))))
				
				.then(Commands.literal("list")
						.executes(ctx -> {
							Map<String, Waymark> data = WaymarkStorage.getData();
							if (data.isEmpty())
								sendMessage(ctx, coloredComponent(Component.literal("No waymarks saved."), Color.RED));

							MutableComponent msgComponent = coloredComponent(Component.literal("Waymarks:\n"), Color.WHITE);
							for (Waymark wm : data.values()) {
								msgComponent = addWaymarkToListComponent(msgComponent, wm);
							}
							
							sendMessage(ctx, msgComponent);

							return 1;
						})
						.then(Commands.argument("dimension", ResourceLocationArgument.id())
							.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(new String[] {
															"minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"
														}, builder))
							.executes(ctx -> {
								ResourceLocation dimId = ResourceLocationArgument.getId(ctx, "dimension");
								ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimId);

								Map<String, Waymark> data = WaymarkStorage.getData();
								if (data.isEmpty())
									sendMessage(ctx, coloredComponent(Component.literal("No waymarks saved."), Color.RED));

								MutableComponent msgComponent = coloredComponent(Component.literal("Waymarks:\n"), Color.WHITE);
								for (Waymark wm : data.values()) {
									// TODO: remove in production
									sendMessage(ctx, Component.literal("" + wm.dimension));
									sendMessage(ctx, Component.literal("" + dimension.location()));

									if (wm.dimension == dimension.location()) {
										msgComponent = addWaymarkToListComponent(msgComponent, wm);
									}
								}
								
								sendMessage(ctx, msgComponent);

								return 1;	
							}))
						
						
						));
	}
}

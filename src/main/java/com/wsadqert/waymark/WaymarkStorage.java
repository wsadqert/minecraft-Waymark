package com.wsadqert.waymark;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class WaymarkStorage {
	private static final Path SAVE_PATH = ServerLifecycleHooks.getCurrentServer()
		.getWorldPath(LevelResource.ROOT)
		.resolve("waymarks.json");

	private static final Map<String, Waymark> data = new HashMap<>();

	public static void save(String name, BlockPos pos, ResourceKey<Level> dimension) {
		data.put(name, new Waymark(name, pos, dimension.location()));
		writeFile();
	}

	public static Map<String, Waymark> getData() {
		return data;
	}

	private static void writeFile() {
		try (Writer writer = Files.newBufferedWriter(SAVE_PATH)) {
			JsonArray arr = new JsonArray();
			for (Waymark wm : data.values()) {
				JsonObject obj = new JsonObject();
				obj.addProperty("name", wm.name);
				obj.addProperty("x", wm.pos.getX());
				obj.addProperty("y", wm.pos.getY());
				obj.addProperty("z", wm.pos.getZ());
				obj.addProperty("dimension", wm.dimension.toString());
				arr.add(obj);
			}
			JsonObject root = new JsonObject();
			root.add("waymarks", arr);
			new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static {
		loadFile();
	}

	private static void loadFile() {
		if (!Files.exists(SAVE_PATH)) return;
		try (Reader reader = Files.newBufferedReader(SAVE_PATH)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray arr = root.getAsJsonArray("waymarks");
			for (JsonElement el : arr) {
				JsonObject obj = el.getAsJsonObject();
				String name = obj.get("name").getAsString();
				int x = obj.get("x").getAsInt();
				int y = obj.get("y").getAsInt();
				int z = obj.get("z").getAsInt();
				ResourceLocation dim = ResourceLocation.parse(obj.get("dimension").getAsString());
				data.put(name, new Waymark(name, new BlockPos(x, y, z), dim));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class Waymark {
		String name;
		BlockPos pos;
		ResourceLocation dimension;

		public Waymark(String name, BlockPos pos, ResourceLocation dim) {
			this.name = name;
			this.pos = pos;
			this.dimension = dim;
		}
	}
}

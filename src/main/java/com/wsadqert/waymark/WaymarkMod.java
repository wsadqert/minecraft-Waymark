package com.wsadqert.waymark;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@Mod("waymark")
@EventBusSubscriber
public class WaymarkMod {
	public static final String MODID = "waymark";

	public WaymarkMod() {
		// Nothing to initialize for now
	}

	@SubscribeEvent
	public static void onCommandRegister(RegisterCommandsEvent event) {
		WaymarkCommand.register(event.getDispatcher());
	}
}

package org.samo_lego.fabrictailor.command;

import com.mojang.authlib.properties.Property;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.function.Supplier;

public class SkinCommand {
    public static void setSkin(ServerPlayerEntity player, Supplier<Optional<Property>> skinProvider) {
        System.out.println("STUB!");
    }
}
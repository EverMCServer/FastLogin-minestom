/**
 *   
 * The MIT License (MIT)
 * 
 * Copyright (c) 2021 djytw
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.evermc.fastlogin.mixins;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.UUID;

import com.evermc.fastlogin.AuthStorage;
import com.evermc.fastlogin.FastLoginExtension;
import com.evermc.fastlogin.MinestomLoginSession;
import com.github.games647.fastlogin.core.StoredProfile;
import com.google.gson.JsonObject;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.network.packet.client.login.EncryptionResponsePacket;
import net.minestom.server.network.packet.client.login.LoginStartPacket;
import net.minestom.server.network.packet.server.login.LoginDisconnectPacket;
import net.minestom.server.network.player.NettyPlayerConnection;
import net.minestom.server.network.player.PlayerConnection;

public class PremiumMixin {

    @Mixin(LoginStartPacket.class)
    public static class LoginStart {

        @Shadow
        public String username;
    
        private static final Component SERVER_STARTING = Component.text("Server is starting", NamedTextColor.RED);
        
        @Redirect(
            at = @At(
                value = "INVOKE",
                target = "Lnet/minestom/server/extras/MojangAuth;isEnabled()Z",
                remap = false
            ),
            remap = false,
            require = 1,
            method = "process"
        )
        private boolean on(PlayerConnection connection) {
            if (FastLoginExtension.isDisabled()) {
                return MojangAuth.isEnabled();
            }
            if (FastLoginExtension.isBungee()) {
                MinecraftServer.LOGGER.info("Bungee player {}", username);
                return false;
            }
            FastLoginExtension.removeSession((InetSocketAddress)connection.getRemoteAddress());
            MinecraftServer.LOGGER.info("Handling player {}", username);
            AuthStorage database = FastLoginExtension.getDatabase();
            if (database == null) {
                connection.sendPacket(new LoginDisconnectPacket(SERVER_STARTING));
                connection.disconnect();
            }
            StoredProfile profile = FastLoginExtension.getDatabase().loadProfile(username);
            if (profile == null) {
                return false;
            }
            InetSocketAddress sockaddr = (InetSocketAddress)connection.getRemoteAddress();
            if (!profile.isPremium()) {
                profile.setLastIp(sockaddr.getAddress().toString().split("/")[1]);
                database.save(profile);
            }
            MinestomLoginSession session = new MinestomLoginSession(username, true, profile);
            FastLoginExtension.putSession(sockaddr, session);
            return profile.isPremium();
        }
    }

    @Mixin(EncryptionResponsePacket.class)
    public static class EncryptionResponse {

        @Inject(
            at = @At(
                value = "INVOKE",
                target = "Lnet/minestom/server/network/ConnectionManager;startPlayState(Lnet/minestom/server/network/player/PlayerConnection;Ljava/util/UUID;Ljava/lang/String;Z)Lnet/minestom/server/entity/Player;",
                remap = false
            ),
            remap = false,
            require = 1,
            method = "lambda$process$0(Lnet/minestom/server/network/player/NettyPlayerConnection;Lnet/minestom/server/network/player/PlayerConnection;)V",
            locals = LocalCapture.CAPTURE_FAILHARD
        )
        private void on(NettyPlayerConnection nettyConnection, PlayerConnection ignore0, CallbackInfo info, String ignore1, byte[] ignore2, String ignore3, InputStream ingore4, JsonObject ignore5, UUID profileUUID, String profileName) {
            InetSocketAddress sockaddr = (InetSocketAddress)nettyConnection.getRemoteAddress();
            MinestomLoginSession session = FastLoginExtension.getSession(sockaddr);
            if (session == null) {
                MinecraftServer.LOGGER.info("No valid session found for {}", profileName);
                return;
            }
            StoredProfile profile = session.getProfile();
            profile.setId(profileUUID);
            profile.setLastIp(sockaddr.getAddress().toString().split("/")[1]);
            profile.setPremium(true);
            FastLoginExtension.getDatabase().save(profile);
        }
    }
}

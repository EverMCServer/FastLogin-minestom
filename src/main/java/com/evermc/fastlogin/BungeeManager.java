/**
 *   
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015-2021 games647 and contributors
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

package com.evermc.fastlogin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.games647.fastlogin.core.message.LoginActionMessage;
import com.github.games647.fastlogin.core.message.LoginActionMessage.Type;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import net.kyori.adventure.text.TextComponent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;

public class BungeeManager {

    private static final String FILE_NAME = "allowed-proxies.txt";
    private static Set<UUID> proxyIds = Collections.emptySet();
    
    public static final Consumer<PlayerPluginMessageEvent> LISTENER = event -> {
        if (!"fastlogin:force".equals(event.getIdentifier())) {
            return;
        }
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(event.getMessage());
        LoginActionMessage loginMessage = new LoginActionMessage();
        loginMessage.readFrom(dataInput);

        MinecraftServer.LOGGER.debug("Received plugin message {}", loginMessage);

        Player targetPlayer = event.getPlayer();
        String name = ((TextComponent)targetPlayer.getName()).content();
        if (!loginMessage.getPlayerName().equals(name)) {
            targetPlayer = MinecraftServer.getConnectionManager().getPlayer(loginMessage.getPlayerName());
        }

        if (targetPlayer == null) {
            MinecraftServer.LOGGER.warn("Force action player {} not found", loginMessage.getPlayerName());
            return;
        }

        UUID sourceId = loginMessage.getProxyId();
        if (isProxyAllowed(sourceId)) {
            readMessage(targetPlayer, loginMessage);
        } else {
            MinecraftServer.LOGGER.warn("Received proxy id: {} that doesn't exist in the proxy file", sourceId);
        }
    };

    public static boolean isProxyAllowed(UUID proxyId) {
        return proxyIds != null && proxyIds.contains(proxyId);
    }

    private static void readMessage(Player player, LoginActionMessage message) {
        String playerName = message.getPlayerName();
        Type type = message.getType();

        InetSocketAddress address = (InetSocketAddress)player.getPlayerConnection().getRemoteAddress();
        MinecraftServer.LOGGER.info("Player info {} command for {} from proxy", type, playerName);
        if (type == Type.LOGIN) {
            MinecraftServer.LOGGER.info("LOGIN: {} {} {}", player.getUuid(), playerName, address);
            // TODO - ForceLogin
        }
    }

    public static void loadBungeeCordIds(FastLoginExtension extension) {
        Path proxiesFile = extension.getDataDirectory().resolve(FILE_NAME);
        try {
            if (Files.notExists(proxiesFile)) {
                Files.createFile(proxiesFile);
            }
            try (Stream<String> lines = Files.lines(proxiesFile)) {
                proxyIds = lines.map(String::trim)
                        .map(UUID::fromString)
                        .collect(Collectors.toSet());
                return;
            }
        } catch (IOException ex) {
            MinecraftServer.LOGGER.error("Failed to read proxies", ex);
        } catch (Exception ex) {
            MinecraftServer.LOGGER.error("Failed to retrieve proxy Id. Disabling BungeeCord support", ex);
        }

        proxyIds = Collections.emptySet();
    }

}

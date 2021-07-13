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

package com.evermc.fastlogin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

import com.evermc.fastlogin.mixins.MojangAuthAccessor;
import com.evermc.fastlogin.utils.OfflineUUIDProvider;
import com.github.games647.fastlogin.core.CommonUtil;

import org.yaml.snakeyaml.Yaml;

import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.extensions.Extension;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.mojangAuth.MojangCrypt;

public class FastLoginExtension extends Extension {

    private static Config mainConfig = null;
    private static AuthStorage database = null;
    private static boolean disabled = false;
    private static boolean isBungee = false;
    private static final ConcurrentMap<String, MinestomLoginSession> loginSession = CommonUtil.buildCache(1, -1);

    @Override
    public void preInitialize() {
        // check online-mode
        if (!BungeeCordProxy.isEnabled() && !MojangAuth.isEnabled()) {
            if (MinecraftServer.isStarted()) {
                // online-mode disabled, server started, not bungee: force enable authentication
                MojangAuthAccessor.setEnabled(true);
                MojangAuthAccessor.setKeyPair(MojangCrypt.generateKeyPair());
            } else {
                // normal
                MojangAuth.init();
            }
        }
        super.preInitialize();
    }

    @Override
    public void initialize() {
        try {
            String config = getConfig("config.yml");
            Yaml yaml = new Yaml();
            mainConfig = yaml.loadAs(config, Config.class);

            database = new AuthStorage(this, mainConfig);
            database.createTables();
            MinecraftServer.LOGGER.info("[FastLogin] Fastlogin started.");
        } catch (Exception e) {
            e.printStackTrace();
            MinecraftServer.LOGGER.error("[FastLogin] Config load failed.");
            FastLoginExtension.disabled = true;
            return;
        }
        if (mainConfig.use_vanilla_offline_uuid) {
            MinecraftServer.getConnectionManager().setUuidProvider(new OfflineUUIDProvider());
        }
        if (BungeeCordProxy.isEnabled()) {
            isBungee = true;
            BungeeManager.loadBungeeCordIds(this);
            MinecraftServer.getGlobalEventHandler().addListener(PlayerPluginMessageEvent.class, BungeeManager.LISTENER);
        }
    }

    @Override
    public void terminate() {
        if (FastLoginExtension.disabled) {
            return;
        }
    }

    public static boolean isDisabled() {
        return FastLoginExtension.disabled;
    }

    public static boolean isBungee() {
        return FastLoginExtension.isBungee;
    }

    public static AuthStorage getDatabase() {
        return FastLoginExtension.database;
    }

    public String getConfig(String path) throws IOException {
        Path file = this.getDataDirectory().resolve(path);
        if (!file.toFile().exists()) {
            this.savePackagedResource(Path.of(path));
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public static ConcurrentMap<String, MinestomLoginSession> getLoginSessions() {
        return loginSession;
    }

    public static MinestomLoginSession getSession(InetSocketAddress addr) {
        String id = getSessionId(addr);
        return loginSession.get(id);
    }

    public static String getSessionId(InetSocketAddress addr) {
        return addr.getAddress().getHostAddress() + ':' + addr.getPort();
    }

    public static void putSession(InetSocketAddress addr, MinestomLoginSession session) {
        String id = getSessionId(addr);
        loginSession.put(id, session);
    }

    public static void removeSession(InetSocketAddress addr) {
        String id = getSessionId(addr);
        loginSession.remove(id);
    }

}

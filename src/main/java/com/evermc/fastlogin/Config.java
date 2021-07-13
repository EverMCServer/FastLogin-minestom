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

public class Config {

    public boolean use_vanilla_offline_uuid = true;
    public PermissionConfig permissions_set_premium_self = new PermissionConfig(1, "fastlogin.command.premium");
    public PermissionConfig permissions_set_premium_others = new PermissionConfig(4, "fastlogin.command.premium.others");
    public SQLiteConfig sqlite = new SQLiteConfig();
    public MySQLConfig mysql = new MySQLConfig();
    public String datasource = "sqlite";

    public static class PermissionConfig {
        public int permission_level;
        public String permission;

        public PermissionConfig(int permission_level, String permission) {
            this.permission = permission;
            this.permission_level = permission_level;
        }

        public PermissionConfig() {}
    }

    public static class SQLiteConfig {
        public String filename = "fastlogin.db";
    }

    public static class MySQLConfig {
        public String hostname;
        public String username;
        public String password;
        public String db_name;
        public int port = 3306;
        public int maximumPoolSize = 5;
    }
}

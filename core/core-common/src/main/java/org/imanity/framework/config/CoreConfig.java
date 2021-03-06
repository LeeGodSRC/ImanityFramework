/*
 * MIT License
 *
 * Copyright (c) 2021 Imanity
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

package org.imanity.framework.config;

import org.imanity.framework.ImanityCommon;
import org.imanity.framework.RepositoryType;
import org.imanity.framework.config.annotation.ConfigurationElement;
import org.imanity.framework.config.format.FieldNameFormatters;
import org.imanity.framework.config.yaml.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CoreConfig extends YamlConfiguration {

    public boolean USE_REDIS = false;

    public boolean USE_REDIS_DISTRIBUTED_LOCK = false;

    public boolean USE_LOCALE = false; // TODO: make a better way to decide enable or disable locale

    public boolean ASYNCHRONOUS_DATA_STORING = true;

    public String CURRENT_SERVER = "server-1";

    public String DEFAULT_LOCALE = "en_us";

    public Storage STORAGE = new Storage();

    public CoreConfig() {
        super(new File(ImanityCommon.PLATFORM.getDataFolder(), "core.yml").toPath(), YamlProperties
            .builder()
                .setFormatter(FieldNameFormatters.LOWER_CASE)
            .setPrependedComments(Arrays.asList(
                    "==============================",
                    "The configuration to adjust data settings",
                    "==============================",
                    " "
            )).build());
    }

    @ConfigurationElement
    public static class Storage {

        public RepositoryType DEFAULT_TYPE = RepositoryType.H2;
        public MySql MYSQL = new MySql();
        public Mongo MONGO = new Mongo();

    }

    @ConfigurationElement
    public static class MySql {

        public boolean ENABLED = false;
        public String HOST = "localhost";
        public String PORT = "3306";
        public String USER = "admin";
        public String PASSWORD = "password";
        public String DATABASE = "database";

    }

    @ConfigurationElement
    public static class Mongo {

        public boolean ENABLED = false;
        public String CONNECTION_STRING = "mongodb://localhost:27017";
        public String DATABASE = "database";

    }
}

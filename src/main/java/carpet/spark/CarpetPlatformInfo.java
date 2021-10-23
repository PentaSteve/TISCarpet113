/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package carpet.spark;

import carpet.settings.CarpetSettings;
import me.lucko.spark.common.platform.AbstractPlatformInfo;

public class CarpetPlatformInfo extends AbstractPlatformInfo {
    private final Type type;

    public CarpetPlatformInfo(Type type) {
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getName() {
        return "TIS Carpet";
    }

    @Override
    public String getVersion() {
        return CarpetSettings.carpetVersion;
    }

    @Override
    public String getMinecraftVersion() {
        return "1.13.2";
    }
}

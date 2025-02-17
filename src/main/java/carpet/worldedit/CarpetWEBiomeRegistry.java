/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package carpet.worldedit;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.biome.BiomeData;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

class CarpetWEBiomeRegistry implements BiomeRegistry {

    @Override
    public Component getRichName(BiomeType biomeType) {
        return TranslatableComponent.of(Util.makeTranslationKey("biome", new ResourceLocation(biomeType.getId())));
    }

    @Deprecated
    @Override
    public BiomeData getData(BiomeType biome) {
        return new CarpetWEBiomeData(biome);
    }

    /**
     * Cached biome data information.
     */
    @Deprecated
    private static class CarpetWEBiomeData implements BiomeData {
        private final BiomeType biome;

        /**
         * Create a new instance.
         *
         * @param biome the base biome
         */
        private CarpetWEBiomeData(BiomeType biome) {
            this.biome = biome;
        }

        @SuppressWarnings("deprecation")
        @Override
        public String getName() {
            return biome.getId();
        }
    }

}

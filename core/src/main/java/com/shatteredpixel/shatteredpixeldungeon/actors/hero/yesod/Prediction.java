/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.actors.hero.yesod;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//the predict ability of Yesod's scarf: the next attack against the hero always misses,
// and the hero's next attack always hits. Each part is spent separately in Char.hit
public class Prediction {

	public static class Evade extends Buff {
		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.FORESIGHT;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.75f, 0.5f, 1f);
		}
	}

	public static class Strike extends Buff {
		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TARGETED;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.75f, 0.5f, 1f);
		}
	}
}

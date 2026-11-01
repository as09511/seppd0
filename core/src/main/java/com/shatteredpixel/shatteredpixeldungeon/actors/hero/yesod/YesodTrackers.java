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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.watabou.utils.Bundle;

//small bookkeeping buffs for Yesod's talents
public class YesodTrackers {

	//document review: items that can still be identified for free (one per talent point)
	public static class DocumentReviewCharges extends CounterBuff {
		{
			revivePersists = true;
		}
	}

	//emergency response: the depth it was last used on, it works once per floor
	public static class EmergencyResponseUsed extends Buff {
		{
			revivePersists = true;
		}

		public int depth = -1;

		public static boolean usedHere(){
			EmergencyResponseUsed b = Dungeon.hero.buff(EmergencyResponseUsed.class);
			return b != null && b.depth == Dungeon.depth;
		}

		public static void markUsed(){
			Buff.affect(Dungeon.hero, EmergencyResponseUsed.class).depth = Dungeon.depth;
		}

		private static final String DEPTH = "depth";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DEPTH, depth);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			depth = bundle.getInt(DEPTH);
		}
	}
}

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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//small bookkeeping buffs for Yesod's talents
public class YesodTrackers {

	//document review: items that can still be identified for free (one per talent point)
	public static class DocumentReviewCharges extends CounterBuff {
		{
			revivePersists = true;
		}
	}

	//document review: how many points have already handed out a charge, so a memory extraction reset can't farm them
	public static class DocumentReviewGranted extends CounterBuff {
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

	//shows the info gained by the surveillance camera once the level is on screen
	public static class CameraReport extends Buff {

		{
			//acts before the hero, so the text appears as soon as the level is shown
			actPriority = VFX_PRIO;
		}

		public int gained = 0;

		@Override
		public boolean act() {
			if (gained > 0 && target.sprite != null){
				target.sprite.showStatus(InfoPower.GAIN_COLOR, Messages.get(InfoPower.class, "gain", gained));
			}
			detach();
			return true;
		}

		private static final String GAINED = "gained";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(GAINED, gained);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			gained = bundle.getInt(GAINED);
		}
	}

	//floors Yesod has already arrived on, for the surveillance camera and commission talents
	public static class VisitedFloors extends Buff {
		{
			revivePersists = true;
		}

		private ArrayList<Integer> floors = new ArrayList<>();

		private static final String FLOORS = "floors";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			int[] arr = new int[floors.size()];
			for (int i = 0; i < arr.length; i++) arr[i] = floors.get(i);
			bundle.put(FLOORS, arr);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			floors.clear();
			if (bundle.contains(FLOORS)){
				for (int f : bundle.getIntArray(FLOORS)) floors.add(f);
			}
		}
	}

	//called when the hero arrives on a level (Dungeon.switchLevel), only acts the first time
	public static void onLevelArrived( Hero hero, Level level ){
		if (hero.heroClass != HeroClass.YESOD) return;
		VisitedFloors visited = Buff.affect(hero, VisitedFloors.class);
		int key = Dungeon.depth*100 + Dungeon.branch;
		if (visited.floors.contains(key)) return;
		visited.floors.add(key);

		//surveillance camera: like magic mapping around the stairs he arrived by, 8/10/12 tiles.
		//hidden traps and secret doors are found too, giving info and triggering security procedure
		if (hero.hasTalent(Talent.SURVEILLANCE_CAMERA)){
			int range = 6 + 2*hero.pointsInTalent(Talent.SURVEILLANCE_CAMERA);
			int info = 0;
			for (int i = 0; i < level.length(); i++){
				if (level.discoverable[i] && level.distance(hero.pos, i) <= range){
					level.mapped[i] = true;
					int terr = level.map[i];
					if ((Terrain.flags[terr] & Terrain.SECRET) != 0){
						level.discover(i);
						info += terr == Terrain.SECRET_TRAP ? InfoPower.SECRET_TRAP_INFO : InfoPower.SECRET_DOOR_INFO;
					}
				}
			}
			int before = InfoPower.current();
			InfoPower.gain(info);
			//the level is still loading, so the "+info" text is shown on the hero's first turn there
			int gained = InfoPower.current() - before;
			if (gained > 0){
				Buff.affect(hero, CameraReport.class).gained = gained;
			}
		}

		//commission: one random non-boss enemy on the floor becomes the target
		if (hero.hasTalent(Talent.COMMISSION)){
			ArrayList<Mob> candidates = new ArrayList<>();
			for (Mob m : level.mobs){
				if (m.alignment == Char.Alignment.ENEMY && m.isAlive()
						&& !Char.hasProp(m, Char.Property.BOSS) && !Char.hasProp(m, Char.Property.MINIBOSS)
						&& !Char.hasProp(m, Char.Property.IMMOVABLE)){
					candidates.add(m);
				}
			}
			if (!candidates.isEmpty()){
				Mob target = Random.element(candidates);
				Buff.affect(target, Commission.class);
			}
		}
	}

	//damage bonuses on the hero's attacks from the Yesod subclass talents and charge,
	// added together and applied to the damage left after the enemy's armor
	public static float attackMultiplier( Hero hero ){
		float bonus = 0f;
		if (hero.hasTalent(Talent.VIPER_YESOD)){
			//+1/1.5/2% per info held
			bonus += 0.005f*(1 + hero.pointsInTalent(Talent.VIPER_YESOD)) * InfoPower.current();
		}
		Charge charge = hero.buff(Charge.class);
		if (charge != null){
			bonus += charge.damageMultiplier() - 1f;
		}
		if (hero.hasTalent(Talent.BLACK_FLAME)){
			//+50/75/100%
			bonus += 0.25f + 0.25f*hero.pointsInTalent(Talent.BLACK_FLAME);
		}
		return 1f + bonus;
	}
}

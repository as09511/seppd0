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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.tiphereth;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

//Tiphereth B waiting out of sight, the same as a prismatic guard: she steps out when an enemy comes close,
//and recovers slowly while she waits
public class TipherethGuard extends Buff {

	{
		type = buffType.POSITIVE;
	}

	private float HP;

	@Override
	public boolean act() {
		Hero hero = (Hero) target;

		Mob closest = null;
		int v = hero.visibleEnemies();
		for (int i = 0; i < v; i++){
			Mob mob = hero.visibleEnemy(i);
			if (mob.isAlive() && !mob.isInvulnerable(TipherethB.class)
					&& mob.state != mob.PASSIVE && mob.state != mob.WANDERING && mob.state != mob.SLEEPING
					&& !hero.mindVisionEnemies.contains(mob)
					&& (closest == null || Dungeon.level.distance(hero.pos, mob.pos) < Dungeon.level.distance(hero.pos, closest.pos))){
				closest = mob;
			}
		}

		if (closest != null && Dungeon.level.distance(hero.pos, closest.pos) < 5){
			int bestPos = -1;
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
				int p = hero.pos + PathFinder.NEIGHBOURS8[i];
				if (Actor.findChar(p) == null && Dungeon.level.passable[p]){
					if (bestPos == -1 || Dungeon.level.trueDistance(p, closest.pos) < Dungeon.level.trueDistance(bestPos, closest.pos)){
						bestPos = p;
					}
				}
			}
			if (bestPos != -1){
				TipherethB b = new TipherethB();
				b.duplicate(hero, Math.max(1, (int) Math.floor(HP)));
				b.state = b.HUNTING;
				GameScene.add(b, 1);
				ScrollOfTeleportation.appear(b, bestPos);
				detach();
				return true;
			}
		}

		spend(TICK);
		if (HP < maxHP() && Regeneration.regenOn()){
			HP = Math.min(maxHP(), HP + 0.1f);
		}
		return true;
	}

	public void set( int HP ){
		this.HP = HP;
	}

	public void heal(){
		HP = maxHP();
	}

	public int maxHP(){
		return TipherethB.maxHealth((Hero) target);
	}

	@Override
	public int icon() {
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight(1f, 0.85f, 0.4f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - HP / (float) maxHP();
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) HP);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", (int) HP, maxHP());
	}

	private static final String HEALTH = "hp";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(HEALTH, HP);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		HP = bundle.getFloat(HEALTH);
	}
}

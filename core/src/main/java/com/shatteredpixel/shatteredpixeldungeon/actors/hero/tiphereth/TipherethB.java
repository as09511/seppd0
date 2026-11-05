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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TipherethBSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Tiphereth B, called by the HQ sephirah's prisma scroll of mirror image. A mirror image that stays in sight:
//she has 1.5x Tiphereth's max health, takes part of the health damage Tiphereth takes, and comes back to her side
//whenever she ends up 3 or more tiles away.
public class TipherethB extends MirrorImage {

	public static final float BASE_HEALTH = 1.5f;
	public static final int RETURN_DISTANCE = 3;

	{
		spriteClass = TipherethBSprite.class;
		intelligentAlly = true;
	}

	private int ownerID = -1;

	public Hero owner(){
		Actor a = Actor.findById(ownerID);
		return a instanceof Hero ? (Hero) a : null;
	}

	//the Tiphereth B on this floor, if she is alive
	public static TipherethB get(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof TipherethB && m.isAlive()) return (TipherethB) m;
		}
		return null;
	}

	//precious one: +40/70/100% health
	public static int maxHealth( Hero hero ){
		float mult = BASE_HEALTH;
		if (hero.hasTalent(Talent.PRECIOUS_ONE)) mult *= 1.1f + 0.3f*hero.pointsInTalent(Talent.PRECIOUS_ONE);
		return Math.max(1, Math.round(hero.HT * mult));
	}

	//the share of Tiphereth's health damage she takes: 50%, value disposal 60/70/80%
	public static float shareRatio( Hero hero ){
		return 0.5f + 0.1f*hero.pointsInTalent(Talent.VALUE_DISPOSAL);
	}

	@Override
	public void duplicate( Hero hero ) {
		super.duplicate(hero);
		ownerID = hero.id();
		//she fights in the open, unlike a mirror image
		Buff.detach(this, MirrorInvis.class);
		HT = HP = maxHealth(hero);
	}

	@Override
	protected boolean act() {
		Hero hero = owner();
		if (hero != null){
			//keeps up with Tiphereth's max health, at the same fraction
			int newHT = maxHealth(hero);
			if (newHT != HT){
				HP = Math.max(1, Math.round(HP * newHT / (float) HT));
				HT = newHT;
			}
			armTier = hero.tier();
			if (Dungeon.level.distance(pos, hero.pos) >= RETURN_DISTANCE) returnTo(hero);
		}
		return super.act();
	}

	private void returnTo( Hero hero ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) cells.add(c);
		}
		if (!cells.isEmpty()) ScrollOfTeleportation.appear(this, Random.element(cells));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + TipherethTrackers.physicalShield(this);
	}

	@Override
	public void damage( int dmg, Object src ) {
		super.damage(TipherethTrackers.traumaShield(this, dmg, src), src);
	}

	@Override
	public CharSprite sprite() {
		return new TipherethBSprite();
	}

	private static final String OWNER = "owner";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerID);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerID = bundle.getInt(OWNER);
	}
}

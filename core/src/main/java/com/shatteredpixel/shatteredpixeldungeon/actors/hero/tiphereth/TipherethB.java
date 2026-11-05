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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrismaticGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrismaticImage;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.TipherethBSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Tiphereth B: the HQ sephirah's scroll of prismatic image calls her instead of a prismatic image.
//She is a prismatic image with 1.5x the health that doesn't hide away between fights: she takes part of the
//health damage Tiphereth takes, slowly recovers like a prismatic guard, comes back to Tiphereth's side from
//3 or more tiles away and follows her between floors. Like a prismatic image she fades for a few turns when she
//falls, and another scroll of prismatic image brings her back to full health.
public class TipherethB extends PrismaticImage {

	public static final float BASE_HEALTH = 1.5f;
	public static final int RETURN_DISTANCE = 3;
	//the same pace a prismatic guard heals at
	public static final float REGEN = 0.1f;

	{
		spriteClass = TipherethBSprite.class;
		//she stays out instead of hiding away when no enemy is in sight
		WANDERING = new Stay();
	}

	//plain wandering, without the prismatic image's return into the guard
	private class Stay extends Mob.Wandering {}

	private int ownerID = -1;
	private float partialRegen = 0;

	public Hero owner(){
		Actor a = Actor.findById(ownerID);
		return a instanceof Hero ? (Hero) a : null;
	}

	//the Tiphereth B on this floor that can take damage
	public static TipherethB get(){
		TipherethB b = find();
		return b != null && b.isAlive() ? b : null;
	}

	//the Tiphereth B on this floor, also while she is fading after falling
	public static TipherethB find(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof TipherethB && m.isActive()) return (TipherethB) m;
		}
		return null;
	}

	//1.5x a prismatic image's health, precious one adds +40/70/100%
	public static int maxHealth( Hero hero ){
		float mult = BASE_HEALTH;
		if (hero.hasTalent(Talent.PRECIOUS_ONE)) mult *= 1.1f + 0.3f*hero.pointsInTalent(Talent.PRECIOUS_ONE);
		return Math.max(1, Math.round(PrismaticGuard.maxHP(hero) * mult));
	}

	//the share of Tiphereth's health damage she takes: 50%, value disposal 60/70/80%
	public static float shareRatio( Hero hero ){
		return 0.5f + 0.1f*hero.pointsInTalent(Talent.VALUE_DISPOSAL);
	}

	//the HQ sephirah reading a scroll of prismatic image: Tiphereth B comes to her side, or is healed if she is here
	public static void call( Hero hero ){
		TipherethB b = find();
		if (b != null){
			b.HP = b.HT;
			b.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(b.HT), FloatingText.HEALING);
			return;
		}
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) cells.add(c);
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(TipherethB.class, "no_space"));
			return;
		}
		b = new TipherethB();
		int max = maxHealth(hero);
		b.duplicate(hero, max);
		GameScene.add(b);
		ScrollOfTeleportation.appear(b, Random.element(cells));
		b.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
		GLog.p(Messages.get(TipherethB.class, "called"));
	}

	@Override
	public void duplicate( Hero hero, int HP ) {
		super.duplicate(hero, HP);
		ownerID = hero.id();
		HT = maxHealth(hero);
		this.HP = Math.min(HP, HT);
	}

	@Override
	protected boolean act() {
		Hero hero = owner();
		if (hero != null && isAlive()){
			//keeps up with her max health as Tiphereth grows, at the same fraction
			int newHT = maxHealth(hero);
			if (newHT != HT){
				HP = Math.max(1, Math.round(HP * newHT / (float) HT));
				HT = newHT;
			}
			if (HP < HT && Regeneration.regenOn()){
				partialRegen += REGEN;
				if (partialRegen >= 1){
					HP = Math.min(HT, HP + 1);
					partialRegen -= 1;
				}
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
		if (!cells.isEmpty()){
			ScrollOfTeleportation.appear(this, Random.element(cells));
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.5f);
		}
	}

	//her armor is Tiphereth's own roll, physical interference shield included
	@Override
	public void damage( int dmg, Object src ) {
		super.damage(TipherethTrackers.traumaShield(this, dmg, src), src);
	}

	@Override
	public CharSprite sprite() {
		return new TipherethBSprite();
	}

	private static final String OWNER = "owner";
	private static final String PARTIAL = "partial_regen";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(OWNER, ownerID);
		bundle.put(PARTIAL, partialRegen);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		ownerID = bundle.getInt(OWNER);
		partialRegen = bundle.getFloat(PARTIAL);
	}
}

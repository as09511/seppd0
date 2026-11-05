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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
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
//She is a prismatic image with 1.5x Tiphereth's max health: she takes part of the health damage Tiphereth takes,
//comes back to Tiphereth's side from more than 3 tiles away and follows her between floors. Like a prismatic image
//she waits out of sight when no enemy is around (see TipherethGuard), fades for a few turns when she falls,
//and another scroll of prismatic image brings her back to full health.
public class TipherethB extends PrismaticImage {

	public static final float BASE_HEALTH = 1.5f;
	public static final int RETURN_DISTANCE = 3;

	{
		spriteClass = TipherethBSprite.class;
		WANDERING = new Wait();
	}

	//like a prismatic image, she steps back out of sight when no enemy is in view
	private class Wait extends Mob.Wandering {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			Hero hero = owner();
			if (!enemyInFOV && hero != null){
				Buff.affect(hero, TipherethGuard.class).set(HP);
				destroy();
				CellEmitter.get(pos).start(Speck.factory(Speck.LIGHT), 0.2f, 3);
				sprite.die();
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	private int ownerID = -1;

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

	//1.5x Tiphereth's max health, precious one adds +40/70/100%
	public static int maxHealth( Hero hero ){
		float mult = BASE_HEALTH;
		if (hero.hasTalent(Talent.PRECIOUS_ONE)) mult *= 1.1f + 0.3f*hero.pointsInTalent(Talent.PRECIOUS_ONE);
		return Math.max(1, Math.round(hero.HT * mult));
	}

	//the share of Tiphereth's health damage she takes: 50%, value disposal 60/70/80%
	public static float shareRatio( Hero hero ){
		return 0.5f + 0.1f*hero.pointsInTalent(Talent.VALUE_DISPOSAL);
	}

	//the HQ sephirah reading a scroll of prismatic image, the same as the prismatic image: Tiphereth B is healed
	//to full if she is here, otherwise she waits out of sight at full health until an enemy comes close
	public static void call( Hero hero ){
		TipherethB b = find();
		if (b != null){
			b.HP = b.HT;
			b.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(b.HT), FloatingText.HEALING);
			return;
		}
		Buff.affect(hero, TipherethGuard.class).heal();
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
			armTier = hero.tier();
			if (Dungeon.level.distance(pos, hero.pos) > RETURN_DISTANCE) returnTo(hero);
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

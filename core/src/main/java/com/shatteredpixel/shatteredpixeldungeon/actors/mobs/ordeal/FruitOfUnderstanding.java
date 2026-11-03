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


package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ordeal;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.FruitOfUnderstandingSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Dawn of Violet: a slow fruit with a weak touch that, every so often, charges up for 2 turns like Goo
//and then strikes everything within 2 tiles (5x5) with 6x damage and 2x accuracy
public class FruitOfUnderstanding extends OrdealMob {

	public static final int BLAST_RANGE     = 2;
	public static final int CHARGE_TURNS    = 2;
	public static final float BLAST_COOLDOWN = 7f;
	public static final float BLAST_DAMAGE   = 6f;
	public static final float BLAST_ACCURACY = 2f;

	{
		spriteClass = FruitOfUnderstandingSprite.class;

		HP = HT = 190;
		defenseSkill = 10;
		EXP = 12;
		baseSpeed = 0.5f;

		attackType = DamageType.WHITE;
		damageMultipliers = new float[]{1.0f, 1.5f, 1.0f, 1.0f};
	}

	//0 = not charging, otherwise the turns charged so far
	private int charge = 0;
	//time left until it can charge again
	private float cooldown = 0;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2, 6);
	}

	@Override
	public int attackSkill( Char target ) {
		return 12;
	}

	//both ways, like Goo, so walls and corners never let the blast through
	private boolean inBlast( int cell ){
		return Dungeon.level.distance(cell, pos) <= BLAST_RANGE
				&& new Ballistica( pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == cell
				&& new Ballistica( cell, pos, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID).collisionPos == pos;
	}

	public boolean charging(){
		return charge > 0;
	}

	@Override
	protected void spend( float time ) {
		super.spend(time);
		if (charge == 0 && cooldown > 0){
			cooldown = Math.max(0, cooldown - time);
		}
	}

	@Override
	protected boolean act() {
		if (charge > 0){
			//a paralysed fruit loses its charge
			if (paralysed > 0){
				cancelCharge();
				return super.act();
			}
			if (charge < CHARGE_TURNS){
				charge++;
				if (sprite != null) ((FruitOfUnderstandingSprite)sprite).charge(charge);
				spend(TICK);
				return true;
			}
			return blast();
		}
		return super.act();
	}

	@Override
	protected boolean canAttack( Char enemy ) {
		if (cooldown <= 0 && inBlast(enemy.pos)){
			return true;
		}
		return super.canAttack(enemy);
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (cooldown <= 0 && inBlast(enemy.pos)){
			charge = 1;
			if (sprite != null) ((FruitOfUnderstandingSprite)sprite).charge(charge);
			if (Dungeon.level.heroFOV[pos]){
				sprite.showStatus( CharSprite.WARNING, Messages.get(this, "!!!") );
				GLog.n( Messages.get(this, "charge") );
			}
			spend(TICK);
			return true;
		}
		return super.doAttack(enemy);
	}

	private boolean blast(){
		charge = 0;
		cooldown = BLAST_COOLDOWN;

		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch != this && ch.isAlive() && inBlast(ch.pos)){
				targets.add(ch);
			}
		}

		if (sprite != null){
			((FruitOfUnderstandingSprite)sprite).blast();
		}
		if (Dungeon.level.heroFOV[pos]){
			PixelScene.shake( 3, 0.2f );
			Sample.INSTANCE.play( Assets.Sounds.BLAST, 1f, 0.8f );
		}

		for (Char ch : targets){
			attack(ch, BLAST_DAMAGE, 0f, BLAST_ACCURACY);
		}

		spend( attackDelay() );
		return true;
	}

	private void cancelCharge(){
		charge = 0;
		if (sprite != null){
			((FruitOfUnderstandingSprite)sprite).charge(0);
			sprite.idle();
		}
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		if (charge > 0){
			((FruitOfUnderstandingSprite)sprite).charge(charge);
		}
	}

	private static final String CHARGE   = "charge";
	private static final String COOLDOWN = "cooldown";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
		bundle.put(COOLDOWN, cooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		charge = bundle.getInt(CHARGE);
		cooldown = bundle.getFloat(COOLDOWN);
	}
}

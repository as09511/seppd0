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


package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ego;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.CombinedEgo;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//combined E.G.O (WAW, balanced as tier 5): the Echo crossbow and the Loneliness revolver, used in turns.
//Echo: 1 arrow that pierces every enemy in its path and paralyses each one it hits for 1 turn.
//Loneliness: 6 bullets fired two at a time, 1 in 6 to weaken for 3 turns like Loneliness, and shooting a
//paralysed enemy takes no time, once per turn.
//Emptying one swaps to the other, which comes loaded. Melee is the same as Echo.
public class EchoLoneliness extends EgoGun implements CombinedEgo {

	private static final int ECHO_AMMO       = 1;
	private static final int LONELINESS_AMMO = 6;
	//Loneliness fires this many bullets with each shot
	private static final int VOLLEY          = 2;

	public static final float PARALYSIS_DURATION = 1f;
	public static final float WEAKNESS_DURATION  = Loneliness.WEAKNESS_DURATION;

	{
		image = ItemSpriteSheet.ECHO_LONELINESS;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.WAW;
		damageType = DamageType.WHITE;
		tier = 5;
	}

	//false: Echo is in hand, true: Loneliness. Left at its default so the magazine set up by EgoGun matches it
	private boolean lonely;

	//melee is the same as Echo, which is tier 4
	@Override
	public int min(int lvl) {
		return  4 +             //4 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  20 +            //20 base
				5*lvl;          //+5 per level
	}

	@Override
	public int maxAmmo() {
		return lonely ? LONELINESS_AMMO : ECHO_AMMO;
	}

	//never reloaded by hand: emptying one weapon swaps to the other, which comes loaded
	@Override
	protected boolean canReload() {
		return false;
	}

	@Override
	public float reloadTime() {
		return 0;
	}

	//Echo 12-30 (+1/+6), Loneliness 3-7 (+1/+3) per bullet, as the two weapons on their own
	@Override
	public int shotMin(int lvl) {
		return lonely ? lonelinessMin(lvl) : echoMin(lvl);
	}

	@Override
	public int shotMax(int lvl) {
		return lonely ? lonelinessMax(lvl) : echoMax(lvl);
	}

	private static int echoMin(int lvl){
		return 12 + lvl;
	}

	private static int echoMax(int lvl){
		return 30 + 6*lvl;
	}

	private static int lonelinessMin(int lvl){
		return 3 + lvl;
	}

	private static int lonelinessMax(int lvl){
		return 7 + 3*lvl;
	}

	@Override
	protected boolean isBow() {
		return !lonely;
	}

	public boolean lonely(){
		return lonely;
	}

	@Override
	protected void fireShot(Hero hero, int target) {
		float delay = hero.attackDelay();
		if (lonely){
			//a paralysed target makes the volley free, once per turn
			Char aimed = Actor.findChar(new Ballistica(hero.pos, target, Ballistica.PROJECTILE).collisionPos);
			boolean free = aimed != null && aimed != hero && aimed.buff(Paralysis.class) != null
					&& hero.buff(FreeShotUsed.class) == null;
			if (free) Buff.affect(hero, FreeShotUsed.class, 1f);
			fireBullet(hero, target, VOLLEY, free ? 0 : delay);
		} else {
			fireArrow(hero, target, delay);
		}
	}

	//Echo: the arrow flies on to the first wall, through every character in its way
	private void fireArrow(Hero hero, int target, float delay){
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
		int end = path.collisionPos;
		fireSound();
		((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(hero.sprite, end, new ArrowVFX(), () -> {
			for (int cell : path.subPath(1, path.dist)){
				Char ch = Actor.findChar(cell);
				if (ch != null && ch != hero && ch.isAlive() && ch.alignment != Char.Alignment.ALLY){
					onShotHit(hero, ch, shoot(hero, ch));
				}
			}
			finishShot(hero, delay);
		});
	}

	//Loneliness: one bullet after another at the same cell, each stops at the first character it reaches
	private void fireBullet(Hero hero, int target, int left, float delay){
		int cell = new Ballistica(hero.pos, target, Ballistica.PROJECTILE).collisionPos;
		fireSound();
		MagicMissile.boltFromChar(hero.sprite.parent, boltType(), hero.sprite, cell, () -> {
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != hero){
				onShotHit(hero, ch, shoot(hero, ch));
			}
			if (left > 1 && ammo > 0 && hero.isAlive()){
				ammo--;
				updateQuickslot();
				fireBullet(hero, target, left - 1, delay);
			} else {
				finishShot(hero, delay);
			}
		});
	}

	@Override
	protected void onShotHit(Hero hero, Char ch, boolean hit) {
		if (hit && !lonely && ch.isAlive()){
			Buff.prolong(ch, Paralysis.class, PARALYSIS_DURATION);
		}
	}

	@Override
	protected int shotProc(Char attacker, Char defender, int damage) {
		if (lonely && Random.Int(6) == 0 && defender.isAlive()){
			Buff.prolong(defender, Weakness.class, WEAKNESS_DURATION);
		}
		return damage;
	}

	//once a magazine is empty the other weapon is taken up, already loaded
	private void finishShot(Hero hero, float delay){
		if (ammo <= 0){
			lonely = !lonely;
			ammo = maxAmmo();
			updateQuickslot();
			GLog.i(Messages.get(this, lonely ? "swap_loneliness" : "swap_echo"));
		}
		hero.spendAndNext(delay);
	}

	@Override
	public String status() {
		return ammo + "/" + maxAmmo();
	}

	@Override
	public String statsInfo() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return Messages.get(this, "stats_desc",
				augment.damageFactor(echoMin(lvl)), augment.damageFactor(echoMax(lvl)),
				augment.damageFactor(lonelinessMin(lvl)), augment.damageFactor(lonelinessMax(lvl)),
				damageTypeName(),
				Messages.get(this, lonely ? "mode_loneliness" : "mode_echo"));
	}

	private static final String LONELY = "lonely";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(LONELY, lonely);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		lonely = bundle.getBoolean(LONELY);
		super.restoreFromBundle(bundle);
	}

	//marks that a free Loneliness volley was already fired this turn
	public static class FreeShotUsed extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
		}
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{Echo.class, Loneliness.class};
			inQuantity = new int[]{1, 1};

			cost = 10;

			output = EchoLoneliness.class;
			outQuantity = 1;
		}

		@Override
		public Item brew( ArrayList<Item> ingredients ) {
			int level = 0;
			for (Item i : ingredients){
				if (i instanceof Echo || i instanceof Loneliness){
					level = Math.max(level, i.level());
				}
			}
			Item result = super.brew(ingredients);
			if (result != null){
				if (level > 0) result.upgrade(level);
				result.identify();
			}
			return result;
		}
	}
}

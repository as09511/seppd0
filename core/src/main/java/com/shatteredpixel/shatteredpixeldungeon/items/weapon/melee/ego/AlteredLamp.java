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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.CombinedEgo;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.CombinedEgoRecipe;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//altered E.G.O Lamp (WAW, balanced as tier 5): a crossbow made from Lamp and Diffraction.
//Melee deals WHITE like Diffraction, the single bolt deals BLACK like Lamp's eye. While equipped it gives
//mind vision of 2 tiles, or 1 more on top of any the hero already has. Like the assassin's blade a sneak
//attack rolls from halfway to max, and it can charm like Lamp (shared 10 turn cooldown): a charmed enemy is
//always caught off guard, and takes 25% more damage from it.
public class AlteredLamp extends EgoGun implements CombinedEgo {

	public static final String AC_CHARM = Lamp.AC_CHARM;

	private static final int MAX_AMMO = 1;
	private static final float RELOAD_TIME = 1f;

	private static final float CHARM_DURATION = 10f;
	private static final int CHARM_COOLDOWN = 10;
	private static final float CHARM_DAMAGE = 1.25f;

	public static final int MIND_VISION = 2;

	{
		image = ItemSpriteSheet.ALTERED_LAMP;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.WAW;
		damageType = DamageType.WHITE;
		tier = 5;
	}

	@Override
	public int min(int lvl) {
		return  5 +             //5 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  25 +            //25 base
				6*lvl;          //+6 per level
	}

	//refined like an ALEPH E.G.O, as it is balanced as tier 5
	@Override
	public RiskLevel costRisk() {
		return RiskLevel.ALEPH;
	}

	@Override
	public int maxAmmo() {
		return MAX_AMMO;
	}

	@Override
	public float reloadTime() {
		return RELOAD_TIME;
	}

	@Override
	public int shotMin(int lvl) {
		return  6 +             //6 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl) {
		return  30 +            //30 base
				6*lvl;          //+6 per level, like a crossbow
	}

	@Override
	protected DamageType shotDamageType() {
		return DamageType.BLACK;
	}

	@Override
	protected boolean isBow() {
		return true;
	}

	//mind vision while equipped: 2 tiles, or 1 more than the hero already has
	public static int mindVisionRange( Hero hero, int current ){
		if (!(hero.belongings.weapon() instanceof AlteredLamp)) return current;
		return current >= 1 ? current + 1 : MIND_VISION;
	}

	//an enemy the wielder charmed is always caught off guard, see Mob.surprisedBy
	public static boolean charmedBy( Char ch, Char attacker ){
		if (!(attacker instanceof Hero) || !(((Hero) attacker).belongings.weapon() instanceof AlteredLamp)) return false;
		Charm charm = ch.buff(Charm.class);
		return charm != null && charm.object == attacker.id();
	}

	//the assassin's blade: a sneak attack rolls from halfway to max, instead of min to max
	private int sneakRoll( Hero hero, int min, int max, int strReq ){
		int diff = max - min;
		int damage = augment.damageFactor(Hero.heroDamageIntRange(min + Math.round(diff*0.50f), max));
		int exStr = hero.STR() - strReq;
		if (exStr > 0) damage += Hero.heroDamageIntRange(0, exStr);
		return damage;
	}

	private static boolean sneaking( Hero hero ){
		Char enemy = hero.attackTarget();
		return enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero);
	}

	@Override
	public int damageRoll(Char owner) {
		if (owner instanceof Hero && sneaking((Hero) owner)){
			return sneakRoll((Hero) owner, min(), max(), STRReq());
		}
		return super.damageRoll(owner);
	}

	@Override
	protected int shotDamageRoll(Char owner, Shot shot) {
		if (owner instanceof Hero && sneaking((Hero) owner)){
			return sneakRoll((Hero) owner, shot.min(), shot.max(), STRReq());
		}
		return -1;
	}

	private static int charmBonus( Char attacker, Char defender, int damage ){
		Charm charm = defender.buff(Charm.class);
		if (charm != null && charm.object == attacker.id()){
			damage = Math.round(damage * CHARM_DAMAGE);
		}
		return damage;
	}

	@Override
	protected int egoProc(Char attacker, Char defender, int damage) {
		return charmBonus(attacker, defender, damage);
	}

	@Override
	protected int shotProc(Char attacker, Char defender, int damage) {
		return charmBonus(attacker, defender, damage);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)){
			actions.add(AC_CHARM);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_CHARM)){
			if (!isEquipped(hero)){
				GLog.w(Messages.get(this, "need_equip"));
			} else if (hero.buff(Lamp.CharmCooldown.class) != null){
				GLog.w(Messages.get(Lamp.class, "cooldown", hero.buff(Lamp.CharmCooldown.class).left()));
			} else {
				usesTargeting = true;
				GameScene.selectCell(charmer);
			}
		}
	}

	private final CellSelector.Listener charmer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char ch = Actor.findChar(cell);
			if (ch == null || ch == hero || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]){
				GLog.w(Messages.get(Lamp.class, "no_target"));
				return;
			}

			Buff.affect(ch, Charm.class, CHARM_DURATION).object = hero.id();
			ch.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
			Sample.INSTANCE.play(Assets.Sounds.CHARMS);
			Buff.affect(hero, Lamp.CharmCooldown.class).set(CHARM_COOLDOWN);
			updateQuickslot();

			hero.sprite.zap(cell);
			Invisibility.dispel();
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(Lamp.class, "charm_prompt");
		}
	};

	@Override
	public String statsInfo() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return Messages.get(this, "stats_desc", augment.damageFactor(shotMin(lvl)), augment.damageFactor(shotMax(lvl)),
				DamageType.BLACK.coloredName(), MIND_VISION, CHARM_COOLDOWN);
	}

	public static class Recipe extends CombinedEgoRecipe {

		{
			inputs =  new Class[]{Lamp.class, Diffraction.class};
			inQuantity = new int[]{1, 1};

			cost = 10;

			output = AlteredLamp.class;
			outQuantity = 1;
		}

	}
}

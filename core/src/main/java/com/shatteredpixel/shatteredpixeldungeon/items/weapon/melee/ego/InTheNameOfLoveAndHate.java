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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//WAW, a magic staff: every attack deals a random damage type.
//Its shots need no ammo, never miss, and heal the allies they pass through.
//While equipped it charges up the Arcana Slave, a thick laser.
public class InTheNameOfLoveAndHate extends EgoGun {

	public static final String AC_ARCANA = "ARCANA";

	public static final int MAX_CHARGE = 100;
	private static final int ABILITY_CHARGE = 25;
	private static final float WEAKNESS_DURATION = 10f;
	private static final int RANDOM_COLOR = 0xFF7EC8;

	private int arcana = 0;

	{
		image = ItemSpriteSheet.LOVE_AND_HATE;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.WAW;
		damageType = DamageType.RED;
		tier = 4;
	}

	@Override
	public int max(int lvl) {
		return  15 +            //15 base, down from 25
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int shotMin(int lvl) {
		return min(lvl);
	}

	@Override
	public int shotMax(int lvl) {
		return max(lvl);
	}

	@Override
	protected boolean usesAmmo() {
		return false;
	}

	@Override
	public int maxAmmo() {
		return 0;
	}

	@Override
	public float reloadTime() {
		return 0;
	}

	private static DamageType randomType(){
		return Random.element(DamageType.values());
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damageType = randomType();
		return super.proc(attacker, defender, damage);
	}

	@Override
	protected DamageType shotDamageType() {
		return randomType();
	}

	@Override
	protected String damageTypeName() {
		return DamageType.colored(Messages.get(this, "random_type"), RANDOM_COLOR);
	}

	//passes through allies, healing them, and always hits the first other character
	@Override
	protected void fireShot(Hero hero, int target) {
		float delay = hero.attackDelay();
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);

		ArrayList<Char> allies = new ArrayList<>();
		Char victim = null;
		int end = path.collisionPos;
		for (int cell : path.subPath(1, path.dist)){
			Char ch = Actor.findChar(cell);
			if (ch == null || ch == hero) continue;
			if (ch.alignment == Char.Alignment.ALLY){
				allies.add(ch);
			} else {
				victim = ch;
				end = cell;
				break;
			}
		}

		final Char hitChar = victim;
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.PINK_MISSILE, hero.sprite, end, new Callback() {
			@Override
			public void call() {
				int heal = 5 + 2*buffedLvl();
				for (Char ally : allies){
					healAttacker(ally, heal);
				}
				if (hitChar != null && hitChar.isAlive()){
					hero.belongings.thrownWeapon = new Shot(InTheNameOfLoveAndHate.this);
					hero.attack(hitChar, 1, 0, Char.INFINITE_ACCURACY);
					hero.belongings.thrownWeapon = null;
				}
				hero.spendAndNext(delay);
			}
		});
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		if (ch instanceof Hero){
			Buff.affect(ch, ArcanaCharger.class);
		}
	}

	public void gainCharge(int amount){
		if (arcana < MAX_CHARGE){
			arcana = Math.min(MAX_CHARGE, arcana + amount);
			updateQuickslot();
			ArcanaCharger charger = Dungeon.hero == null ? null : Dungeon.hero.buff(ArcanaCharger.class);
			if (charger != null) charger.refreshAction();
		}
	}

	@Override
	public String status() {
		return Integer.toString(arcana);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)){
			actions.add(AC_ARCANA);
		}
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_ARCANA)){
			if (!isEquipped(hero)){
				GLog.w(Messages.get(this, "need_equip"));
			} else if (arcana < MAX_CHARGE){
				GLog.w(Messages.get(this, "not_charged", arcana, MAX_CHARGE));
			} else {
				usesTargeting = true;
				GameScene.selectCell(arcanaAimer);
			}
		}
	}

	public int arcanaMin(int lvl){
		return 22 + 3*lvl;
	}

	public int arcanaMax(int lvl){
		return 33 + 8*lvl;
	}

	private final CellSelector.Listener arcanaAimer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell != null && cell != Dungeon.hero.pos && arcana >= MAX_CHARGE){
				arcanaSlave(Dungeon.hero, cell);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(InTheNameOfLoveAndHate.class, "arcana_prompt");
		}
	};

	//a laser 3 cells thick, in a straight line until it hits a wall
	private void arcanaSlave(Hero hero, int target){
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
		int width = Dungeon.level.width();
		int dx = target % width - hero.pos % width;
		int dy = target / width - hero.pos / width;
		int sx = (int)Math.signum(dx), sy = (int)Math.signum(dy);

		//the two side cells of each cell on the path
		int[][] sides;
		if (Math.abs(dx) > Math.abs(dy)){
			sides = new int[][]{{0, 1}, {0, -1}};
		} else if (Math.abs(dy) > Math.abs(dx)){
			sides = new int[][]{{1, 0}, {-1, 0}};
		} else {
			sides = new int[][]{{sx, 0}, {0, sy}};
		}

		LinkedHashSet<Integer> cells = new LinkedHashSet<>();
		for (int cell : path.subPath(1, path.dist)){
			if (Dungeon.level.solid[cell]) continue;
			cells.add(cell);
			int x = cell % width, y = cell / width;
			for (int[] side : sides){
				int nx = x + side[0], ny = y + side[1];
				if (nx < 0 || ny < 0 || nx >= width || ny >= Dungeon.level.height()) continue;
				int n = nx + ny*width;
				if (!Dungeon.level.solid[n]) cells.add(n);
			}
		}
		cells.remove(hero.pos);

		hero.sprite.zap(target);
		hero.sprite.parent.add(new Beam.ArcanaRay(hero.sprite.center(),
				DungeonTilemap.raisedTileCenterToWorld(path.collisionPos)));
		Sample.INSTANCE.play(Assets.Sounds.RAY);

		DamageType type = randomType();
		int lvl = buffedLvl();
		ArrayList<Char> targets = new ArrayList<>();
		for (int cell : cells){
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != hero && ch.alignment != Char.Alignment.ALLY){
				targets.add(ch);
			}
		}
		for (Char ch : targets){
			if (!ch.isAlive()) continue;
			int dmg = augment.damageFactor(Random.NormalIntRange(arcanaMin(lvl), arcanaMax(lvl)));
			dmg = DamageType.apply(ch, type, dmg);
			ch.damage(dmg, hero);
		}

		arcana = 0;
		updateQuickslot();
		ArcanaCharger charger = hero.buff(ArcanaCharger.class);
		if (charger != null) charger.refreshAction();
		Buff.prolong(hero, Weakness.class, WEAKNESS_DURATION);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public String statsInfo() {
		int lvl = levelKnown ? buffedLvl() : 0;
		return Messages.get(this, "stats_desc", augment.damageFactor(shotMin(lvl)), augment.damageFactor(shotMax(lvl)),
				5 + 2*lvl, MAX_CHARGE, augment.damageFactor(arcanaMin(lvl)), augment.damageFactor(arcanaMax(lvl)), (int)WEAKNESS_DURATION);
	}

	//duelist ability: charges up the Arcana Slave
	@Override
	protected void egoAbility(Hero hero, Integer target, int dmgBoost) {
		beforeAbilityUsed(hero, null);
		gainCharge(ABILITY_CHARGE);
		hero.sprite.operate(hero.pos);
		afterAbilityUsed(hero);
	}

	@Override
	public String abilityInfo() {
		return Messages.get(this, "ability_desc", ABILITY_CHARGE);
	}

	private static final String ARCANA = "arcana";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ARCANA, arcana);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		arcana = bundle.getInt(ARCANA);
	}

	//gives the equipped weapon 1 charge per turn, and goes away once it is no longer equipped
	//once fully charged, the Arcana Slave can also be fired from the action button
	public static class ArcanaCharger extends Buff implements ActionIndicator.Action {

		{
			type = buffType.NEUTRAL;
		}

		private InTheNameOfLoveAndHate weapon(){
			if (target instanceof Hero && ((Hero) target).belongings.weapon() instanceof InTheNameOfLoveAndHate){
				return (InTheNameOfLoveAndHate) ((Hero) target).belongings.weapon();
			}
			return null;
		}

		@Override
		public boolean act() {
			InTheNameOfLoveAndHate wep = weapon();
			if (wep != null){
				wep.gainCharge(1);
				refreshAction();
				spend(TICK);
			} else {
				detach();
			}
			return true;
		}

		//shows the button while fully charged, without taking it over from another class ability
		public void refreshAction(){
			InTheNameOfLoveAndHate wep = weapon();
			if (wep != null && wep.arcana >= MAX_CHARGE){
				if (ActionIndicator.action == null) ActionIndicator.setAction(this);
			} else {
				ActionIndicator.clearAction(this);
			}
		}

		@Override
		public void fx(boolean on) {
			if (on) refreshAction();
		}

		@Override
		public void detach() {
			super.detach();
			ActionIndicator.clearAction(this);
		}

		@Override
		public String actionName() {
			return Messages.get(InTheNameOfLoveAndHate.class, "ac_arcana");
		}

		@Override
		public Visual primaryVisual() {
			InTheNameOfLoveAndHate wep = weapon();
			if (wep == null) return new HeroIcon(this);
			return new ItemSprite(wep);
		}

		@Override
		public int indicatorColor() {
			return 0xC8327E;
		}

		@Override
		public void doAction() {
			InTheNameOfLoveAndHate wep = weapon();
			if (wep != null){
				wep.execute((Hero) target, AC_ARCANA);
			}
		}
	}

}

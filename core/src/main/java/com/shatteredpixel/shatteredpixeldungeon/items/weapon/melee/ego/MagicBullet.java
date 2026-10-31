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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.DamageType;
import com.shatteredpixel.shatteredpixeldungeon.lobotomy.RiskLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//WAW
//A gun: weak melee attacks, plus 7 bullets that pierce through every character and door in their path.
//The 7th bullet always hits a random character on the floor instead, possibly even the wielder.
public class MagicBullet extends EgoGun {

	private static final int MAX_AMMO = 7;
	private static final float RELOAD_TIME = 3f;
	//the 7th bullet hits this many cells to either side of its random target
	private static final int FINAL_SHOT_RADIUS = 5;

	{
		image = ItemSpriteSheet.MAGIC_BULLET;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;

		risk = RiskLevel.WAW;
		damageType = DamageType.BLACK;
		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return  4 +             //4 base
				lvl;            //+1 per level
	}

	@Override
	public int max(int lvl) {
		return  15 +            //15 base, down from 25
				4*lvl;          //+4 per level, down from +5
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
	public int shotMin(int lvl){
		return  6 +             //6 base
				lvl;            //+1 per level
	}

	@Override
	public int shotMax(int lvl){
		return  22 +            //22 base
				5*lvl;          //+5 per level
	}

	@Override
	protected void fireShot(Hero hero, int target){
		//shooting is affected by attack speed, just like melee attacks
		float delay = hero.attackDelay();
		Sample.INSTANCE.play(Assets.Sounds.RAY);

		if (ammo <= 0){
			finalShot(hero);
			//the gun is instantly reloaded after the 7th bullet
			ammo = MAX_AMMO;
			updateQuickslot();
		} else {
			normalShot(hero, target);
		}

		hero.spendAndNext(delay);
	}

	//the ray sound of the shot is all that is heard
	@Override
	protected void shotHitSound(float pitch) {
	}

	//pierces every character and door until it hits a wall or other obstacle
	private void normalShot(Hero hero, int target){
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);

		hero.sprite.parent.add(new Beam.DeathRay(hero.sprite.center(),
				DungeonTilemap.raisedTileCenterToWorld(path.collisionPos)));

		ArrayList<Char> targets = new ArrayList<>();
		boolean terrainAffected = false;
		for (int cell : path.subPath(1, path.dist)){
			int terr = Dungeon.level.map[cell];
			if (terr == Terrain.DOOR || terr == Terrain.OPEN_DOOR){
				Dungeon.level.destroy(cell);
				GameScene.updateMap(cell);
				terrainAffected = true;
			}
			Char ch = Actor.findChar(cell);
			if (ch != null) targets.add(ch);
		}

		for (Char ch : targets){
			hitChar(hero, ch);
		}

		if (terrainAffected){
			Dungeon.observe();
		}
	}

	//hits everything within 5 cells left and right of a random character on the floor, ignoring walls
	private void finalShot(Hero hero){
		ArrayList<Char> candidates = new ArrayList<>(Actor.chars());
		Char victim = Random.element(candidates);
		if (victim == null) return;

		int width = Dungeon.level.width();
		int x = victim.pos % width;
		int rowStart = victim.pos - x;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int dx = -FINAL_SHOT_RADIUS; dx <= FINAL_SHOT_RADIUS; dx++){
			if (x + dx >= 0 && x + dx < width){
				cells.add(rowStart + x + dx);
			}
		}

		hero.sprite.parent.add(new Beam.DeathRay(
				DungeonTilemap.raisedTileCenterToWorld(cells.get(0)),
				DungeonTilemap.raisedTileCenterToWorld(cells.get(cells.size()-1))));
		GLog.n(Messages.get(this, "final_shot"));

		ArrayList<Char> targets = new ArrayList<>();
		for (int cell : cells){
			Char ch = Actor.findChar(cell);
			if (ch != null) targets.add(ch);
		}
		for (Char ch : targets){
			hitChar(hero, ch);
		}

		//lights up exactly the cells that were hit
		if (hero.isAlive()) {
			Buff.affect(hero, Flare.class, hero.attackDelay() + 1f).set(cells);
			Dungeon.observe();
		}
	}

	private void hitChar(Hero hero, Char ch){
		Shot shot = new Shot(this);
		if (ch == hero){
			int dmg = Math.max(0, shot.damageRoll(hero) - hero.drRoll());
			dmg = DamageType.apply(hero, DamageType.BLACK, dmg);
			hero.damage(dmg, this);
			if (!hero.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(this, "ondeath"));
			}
		} else if (ch.isAlive()) {
			//magic bullets never miss
			hero.belongings.thrownWeapon = shot;
			hero.attack(ch, 1, 0, Char.INFINITE_ACCURACY);
			hero.belongings.thrownWeapon = null;
		}
	}

	//the quote always goes at the very bottom of the description
	@Override
	public String info() {
		return super.info() + "\n\n" + Messages.get(this, "quote");
	}

	//reveals the cells hit by the 7th bullet, see Level.updateFieldOfView and Dungeon.observe
	public static class Flare extends FlavourBuff {

		public int[] cells = new int[0];
		public int depth, branch;

		public void set(ArrayList<Integer> cells){
			this.cells = new int[cells.size()];
			for (int i = 0; i < cells.size(); i++){
				this.cells[i] = cells.get(i);
			}
			depth = Dungeon.depth;
			branch = Dungeon.branch;
		}

		@Override
		public void detach() {
			super.detach();
			//recalculate FOV right away so the cells go dark even if the hero is resting
			if (target == Dungeon.hero && Dungeon.level != null){
				Dungeon.observe();
			}
			for (int cell : cells){
				GameScene.updateFog(cell, 1);
			}
		}

		private static final String CELLS = "cells";
		private static final String DEPTH = "depth";
		private static final String BRANCH = "branch";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(CELLS, cells);
			bundle.put(DEPTH, depth);
			bundle.put(BRANCH, branch);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			cells = bundle.getIntArray(CELLS);
			depth = bundle.getInt(DEPTH);
			branch = bundle.getInt(BRANCH);
		}
	}
}

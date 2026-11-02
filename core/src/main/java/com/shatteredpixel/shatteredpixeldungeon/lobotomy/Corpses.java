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


package com.shatteredpixel.shatteredpixeldungeon.lobotomy;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.BlobEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ego.LaughterArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.SparseArray;

//the bodies enemies leave behind when they die. They stay on the floor until eaten,
//which only someone wearing Laughter's armor can do, by examining a body next to them.
public class Corpses extends Blob {

	public static final int HEAL = 10;

	{
		//once seen, bodies stay drawn under the fog like items do
		alwaysVisible = true;
	}

	//bodies never spread or fade
	@Override
	protected void evolve() {
		int cell;
		for (int i = area.top-1; i <= area.bottom; i++) {
			for (int j = area.left-1; j <= area.right; j++) {
				cell = j + i*Dungeon.level.width();
				if (Dungeon.level.insideMap(cell)) {
					off[cell] = cur[cell];
					volume += off[cell];
				}
			}
		}
	}

	//enemies with flesh leave a body where they die, unless they fall or die inside a wall
	public static void leaveCorpse( Mob mob ){
		if (Dungeon.level == null || mob.alignment != Char.Alignment.ENEMY) return;
		if (Char.hasProp(mob, Char.Property.INORGANIC)) return;
		int pos = mob.pos;
		if (!Dungeon.level.insideMap(pos) || Dungeon.level.pit[pos] || Dungeon.level.solid[pos]) return;
		if (at(pos)) return;
		GameScene.add(Blob.seed(pos, 1, Corpses.class));
	}

	public static boolean at( int cell ){
		Corpses c = Dungeon.level == null ? null : (Corpses) Dungeon.level.blobs.get(Corpses.class);
		return c != null && c.volume > 0 && cell >= 0 && cell < c.cur.length && c.cur[cell] > 0;
	}

	//called when the hero examines a cell, returns true if she ate the body there
	public static boolean tryEat( Hero hero, int cell ){
		if (!at(cell) || !(hero.belongings.armor() instanceof LaughterArmor)) return false;
		if (cell != hero.pos && !Dungeon.level.adjacent(cell, hero.pos)){
			GLog.w(Messages.get(Corpses.class, "too_far"));
			return false;
		}

		Dungeon.level.blobs.get(Corpses.class).clear(cell);

		hero.sprite.operate(cell);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		GLog.i(Messages.get(Corpses.class, "eat"));
		int heal = Math.min(HEAL, hero.HT - hero.HP);
		if (heal > 0){
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	public String tileDesc() {
		if (Dungeon.hero != null && Dungeon.hero.belongings.armor() instanceof LaughterArmor){
			return Messages.get(this, "desc_laughter");
		}
		return Messages.get(this, "desc");
	}

	//the bodies are drawn as still images on the floor, not as particles
	@Override
	public void use( BlobEmitter emitter ) {
		super.use(emitter);
		emitter.add(new Bodies(this));
	}

	private static class Bodies extends Group {

		private final Corpses blob;
		private final SparseArray<Image> images = new SparseArray<>();

		Bodies( Corpses blob ){
			this.blob = blob;
		}

		@Override
		public void update() {
			super.update();
			if (Dungeon.level == null || blob.cur == null) return;

			//remove the bodies that were eaten
			for (int cell : images.keyArray()){
				if (blob.cur[cell] <= 0){
					images.get(cell).killAndErase();
					images.remove(cell);
				}
			}

			//add new ones, once the cell has been seen
			if (blob.volume <= 0) return;
			int w = Dungeon.level.width();
			for (int i = blob.area.left; i < blob.area.right; i++) {
				for (int j = blob.area.top; j < blob.area.bottom; j++) {
					int cell = i + j*w;
					if (blob.cur[cell] > 0 && images.get(cell) == null
							&& (Dungeon.level.heroFOV[cell] || Dungeon.level.visited[cell] || Dungeon.level.mapped[cell])){
						Image body = new Image(Assets.Sprites.ITEMS);
						body.frame(ItemSpriteSheet.film.get(ItemSpriteSheet.CORPSE));
						PointF p = DungeonTilemap.tileToWorld(cell);
						body.x = p.x;
						body.y = p.y;
						add(body);
						images.put(cell, body);
					}
				}
			}
		}
	}
}

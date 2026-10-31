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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

//Sefirot Pixel Dungeon's menu background: a dark crimson gradient with the qliphoth tree
// (the tree of the sephirot, inverted and corrupted) glowing behind it
public class SefirotBackground extends Group {

	private static final int TREE_HEIGHT = 168;

	//shared so the glow keeps its rhythm when moving between menus
	private static float time = 0;

	public SefirotBackground( int width, int height ){
		super();

		Image gradient = new Image(TextureCache.createGradient(0xFF2A1218, 0xFF1C0C11, 0xFF12080B, 0xFF0A0507, 0xFF050304));
		gradient.angle = 90;
		gradient.x = width;
		gradient.scale.x = height/5f;
		gradient.scale.y = width;
		add(gradient);

		float scale = height * 0.92f / TREE_HEIGHT;
		if (scale >= 1) scale = (float)Math.floor(scale);

		Image tree = new Image(Assets.Splashes.Title.QLIPHOTH_TREE);
		tree.scale.set(scale);
		tree.x = (width - tree.width()) / 2f;
		tree.y = (height - tree.height()) / 2f;
		tree.alpha(0.55f);
		add(tree);

		Image glow = new Image(Assets.Splashes.Title.QLIPHOTH_GLOW){
			@Override
			public void update() {
				super.update();
				time += Game.elapsed;
				am = 0.3f + 0.2f*(float)Math.sin(time * 1.2f);
			}

			@Override
			public void draw() {
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		};
		glow.scale.set(scale);
		glow.x = tree.x;
		glow.y = tree.y;
		add(glow);
	}
}

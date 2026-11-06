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


package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.tiles.CustomTilemap;
import com.watabou.noosa.Tilemap;
import com.watabou.noosa.audio.Music;

import java.util.ArrayList;
import java.util.Arrays;

//L Corp headquarters (L사 본사), a boss arena modelled on the qliphoth tree: a 3x3 grid of 6x6 rooms joined by 2 wide, 8 long corridors,
//plus Malkuth's room above the top middle one, where the hero arrives.
//Each room's floor has its sephira's colour, laid out like the tree turned upside down:
//           Malkuth
//   Netzach  Yesod     Hod
//   Chesed  Tiphereth  Gebura
//   Hokma    Keter     Binah
public class LCorpHQLevel extends Level {

	public static final int ROOM = 6;
	public static final int CORRIDOR = 8;
	public static final int CORRIDOR_WIDTH = 2;
	private static final int STEP = ROOM + CORRIDOR;

	//index of each sephira's floor tile in the texture, also used as the room ids
	public static final int KETER = 0, HOKMA = 1, BINAH = 2, CHESED = 3, GEBURA = 4,
			TIPHERETH = 5, NETZACH = 6, HOD = 7, YESOD = 8, MALKUTH = 9;

	//the 3x3 grid below Malkuth, by row
	private static final int[][] GRID = {
			{NETZACH, YESOD,     HOD},
			{CHESED,  TIPHERETH, GEBURA},
			{HOKMA,   KETER,     BINAH}
	};

	//one wall tile of border around everything
	private static final int WIDTH = 1 + 3*ROOM + 2*CORRIDOR + 1;
	private static final int HEIGHT = 1 + 4*ROOM + 3*CORRIDOR + 1;

	{
		color1 = 0x801500;
		color2 = 0xa68521;

		viewDistance = 8;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_CITY;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_CITY;
	}

	//top left floor cell of a room in grid coordinates, row -1 is Malkuth's room
	private static int roomX(int col){
		return 1 + col*STEP;
	}

	private static int roomY(int row){
		return 1 + (row+1)*STEP;
	}

	//which sephira's room a cell belongs to, or -1 for corridors and walls
	public int roomAt(int cell){
		int x = cell % width(), y = cell / width();
		if (inRoom(x, y, 1, -1)) return MALKUTH;
		for (int row = 0; row < 3; row++){
			for (int col = 0; col < 3; col++){
				if (inRoom(x, y, col, row)) return GRID[row][col];
			}
		}
		return -1;
	}

	private static boolean inRoom(int x, int y, int col, int row){
		int rx = roomX(col), ry = roomY(row);
		return x >= rx && x < rx + ROOM && y >= ry && y < ry + ROOM;
	}

	@Override
	protected boolean build() {

		setSize(WIDTH, HEIGHT);
		Arrays.fill(map, Terrain.WALL);

		//rooms
		carve(roomX(1), roomY(-1), ROOM, ROOM);
		for (int row = 0; row < 3; row++){
			for (int col = 0; col < 3; col++){
				carve(roomX(col), roomY(row), ROOM, ROOM);
			}
		}

		//corridors, centred on the sides of the rooms they join
		for (int[] c : corridors()){
			carve(c[0], c[1], c[2], c[3]);
		}

		//the hero arrives at the top of Malkuth's room
		int offset = (ROOM - CORRIDOR_WIDTH)/2;
		int entrance = roomX(1) + offset + roomY(-1)*width();
		map[entrance] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this,
				entrance,
				LevelTransition.Type.BRANCH_ENTRANCE,
				Dungeon.depth,
				0,
				LevelTransition.Type.REGULAR_ENTRANCE));

		CustomTilemap floor = new SephiraFloors();
		floor.setRect(0, 0, width(), height());
		customTiles.add(floor);

		return true;
	}

	private static ArrayList<int[]> corridors;

	//every corridor as {x, y, width, height} of its floor: the one below Malkuth's room, then those of the grid
	public static ArrayList<int[]> corridors(){
		if (corridors == null){
			corridors = new ArrayList<>();
			int offset = (ROOM - CORRIDOR_WIDTH)/2;
			corridors.add(new int[]{roomX(1) + offset, roomY(-1) + ROOM, CORRIDOR_WIDTH, CORRIDOR});
			for (int row = 0; row < 3; row++){
				for (int col = 0; col < 3; col++){
					if (col < 2) corridors.add(new int[]{roomX(col) + ROOM, roomY(row) + offset, CORRIDOR, CORRIDOR_WIDTH});
					if (row < 2) corridors.add(new int[]{roomX(col) + offset, roomY(row) + ROOM, CORRIDOR_WIDTH, CORRIDOR});
				}
			}
		}
		return corridors;
	}

	private void carve(int x, int y, int w, int h){
		for (int j = y; j < y + h; j++){
			for (int i = x; i < x + w; i++){
				map[i + j*width()] = Terrain.EMPTY;
			}
		}
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return entrance() + width();
	}

	//each room's floor in its sephira's colour, corridors keep the regular floor
	public static class SephiraFloors extends CustomTilemap {

		{
			texture = Assets.Environment.QLIPHOTH_FLOOR;
		}

		@Override
		public Tilemap create() {
			Tilemap v = super.create();
			LCorpHQLevel level = (LCorpHQLevel) Dungeon.level;
			int[] data = new int[tileW*tileH];
			for (int i = 0; i < data.length; i++){
				int cell = (tileX + i % tileW) + (tileY + i / tileW) * level.width();
				data[i] = level.map[cell] == Terrain.EMPTY ? level.roomAt(cell) : -1;
			}
			v.map(data, tileW);
			return v;
		}

		@Override
		public String name(int tileX, int tileY) {
			int room = ((LCorpHQLevel) Dungeon.level).roomAt(tileX + this.tileX + (tileY + this.tileY) * Dungeon.level.width());
			return room == -1 ? null : Messages.get(LCorpHQLevel.class, "room_" + room);
		}
	}
}

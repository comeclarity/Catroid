/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2015 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.sensing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;

import org.catrobat.catroid.common.Constants;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.utils.ImageEditing;

import java.util.ArrayList;

import ar.com.hjg.pngj.PngjInputException;

public class CollisionDetection {

	public static boolean intersectPolygons(Polygon first, Polygon second) {
		int first_length = first.getTransformedVertices().length;
		for (int first_index = 0; first_index < first_length; first_index += 2) {
			float x1 = first.getTransformedVertices()[first_index];
			float y1 = first.getTransformedVertices()[first_index + 1];
			float x2 = first.getTransformedVertices()[(first_index + 2) % first_length];
			float y2 = first.getTransformedVertices()[(first_index + 3) % first_length];

			if (Intersector.intersectSegmentPolygon(new Vector2(x1, y1), new Vector2(x2, y2), second)) {
				return true;
			}
		}

		return false;
	}

	public static boolean  checkCollisionForPolygonsInPolygons(Polygon[] first, Polygon[] second)
	{
		for (Polygon p_first : first) {
			int contained_in = 0;
			for (Polygon p_second : second) {
				if (p_second.contains(p_first.getTransformedVertices()[0], p_first.getTransformedVertices()[1])) {
					contained_in++;
				}
			}
			if (contained_in % 2 != 0) return true;
		}

		for (Polygon p_second : second) {
			int contained_in = 0;
			for (Polygon p_first : first) {
				if (p_first.contains(p_second.getTransformedVertices()[0], p_second.getTransformedVertices()[1])) {
					contained_in++;
				}
			}
			if (contained_in % 2 != 0) return true;
		}
		return false;
	}

	public static boolean checkCollisionBetweenPolygons(Polygon[] first, Polygon[] second) {
		for (Polygon p1 : first){
			for (Polygon p2 : second) {
				if (intersectPolygons(p1, p2)) {
					return true;
				}
			}
		}
		if(checkCollisionForPolygonsInPolygons(first,second))
			return true;

		return false;
	}

	public static double checkCollisionBetweenLooks(Look firstLook, Look secondLook) {
		if(!firstLook.isVisible() || !secondLook.isVisible())
			return 0d;
		if(!firstLook.getHitbox().overlaps(secondLook.getHitbox()))
			return 0d;

		boolean colliding = checkCollisionBetweenPolygons(firstLook.getCurrentCollisionPolygon(),
													      secondLook.getCurrentCollisionPolygon());

		return colliding ? 1d : 0d;

	}

	public static boolean[][] createCollisionGrid(Bitmap bitmap, int grid_width, int grid_height) {
		boolean[][] grid = new boolean[grid_width][grid_height];
		for (int x_grid = 0; x_grid < grid_width; x_grid++) {
			for (int y_grid = 0; y_grid < grid_height; y_grid++) {
				for (int x_tile = x_grid * Constants.COLLISION_GRID_SIZE;
						x_tile < (x_grid + 1) * Constants.COLLISION_GRID_SIZE; x_tile++) {
					if (grid[x_grid][y_grid]) {
						break;
					}
					for (int y_tile = y_grid * Constants.COLLISION_GRID_SIZE;
							y_tile < (y_grid + 1) * Constants.COLLISION_GRID_SIZE; y_tile++) {
						if (bitmap.getPixel(x_tile, y_tile) != 0) {
							grid[x_grid][y_grid] = true;
							break;
						}
					}
				}
			}
		}
		//CollisionDetection.printCollisionGrid(grid, grid_width, grid_height);
		return grid;
	}

	public static ArrayList<CollisionPolygonVertex> createHorizontalVertices(boolean[][] grid, int grid_width, int
			grid_height) {
		ArrayList<CollisionPolygonVertex> horizontal = new ArrayList<CollisionPolygonVertex>();
		for (int y = 0; y < grid_height; y++) {
			for (int x = 0; x < grid_width; x++) {
				if (grid[x][y]) {

					boolean top_or_top_edge = y == 0 || !grid[x][y - 1];
					if (top_or_top_edge) {
						boolean extend_previous = horizontal.size() > 0 &&
								horizontal.get(horizontal.size() - 1).end_x == x &&
								horizontal.get(horizontal.size() - 1).end_y == y;
						boolean extend_previous_other_side = horizontal.size() > 1 &&
								horizontal.get(horizontal.size() - 2).end_x == x &&
								horizontal.get(horizontal.size() - 2).end_y == y;

						if (extend_previous) {
							horizontal.get(horizontal.size() - 1).extend(x + 1, y);
						} else if (extend_previous_other_side) {
							horizontal.get(horizontal.size() - 2).extend(x + 1,
									horizontal.get(horizontal.size() - 2).end_y);
						} else{
							horizontal.add(new CollisionPolygonVertex(x, y, x + 1, y));
						}
					}

					boolean bottom_or_bottom_edge = y == grid_height - 1 || !grid[x][y + 1];
					if (bottom_or_bottom_edge) {
						boolean extend_previous = horizontal.size() > 0 &&
								horizontal.get(horizontal.size() - 1).end_x == x &&
								horizontal.get(horizontal.size() - 1).end_y == y + 1;
						boolean extend_previous_other_side = horizontal.size() > 1 &&
								horizontal.get(horizontal.size() - 2).end_x == x &&
								horizontal.get(horizontal.size() - 2).end_y == y + 1;

						if (extend_previous) {
							horizontal.get(horizontal.size() - 1).extend(x + 1, y + 1);
						} else if (extend_previous_other_side) {
							horizontal.get(horizontal.size() - 2).extend(x + 1,
									horizontal.get(horizontal.size() - 2).end_y);
						} else {
							horizontal.add(new CollisionPolygonVertex(x, y + 1, x + 1, y + 1));
						}
					}
				}
			}
		}
		return horizontal;
	}

	public static ArrayList<CollisionPolygonVertex> createVerticalVertices(boolean[][] grid, int grid_width, int
			grid_height) {
		ArrayList<CollisionPolygonVertex> vertical = new ArrayList<CollisionPolygonVertex>();
		for (int x = 0; x < grid_width; x++) {
			for (int y = 0; y < grid_height; y++) {
				if (grid[x][y]) {

					boolean left_or_left_edge = x == 0 || !grid[x - 1][y];
					if (left_or_left_edge) {
						boolean extend_previous = vertical.size() > 0 &&
								vertical.get(vertical.size() - 1).end_x == x &&
								vertical.get(vertical.size() - 1).end_y == y;
						boolean extend_previous_other_side = vertical.size() > 1 &&
								vertical.get(vertical.size() - 2).end_x == x &&
								vertical.get(vertical.size() - 2).end_y == y;

						if (extend_previous) {
							vertical.get(vertical.size() - 1).extend(vertical.get(vertical.size() - 1).end_x, y + 1);
						} else if (extend_previous_other_side) {
							vertical.get(vertical.size() - 2).extend(vertical.get(vertical.size() - 2).end_x, y + 1);
						} else {
							vertical.add(new CollisionPolygonVertex(x, y, x, y + 1));
						}
					}

					boolean right_or_right_edge = x == grid_width - 1 || !grid[x + 1][y];
					if (right_or_right_edge) {
						boolean extend_previous = vertical.size() > 0 &&
								vertical.get(vertical.size() - 1).end_x == x + 1 &&
								vertical.get(vertical.size() - 1).end_y == y;
						boolean extend_previous_other_side = vertical.size() > 1 &&
								vertical.get(vertical.size() - 2).end_x == x + 1 &&
								vertical.get(vertical.size() - 2).end_y == y;

						if (extend_previous) {
							vertical.get(vertical.size() - 1).extend(vertical.get(vertical.size() - 1).end_x, y + 1);
						} else if (extend_previous_other_side) {
							vertical.get(vertical.size() - 2).extend(vertical.get(vertical.size() - 2).end_x, y + 1);
						} else {
							vertical.add(new CollisionPolygonVertex(x + 1, y, x + 1, y + 1));
						}
					}
				}
			}
		}
		return vertical;
	}

		private static float pointToLineDistance(PointF line_start, PointF line_end, PointF point) {
    	float normalLength = (float)Math.sqrt((line_end.x-line_start.x)*(line_end.x-line_start.x)+
										(line_end.y-line_start.y)*(line_end.y-line_start.y));
    	return Math.abs((point.x-line_start.x)*(line_end.y-line_start.y)-
						(point.y- line_start.y)*(line_end.x-line_start.x))/normalLength;
  	}

	public static ArrayList<PointF> smoothPolygon(ArrayList<PointF> points, int start, int end, float epsilon) {
		//Ramer-Douglas-Peucker Algorithm
		float dmax = 0f;
		int index = start;

		for (int i = index + 1; i < end; ++i) {
			float d = pointToLineDistance(points.get(start), points.get(end), points.get(i));
			if (d > dmax) {
				index = i;
				dmax = d;
			}
		}

		ArrayList<PointF> finalRes = new ArrayList<>();
		if (dmax > epsilon) {
			ArrayList<PointF> res1 = smoothPolygon(points, start, index, epsilon);
			ArrayList<PointF> res2 = smoothPolygon(points, index, end, epsilon);

			for (int i = 0; i < res1.size()-1; i++) {
				finalRes.add(res1.get(i));
			}
			for (int i = 0; i < res2.size(); i++) {
				finalRes.add(res2.get(i));
			}

		}
		else {
			finalRes.add(points.get(start));
			finalRes.add(points.get(end));
		}
		return finalRes;
	}

	public static ArrayList<PointF> getPointsFromPolygon(ArrayList<CollisionPolygonVertex> polygon) {
		ArrayList<PointF> points = new ArrayList<>();
		for (CollisionPolygonVertex vertex : polygon) {
			points.add(vertex.getStartPoint());
		}
		return points;
	}

	public static Polygon createPolygonFromPoints(ArrayList<PointF> points) {
		float[] polygon_nodes = new float[points.size() * 2];
		for (int node = 0; node < points.size(); node++) {
			polygon_nodes[node*2] = points.get(node).x;
			polygon_nodes[node*2+1] = points.get(node).y;
		}
		return new Polygon(polygon_nodes);
	}

	public static ArrayList<PointF> fitToGridSize(ArrayList<PointF> points) {
		ArrayList<PointF> scaled = new ArrayList<>();

		for (PointF point : points) {
			PointF p = new PointF(point.x * Constants.COLLISION_GRID_SIZE, point.y * Constants.COLLISION_GRID_SIZE);
			scaled.add(p);
		}

		return scaled;
	}

	public static ArrayList<ArrayList<CollisionPolygonVertex>> createBoundingPolygon(String absoluteBitmapPath,
			LookData lookData) {
		Bitmap bitmap = BitmapFactory.decodeFile(absoluteBitmapPath);
		if(bitmap == null)
		{
			Log.i("CollisionDetection", "bitmap " + absoluteBitmapPath + " is null. Cannot create Collision polygon");
			return null;
		}

		Matrix matrix = new Matrix();
		matrix.preScale(1.0f, -1.0f);
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

		int grid_width = bitmap.getWidth()/Constants.COLLISION_GRID_SIZE;
		int grid_height = bitmap.getHeight()/Constants.COLLISION_GRID_SIZE;
		boolean[][] grid = CollisionDetection.createCollisionGrid(bitmap, grid_width, grid_height);

		if(lookData.isCalculationThreadCancelled)
			return null;
		ArrayList<CollisionPolygonVertex> vertical = CollisionDetection.createVerticalVertices(grid, grid_width,
				grid_height);
		if(lookData.isCalculationThreadCancelled)
			return null;
		ArrayList<CollisionPolygonVertex> horizontal = CollisionDetection.createHorizontalVertices(grid, grid_width,
				grid_height);

		ArrayList<ArrayList<CollisionPolygonVertex>> final_vertices = new ArrayList<>();
		final_vertices.add(new ArrayList<CollisionPolygonVertex>());
		int polygon_number = 0;

		final_vertices.get(polygon_number).add(vertical.get(0));
		vertical.remove(0);

		do {

			if(lookData.isCalculationThreadCancelled)
				return null;
			CollisionPolygonVertex end = final_vertices.get(polygon_number).get(final_vertices.get(polygon_number)
					.size() - 1);
			boolean found = false;
			for (int h_i = 0; h_i < horizontal.size(); h_i++) {
				if (end.isConnected(horizontal.get(h_i))) {
					final_vertices.get(polygon_number).add(horizontal.get(h_i));
					horizontal.remove(h_i);
					found = true;
					break;
				} else if (end.isConnectedBackwards(horizontal.get(h_i))) {
					CollisionPolygonVertex to_add = horizontal.get(h_i);
					to_add.flip();
					final_vertices.get(polygon_number).add(to_add);
					horizontal.remove(h_i);
					found = true;
					break;
				}
			}

			if(found)
				end = final_vertices.get(polygon_number).get(final_vertices.get(polygon_number).size() - 1);

			for (int v_i = 0; v_i < vertical.size(); v_i++) {
				if (end.isConnected(vertical.get(v_i))) {
					final_vertices.get(polygon_number).add(vertical.get(v_i));
					vertical.remove(v_i);
					found = true;
					break;
				} else if (end.isConnectedBackwards(vertical.get(v_i))) {
					CollisionPolygonVertex to_add = vertical.get(v_i);
					to_add.flip();
					final_vertices.get(polygon_number).add(to_add);
					vertical.remove(v_i);
					found = true;
					break;
				}
			}
			if(!found)
			{
				polygon_number++;
				final_vertices.add(new ArrayList<CollisionPolygonVertex>());
				final_vertices.get(polygon_number).add(vertical.get(0));
				vertical.remove(0);
			}

		} while (horizontal.size() > 0);

		return final_vertices;
	}

	public static void writeCollisionVerticesToPNGMeta(Polygon[] collisionPolygon,
			String absolutePath) {
		String metaToWrite = "";
		for (Polygon polygon : collisionPolygon) {
			for (int f = 0; f < polygon.getVertices().length; f++) {
				metaToWrite += String.valueOf(polygon.getVertices()[f]) + ";";
			}
			metaToWrite = metaToWrite.substring(0, metaToWrite.length()-1);
			metaToWrite += "|";
		}
		metaToWrite = metaToWrite.substring(0, metaToWrite.length()-1);
		ImageEditing.writeMetaDataStringToPNG(absolutePath, Constants.COLLISION_META_TAG_KEY, metaToWrite);

	}

	public static Polygon[] getCollisionPolygonFromPNGMeta(String absolutePath) {
		//TODO: make failsafe (manipulated metadata), maybe already done by try catch, but still to think through
		String metadata = null;
		try
		{
			metadata = ImageEditing.readMetaDataStringFromPNG(absolutePath, Constants.COLLISION_META_TAG_KEY);
		}
		catch (PngjInputException e)
		{
			System.out.println("error at  " + absolutePath + ", creating new polygon");
			return null;
		}
		if (metadata == null) return null;

		String[] polygonStrings = metadata.split("\\|");
		Polygon[] collisionPolygon = new Polygon[polygonStrings.length];
		for (int polygonString = 0; polygonString < polygonStrings.length; polygonString++) {
			String[] pointStrings = polygonStrings[polygonString].split(";");
			float[] points = new float[pointStrings.length];
			for (int pointString = 0; pointString < pointStrings.length; pointString++) {
				points[pointString] = Float.valueOf(pointStrings[pointString]);
			}
			collisionPolygon[polygonString] = new Polygon(points);
		}

		return collisionPolygon;
	}



	public static void printDebugVertices(Polygon p) {
		Log.d("DebugVertices", "New Polygon " + p.getVertices().length/2);
		float[] v = p.getVertices();
		for (int i = 0; i < v.length; i+=2) {
			//Log.d("DebugVertices", i + " / " + v.length);
			if (i+1 != v.length-1) {
				Log.d("DebugVertices", "strecke(" + v[i] + "|" + v[i + 1] + " " + v[i + 2] + "|" + v[i + 3] + ")");
			} else {
				Log.d("DebugVertices", "strecke(" + v[i] + "|" + v[i + 1] + " " + v[0] + "|" + v[1] + ")");
			}
		}
	}

	public static void printCollisionPolygonVertices(ArrayList<CollisionPolygonVertex> horizontal,
			ArrayList<CollisionPolygonVertex> vertical) {
				for (CollisionPolygonVertex v : horizontal) {
			Log.d("Horizontal Vertex", v.toString());
		}
		for (CollisionPolygonVertex v : vertical) {
			Log.d("Vertical Vertex", v.toString());
		}
	}

	public static void printCollisionGrid(boolean[][] grid, int width, int height) {
		for (int y = 0; y < height; y++) {
			String line = "";
			for (int x = 0; x < width; x++) {
				if (grid[x][y]) {
					line += "O ";
				} else {
					line += ". ";
				}
			}
			Log.d("CollisionGrid", line);
		}
	}
















	/*

	TODO: REMOVE!!!

	public static double checkEdgeCollision(Look firstLook) {
		//Why should we collide when not visible ?
		if (!firstLook.visible) {
			return 0d;
		}
		int screenWidth = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenWidth;
		int screenHeigth = ProjectManager.getInstance().getCurrentProject().getXmlHeader().virtualScreenHeight;
		int treshhold_y = screenHeigth/2;
		int treshhold_x = screenWidth/2;
		//Where are we touching ?
		boolean touchingTop = (firstLook.getHitbox().y + firstLook.getHitbox().height) >= treshhold_y
				&& firstLook.getHitbox().y < treshhold_y;
		boolean touchingLeft = firstLook.getHitbox().x <= -treshhold_x
				&& firstLook.getHitbox().x + firstLook.getHitbox().width >= -treshhold_x;
		boolean touchingBottom = firstLook.getHitbox().y <= -treshhold_y
				&& firstLook.getHitbox().y + firstLook.getHitbox().height > -treshhold_y;
		boolean touchingRight = (firstLook.getHitbox().x + firstLook.getHitbox().width) >= treshhold_x
				&& firstLook.getHitbox().x < treshhold_x;

		//Basic check again
		if (!(touchingTop || touchingBottom || touchingLeft || touchingRight)) return 0d;

		Pixmap mask = firstLook.getRotatedCollisionMask();

		//Get the correct row of pixels, and check if any is not white, than we have a collision
		if (touchingTop) {
			int rowToCheck = (int) ((-1) * (firstLook.getHitbox().y - treshhold_y)
					* Constants.COLLISION_MASK_SIZE / firstLook.getHitbox().height);
			for (int pix = 0; pix < mask.getHeight(); pix++) {
				if (mask.getPixel(pix, rowToCheck) != 0) {
					return 1d;
				}
			}
		}

		if (touchingLeft) {
			int colToCheck = (int) ((-firstLook.getHitbox().x - treshhold_x)
					* Constants.COLLISION_MASK_SIZE / firstLook.getHitbox().width);
			for (int pix = 0; pix < mask.getWidth(); pix++) {
				if (mask.getPixel(colToCheck, pix) != 0) {
					return 1d;
				}
			}
		}

		if (touchingBottom) {
			int rowToCheck = (int) ((-1) * (firstLook.getHitbox().y + treshhold_y)
					* Constants.COLLISION_MASK_SIZE / firstLook.getHitbox().height);
			for (int pix = 0; pix < mask.getHeight(); pix++) {
				if (mask.getPixel(pix, rowToCheck) != 0) {
					return 1d;
				}
			}
		}

		if (touchingRight) {
			int colToCheck = (int) ((treshhold_x - firstLook.getHitbox().x)
					* Constants.COLLISION_MASK_SIZE / firstLook.getHitbox().width);
			for (int pix = 0; pix < mask.getWidth(); pix++) {
				if (mask.getPixel(colToCheck, pix) != 0) {
					return 1d;
				}
			}
		}

		return 0d;
	}
	*/
}

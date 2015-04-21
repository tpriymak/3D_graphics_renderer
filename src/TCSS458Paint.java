import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.*;

class Range {
	Range(int mi, int ma, double minz, double maxz) {
		min = mi;
		max = ma;
		this.z1 = minz;
		this.z2 = maxz;
	}

	int min;
	int max;
	double z1;
	double z2;
}

/**
 * This class draws lines and triangles.
 * 
 * @author John Mayer, Srdjan Stojcic, Timur Priymak
 * @version 01.17.2014
 */

@SuppressWarnings("serial")
public class TCSS458Paint extends JFrame implements KeyListener {
	private static final int ROTATE = 5; // Arbitrary amount to rotate by when
											// using the arrow keys
	private int width;
	private int height;
	private int imageSize;
	private int[] pixels;
	private int[] color; // Keeps track of rgb values of the color to be used
	private Range[] coords; // Keeps track of scanline pixel coordinates
	private double[][] ctm = new double[][] { { 1, 0, 0, 0 }, { 0, 1, 0, 0 },
			{ 0, 0, 1, 0 }, { 0, 0, 0, 1 } }; // Current Transformation Matrix
												// keeps track of
	// transformations
	private double[][] cam; // Current Action Matrix keeps track of rotations
							// done by user
	private double[][] projection = new double[][] { { 1, 0, 0, 0 },
			{ 0, 1, 0, 0 }, { 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
	//Current look at matrix which dictates the position of the camera
	private double[][] look_at = new double[][] { { 1, 0, 0, 0 },
			{ 0, 1, 0, 0 }, { 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
	//The current view array which holds the left right bottom top far near values
	private double[] view = new double[] {0, 0, 0, 0, 0, 0};
	//The z-buffer which holds values that decide which object is going to be in front of another
	private double[][] z_buffer;
	//Is the current view perspective?
	private boolean is_persp = false;
	//Is the current view chosen by the user?
	private boolean user_proj = false;
	//The path to the input file
	private String path = "";

	/**
	 * Draws the pixel by first checking it against the z_buffer then filling in
	 * an rgb value
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @param r
	 * @param g
	 * @param b
	 */
	void drawPixel(int x, int y, double z, int r, int g, int b) {

		try {
			if (z_buffer[x][y] < z || z_buffer[x][y] == z) {
				pixels[(height - y - 1) * width * 3 + x * 3] = r;
				pixels[(height - y - 1) * width * 3 + x * 3 + 1] = g;
				pixels[(height - y - 1) * width * 3 + x * 3 + 2] = b;
				z_buffer[x][y] = z;
			}
		} catch (Exception e) {

		}
	}

	void createImage() {
		// Get file path name
		if (path.equals("")) {
			Scanner pathScanner = new Scanner(System.in);
			System.out.println("Please enter the file path:");
			path = pathScanner.nextLine();
			pathScanner.close();
			cam = new double[][] { { 1, 0, 0, 0 }, { 0, 1, 0, 0 },
					{ 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
		}
		try {
			Scanner fileScanner = new Scanner(new File(path));
			Scanner lineScanner;
			String command;
			color = new int[3];
			// Scan all lines for commands and handle them accordingly
			while (fileScanner.hasNextLine()) {
				lineScanner = new Scanner(fileScanner.nextLine());
				command = lineScanner.next();
				switch (command) {
				case "DIM":
					setDimensions(lineScanner.nextInt(), lineScanner.nextInt());
					z_buffer = new double[height][width];
					doBackground();
					break;
				case "RGB":
					color[0] = (int) Math.round(lineScanner.nextDouble() * 255);
					color[1] = (int) Math.round(lineScanner.nextDouble() * 255);
					color[2] = (int) Math.round(lineScanner.nextDouble() * 255);
					break;
				case "LINE":
					drawLine(lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble());
					break;
				case "TRI":
					drawTriangle(lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble());
					break;
				case "LOAD_IDENTITY_MATRIX":
					ctm = new double[][] { { 1, 0, 0, 0 }, { 0, 1, 0, 0 },
							{ 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
					break;
				case "TRANSLATE":
					double[][] translate = new double[][] {
							{ 1, 0, 0, lineScanner.nextDouble() },
							{ 0, 1, 0, lineScanner.nextDouble() },
							{ 0, 0, 1, lineScanner.nextDouble() },
							{ 0, 0, 0, 1 } };
					ctm = matrixMult(translate, ctm);
					break;
				case "ROTATEX":
					double thetax = Math.toRadians(lineScanner.nextDouble());
					double[][] rotatex = new double[][] {
							{ 1, 0, 0, 0 },
							{ 0, (double) Math.cos(thetax),
									(double) -Math.sin(thetax), 0 },
							{ 0, (double) Math.sin(thetax),
									(double) Math.cos(thetax), 0 },
							{ 0, 0, 0, 1 } };
					ctm = matrixMult(rotatex, ctm);
					break;
				case "ROTATEY":
					double thetay = Math.toRadians(lineScanner.nextDouble());
					double[][] rotatey = new double[][] {
							{ (double) Math.cos(thetay), 0,
									(double) Math.sin(thetay), 0 },
							{ 0, 1, 0, 0 },
							{ (double) -Math.sin(thetay), 0,
									(double) Math.cos(thetay), 0 },
							{ 0, 0, 0, 1 } };
					ctm = matrixMult(rotatey, ctm);
					break;
				case "ROTATEZ":
					double thetaz = Math.toRadians(lineScanner.nextDouble());
					double[][] rotatez = new double[][] {
							{ (double) Math.cos(thetaz),
									(double) -Math.sin(thetaz), 0, 0 },
							{ (double) Math.sin(thetaz),
									(double) Math.cos(thetaz), 0, 0 },
							{ 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
					ctm = matrixMult(rotatez, ctm);
					break;
				case "SCALE":
					double[][] scale = new double[][] {
							{ lineScanner.nextDouble(), 0, 0, 0 },
							{ 0, lineScanner.nextDouble(), 0, 0 },
							{ 0, 0, lineScanner.nextDouble(), 0 },
							{ 0, 0, 0, 1 } };
					ctm = matrixMult(scale, ctm);
					break;
				case "WIREFRAME_CUBE":
					drawCube();
					break;
				case "SOLID_CUBE":
					drawSolidCube();
					break;
				case "FRUSTUM":
					if (!user_proj) {
						view = new double[] { lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble() };
						frustum();
					}
					break;
				case "ORTHO":
					if (!user_proj) {
						view = new double[] { lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble(),
								lineScanner.nextDouble() };
						ortho();
					}
					break;
				case "LOOKAT":
					lookAt(lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble(), lineScanner.nextDouble(),
							lineScanner.nextDouble());
				}
			}
			fileScanner.close();
		} catch (FileNotFoundException e) {
			System.out.println("File was not found.");
			e.printStackTrace();
		}
	}

	/**
	 * Changes the projection to perspective by implementing frustum
	 */
	private void frustum() {
		projection = new double[][] {
				{ (2f * view[4]) / (view[1] - view[0]), 0,
						((view[1] + view[0]) / (view[1] - view[0])), 0 },
				{ 0, ((2f * view[4]) / (view[3] - view[2])),
						((view[3] + view[2]) / (view[3] - view[2])), 0 },
				{ 0, 0, -(view[5] + view[4]) / (view[5] - view[4]),
						-(2f * view[5] * view[4]) / (view[5] - view[4]) },
				{ 0, 0, -1, 0 } };
		is_persp = true;
	}

	/**
	 * Changes the projection to orthographic
	 */
	private void ortho() {
		projection = new double[][] {
				{ 2 / (view[1] - view[0]), 0, 0,
						-(view[1] + view[0]) / (view[1] - view[0]) },
				{ 0, 2f / (view[3] - view[2]), 0,
						-(view[3] + view[2]) / (view[3] - view[2]) },
				{ 0, 0, -2f / (view[5] - view[4]),
						-(view[5] + view[4]) / (view[5] - view[4]) },
				{ 0, 0, 0, 1 } };
		is_persp = false;
	}

	private void doBackground() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				z_buffer[x][y] = -2;
				drawPixel(x, y, -1, 255, 255, 255);
			}
		}
	}
/**
 * Prints out contents of matrix
 * @param matrix
 */
	private void testMatrix(double[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			System.out.print("(");
			for (int j = 0; j < matrix[0].length; j++) {
				System.out.print(matrix[i][j] + ", ");
			}
			System.out.print(")\n");
		}
	}

	/**
	 * Draws a triangle with the given coordinates.
	 * 
	 * @param x0
	 *            X coordinate of the first point.
	 * @param y0
	 *            Y coordinate of the first point.
	 * @param x1
	 *            X coordinate of the second point.
	 * @param y1
	 *            Y coordinate of the second point.
	 * @param x2
	 *            X coordinate of the third point.
	 * @param y2
	 *            Y coordinate of the third point.
	 */
	private void drawTriangle(double sx0, double sy0, double sz0, double sx1,
			double sy1, double sz1, double sx2, double sy2, double sz2) {

		// initialize array to keep track of all pixel positions
		coords = new Range[height];
		double[][] p1 = transformCoords(sx0, sy0, sz0);
		double[][] p2 = transformCoords(sx1, sy1, sz1);
		double[][] p3 = transformCoords(sx2, sy2, sz2);
		drawLine(worldToScreenX(p1[0][0]), worldToScreenY(p1[1][0]), p1[2][0],
				worldToScreenX(p2[0][0]), worldToScreenY(p2[1][0]), p2[2][0],
				true);
		drawLine(worldToScreenX(p1[0][0]), worldToScreenY(p1[1][0]), p1[2][0],
				worldToScreenX(p3[0][0]), worldToScreenY(p3[1][0]), p3[2][0],
				true);
		drawLine(worldToScreenX(p2[0][0]), worldToScreenY(p2[1][0]), p2[2][0],
				worldToScreenX(p3[0][0]), worldToScreenY(p3[1][0]), p3[2][0],
				true);

		int largest_y = findLargestNum(worldToScreenY(p1[1][0]),
				worldToScreenY(p2[1][0]), worldToScreenY(p3[1][0]));
		int smallest_y = findSmallestNum(worldToScreenY(p1[1][0]),
				worldToScreenY(p2[1][0]), worldToScreenY(p3[1][0]));
		for (int i = smallest_y; i <= largest_y; i++) {
			// only draw the lines that we've covered/initialized
			if (i >= 0 && i < height) {
				if (coords[i] != null) {
					drawLine(coords[i].min, i, coords[i].z1, coords[i].max, i,
							coords[i].z2, false);
				}
			}
		}
	}

	/**
	 * Draws a cube around the center of the screen by calling drawLine
	 */
	private void drawCube() {
		drawLine(.5f, .5f, .5f, .5f, .5f, -.5f);
		drawLine(.5f, .5f, -.5f, -.5f, .5f, -.5f);
		drawLine(-.5f, .5f, -.5f, -.5f, .5f, .5f);
		drawLine(-.5f, .5f, .5f, .5f, .5f, .5f);
		drawLine(.5f, .5f, .5f, .5f, -.5f, .5f);
		drawLine(.5f, -.5f, .5f, .5f, -.5f, -.5f);
		drawLine(.5f, -.5f, -.5f, -.5f, -.5f, -.5f);
		drawLine(-.5f, -.5f, -.5f, -.5f, -.5f, .5f);
		drawLine(-.5f, -.5f, .5f, .5f, -.5f, .5f);
		drawLine(.5f, -.5f, -.5f, .5f, .5f, -.5f);
		drawLine(-.5f, -.5f, -.5f, -.5f, .5f, -.5f);
		drawLine(-.5f, -.5f, .5f, -.5f, .5f, .5f);
	}

	/**
	 * Draws a solid cube by using triangles
	 */
	private void drawSolidCube() {
		drawTriangle(.5f, .5f, .5f, .5f, .5f, -.5f, -.5f, .5f, -.5f);
		drawTriangle(-.5f, .5f, -.5f, .5f, .5f, .5f, -.5f, .5f, .5f);
		drawTriangle(.5f, .5f, .5f, .5f, -.5f, .5f, -.5f, -.5f, .5f);
		drawTriangle(-.5f, .5f, .5f, .5f, .5f, .5f, -.5f, -.5f, .5f);
		drawTriangle(-.5f, .5f, -.5f, -.5f, .5f, .5f, -.5f, -.5f, .5f);
		drawTriangle(-.5f, -.5f, .5f, -.5f, -.5f, -.5f, -.5f, .5f, -.5f);
		drawTriangle(-.5f, .5f, -.5f, .5f, .5f, -.5f, -.5f, -.5f, -.5f);
		drawTriangle(.5f, -.5f, -.5f, .5f, .5f, -.5f, -.5f, -.5f, -.5f);
		drawTriangle(.5f, .5f, -.5f, .5f, .5f, .5f, .5f, -.5f, -.5f);
		drawTriangle(.5f, -.5f, -.5f, .5f, .5f, .5f, .5f, -.5f, .5f);
		drawTriangle(.5f, -.5f, .5f, .5f, -.5f, -.5f, -.5f, -.5f, -.5f);
		drawTriangle(-.5f, -.5f, -.5f, -.5f, -.5f, .5f, .5f, -.5f, .5f);
	}

	/**
	 * Finds and returns the largest number out of the three numbers given.
	 * 
	 * @param sy0
	 *            The first number.
	 * @param sy1
	 *            The second number.
	 * @param sy2
	 *            The third number.
	 * @return The largest number of the three given numbers.
	 */
	private int findLargestNum(int sy0, int sy1, int sy2) {
		return Math.max(Math.max(sy0, sy1), Math.max(sy1, sy2));
	}

	/**
	 * Finds and returns the largest number out of the three numbers given.
	 * 
	 * @param sy0
	 *            The first number.
	 * @param sy1
	 *            The second number.
	 * @param sy2
	 *            The third number.
	 * @return The largest number of the three given numbers.
	 */
	private int findSmallestNum(int sy0, int sy1, int sy2) {
		return Math.min(Math.min(sy0, sy1), Math.min(sy1, sy2));
	}

	/**
	 * Transforms the coordinates to the correct screen coordinates and then
	 * calls the actual drawline method
	 * 
	 * @param x1
	 *            X coordinate of the first point.
	 * @param y1
	 *            Y coordinate of the first point.
	 * @param x2
	 *            X coordinate of the second point.
	 * @param y2
	 *            Y coordinate of the second point.
	 */
	private void drawLine(double fx1, double fy1, double fz1, double fx2,
			double fy2, double fz2) {
		double[][] coords1 = transformCoords(fx1, fy1, fz1);
		double[][] coords2 = transformCoords(fx2, fy2, fz2);
		drawLine(worldToScreenX(coords1[0][0]), worldToScreenY(coords1[1][0]),
				coords1[2][0], worldToScreenX(coords2[0][0]),
				worldToScreenY(coords2[1][0]), coords2[2][0], false);
	}

	/**
	 * Draws a line given the coordinates if triangle = false and it just keeps
	 * the coordinates of where each pixel would be drawn when triangle = true
	 * (in this case it doesn't draw the pixels, just keeps track of their
	 * locations).
	 * 
	 * @param sx1
	 *            X coordinate of the first point.
	 * @param sy1
	 *            Y coordinate of the first point.
	 * @param sx2
	 *            X coordinate of the second point.
	 * @param sy2
	 *            Y coordinate of the second point.
	 * @param triangle
	 *            Decides whether the method should draw the pixels (false) or
	 *            just keep track of them (true);
	 */
	private void drawLine(int sx1, int sy1, double sz1, int sx2, int sy2,
			double sz2, boolean triangle) {
		double z_slope;
		double slope;
		// avoids division by 0 and sets appropriate slope value
		if (sx2 - sx1 == 0) {
			slope = Float.MAX_VALUE;
		} else {
			slope = (double) (sy2 - sy1) / (sx2 - sx1);
		}
		int start, end;
		if (slope >= -1 && slope <= 1) {
			double y;
			double y_z;
			z_slope = (sz2 - sz1) / (sx2 - sx1);
			// choose start and end points
			if (sx1 < sx2) {
				start = sx1;
				end = sx2;
				y = sy1;
				y_z = sz1;
			} else {
				start = sx2;
				end = sx1;
				y = sy2;
				y_z = sz2;
			}

			for (int x = start; x <= end; x++) {
				//checks to see if coordinates are in bounds
				//mostly for when changing from frustum to ortho
				if (y >= 0 && y < height - 1) {
					int newY = (int) Math.round(y);
					if (triangle) {
						// just keep track of where pixels would be placed
						if (coords[newY] == null) {
							coords[newY] = new Range(x, x, y_z, y_z);
						} else {
							if (coords[newY].min > x) {
								coords[newY].min = x;
								coords[newY].z1 = y_z;
							} else if (coords[newY].max < x) {
								coords[newY].max = x;
								coords[newY].z2 = y_z;
							}
						}
					} else {
						drawPixel(x, newY, y_z, color[0], color[1], color[2]);
					}
				}
				y += slope;
				y_z += z_slope;
			}
		} else if (slope > 1 || slope < -1) {

			double x;
			double y_z;
			z_slope = (sz2 - sz1) / (sy2 - sy1);
			// choose start and end points
			if (sy1 < sy2) {
				start = sy1;
				end = sy2;
				x = sx1;
				y_z = sz1;
			} else {
				start = sy2;
				end = sy1;
				x = sx2;
				y_z = sz2;
			}
			for (int y = start; y <= end; y++) {
				if (y >= 0 && y < height - 1) {
					int newX = (int) Math.round(x);
					if (triangle) {

						if (coords[y] == null) {
							coords[y] = new Range(newX, newX, y_z, y_z);
						} else {
							if (coords[y].min > newX) {
								coords[y].min = newX;
								coords[y].z1 = y_z;
							} else if (coords[y].max < newX) {
								coords[y].max = newX;
								coords[y].z2 = y_z;
							}
						}
					} else {
						drawPixel(newX, y, y_z, color[0], color[1], color[2]);
					}
				}
				x += 1 / slope;
				y_z += z_slope;
			}
		}
	}

	/**
	 * Multiplies 2 matrices together
	 * 
	 * @param matrix1
	 *            the first matrix
	 * @param matrix2
	 *            the second matrix
	 * @return the product matrix
	 */
	private double[][] matrixMult(double[][] matrix1, double[][] matrix2) {
		double[][] fin_matrix = new double[matrix1.length][matrix2[0].length];
		double sum = 0;
		int place = 0;
		for (int row = 0; row < matrix1.length; row++) {
			for (int col = 0; col < matrix2.length; col++) {
				sum += matrix1[row][col] * matrix2[col][place];
			}
			fin_matrix[row][place] = sum;
			sum = 0;
			place++;
			if (place < matrix2[0].length) {
				row--;
			} else {
				place = 0;
			}
		}
		return fin_matrix;
	}

	/**
	 * Takes a vector and reduces it to length 1
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return a normalized vector
	 */
	private double[] normalize(double x, double y, double z) {
		double sqrt = (double) Math.sqrt(x * x + y * y + z * z);
		if (sqrt == 0) {
			return new double[] { 0, 0, 0 };
		} else {
			double invV = (double) (1 / sqrt);
			return new double[] { x * invV, y * invV, z * invV };
		}
	}

	/**
	 * Calculates the cross product of 2 vectors
	 * 
	 * @param x1
	 * @param y1
	 * @param z1
	 * @param x2
	 * @param y2
	 * @param z2
	 * @return The cross product
	 */
	private double[] cross(double x1, double y1, double z1, double x2,
			double y2, double z2) {
		return new double[] { y1 * z2 - z1 * y2, z1 * x2 - x1 * z2,
				x1 * y2 - y1 * x2 };
	}
	/**
	 * Prints out contents of array
	 */
	private void testArray(double[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.print(array[i] + " ");
		}
		System.out.println();
	}

	/**
	 * Finds the correct values for the look_at matrix by taking cross products and normalization
	 * @param eye1
	 * @param eye2
	 * @param eye3
	 * @param at1
	 * @param at2
	 * @param at3
	 * @param up1
	 * @param up2
	 * @param up3
	 */
	private void lookAt(double eye1, double eye2, double eye3, double at1,
			double at2, double at3, double up1, double up2, double up3) {
		double[] n = normalize(eye1 - at1, eye2 - at2, eye3 - at3);
		double[] cross_u = cross(up1, up2, up3, n[0], n[1], n[2]);
		double[] u = normalize(cross_u[0], cross_u[1], cross_u[2]);
		double[] cross_v = cross(n[0], n[1], n[2], u[0], u[1], u[2]);
		double[] v = normalize(cross_v[0], cross_v[1], cross_v[2]);
		look_at = matrixMult(new double[][] { { u[0], u[1], u[2], 0 },
				{ v[0], v[1], v[2], 0 }, { n[0], n[1], n[2], 0 },
				{ 0, 0, 0, 1 } }, new double[][] { { 1, 0, 0, -eye1 },
				{ 0, 1, 0, -eye2 }, { 0, 0, 1, -eye3 }, { 0, 0, 0, 1 } });
	}

	/**
	 * Converts an X world coordinate to a screen coordinate.
	 * 
	 * @param x_world
	 *            Coordinate to convert.
	 * @return The screen coordinate of the given world coordinate as an int.
	 */
	private int worldToScreenX(double x_world) {
		return (int) Math.round((width - 1.0f) * (x_world + 1.0f) / 2.0f);
	}

	/**
	 * Converts a Y world coordinate to a screen coordinate.
	 * 
	 * @param y_world
	 *            Coordinate to convert.
	 * @return The screen coordinate of the given world coordinate as an int.
	 */
	private int worldToScreenY(double y_world) {
		return (int) Math.round((height - 1.0f) * (y_world + 1.0f) / 2.0f);
	}

	/**
	 * Finds the new coordinates according to the values in the CTM, CAM and 
	 * either the frustum or the ortho matrix.
	 * 
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @param z
	 *            z coordinate
	 * @return A 1x4 matrix of x- y- z- coordinates
	 */
	private double[][] transformCoords(double x, double y, double z) {
		double[][] xyz = new double[][] { { x }, { y }, { z }, { 1 } };
		xyz = matrixMult(ctm, xyz);
		xyz = matrixMult(cam, xyz);
		if (is_persp) {
			xyz = matrixMult(look_at, xyz);
			xyz = matrixMult(projection, xyz);
			xyz[0][0] = xyz[0][0] / xyz[3][0];
			xyz[1][0] = xyz[1][0] / xyz[3][0];
			xyz[2][0] = -xyz[2][0] / xyz[3][0];
		} else {
			xyz = matrixMult(look_at, xyz);
			xyz = matrixMult(projection, xyz);
			xyz[2][0] = -xyz[2][0];
		}
		return xyz;
	}

	/**
	 * Sets the dimensions of the panel to the given size.
	 * 
	 * @param width
	 *            The width.
	 * @param height
	 *            The height.
	 */
	private void setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
		imageSize = width * height;
		pixels = new int[imageSize * 3];
	}

	public TCSS458Paint() {
		this.setFocusable(true);
		createImage();
		getContentPane().add(createImageLabel(pixels), 0);
		addKeyListener(this);
	}

	private JLabel createImageLabel(int[] pixels) {
		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		raster.setPixels(0, 0, width, height, pixels);
		JLabel label = new JLabel(new ImageIcon(image));
		return label;
	}

	public static void main(String args[]) {
		JFrame frame = new TCSS458Paint();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Picks up arrow presses and either rotates the object accordingly or changes the view
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		int code = e.getKeyCode();
		switch (code) {
		case KeyEvent.VK_LEFT:
			cam = matrixMult(
					cam,
					new double[][] {
							{ (double) Math.cos(Math.toRadians(-ROTATE)), 0,
									(double) Math.sin(Math.toRadians(-ROTATE)),
									0 },
							{ 0, 1, 0, 0 },
							{ (double) -Math.sin(Math.toRadians(-ROTATE)), 0,
									(double) Math.cos(Math.toRadians(-ROTATE)),
									0 }, { 0, 0, 0, 1 } });
			break;
		case KeyEvent.VK_RIGHT:
			cam = matrixMult(
					cam,
					new double[][] {
							{ (double) Math.cos(Math.toRadians(ROTATE)), 0,
									(double) Math.sin(Math.toRadians(ROTATE)),
									0 },
							{ 0, 1, 0, 0 },
							{ (double) -Math.sin(Math.toRadians(ROTATE)), 0,
									(double) Math.cos(Math.toRadians(ROTATE)),
									0 }, { 0, 0, 0, 1 } });
			break;
		case KeyEvent.VK_DOWN:
			cam = matrixMult(cam, new double[][] {
					{ 1, 0, 0, 0 },
					{ 0, (double) Math.cos(Math.toRadians(-ROTATE)),
							(double) -Math.sin(Math.toRadians(-ROTATE)), 0 },
					{ 0, (double) Math.sin(Math.toRadians(-ROTATE)),
							(double) Math.cos(Math.toRadians(-ROTATE)), 0 },
					{ 0, 0, 0, 1 } });
			break;
		case KeyEvent.VK_UP:
			cam = matrixMult(cam, new double[][] {
					{ 1, 0, 0, 0 },
					{ 0, (double) Math.cos(Math.toRadians(ROTATE)),
							(double) -Math.sin(Math.toRadians(ROTATE)), 0 },
					{ 0, (double) Math.sin(Math.toRadians(ROTATE)),
							(double) Math.cos(Math.toRadians(ROTATE)), 0 },
					{ 0, 0, 0, 1 } });
			break;
		case KeyEvent.VK_P:
			is_persp = true;
			user_proj = true;
			frustum();
			break;
		case KeyEvent.VK_O:
			is_persp = false;
			user_proj = true;
			ortho();
			break;
		case KeyEvent.VK_L:
			if (e.isShiftDown()) {
				view[0] = view[0] + .1f;
			} else {
				view[0] = view[0] - .1f;
			}
			activateProjection();
			break;
		case KeyEvent.VK_R:
			if (e.isShiftDown()) {
				view[1] = view[1] - .1f;
			} else {
				view[1] = view[1] + .1f;
			}
			activateProjection();
			break;
		case KeyEvent.VK_T:
			if (e.isShiftDown()) {
				view[3] = view[3] - .1f;
			} else {
				view[3] = view[3] + .1f;
			}
			activateProjection();
			break;
		case KeyEvent.VK_B:
			if (e.isShiftDown()) {
				view[2] = view[2] + .1f;
			} else {
				view[2] = view[2] - .1f;
			}
			activateProjection();
			break;
		case KeyEvent.VK_N:
			if (e.isShiftDown()) {
				view[4] = view[4] - .1f;
			} else {
				view[4] = view[4] + .1f;
			}
			activateProjection();
			break;
		case KeyEvent.VK_F:
			if (e.isShiftDown()) {
				view[5] = view[5] - .1f;
			} else {
				view[5] = view[5] + .1f;
			}
			activateProjection();
			break;
		}

		this.getContentPane().removeAll();
		createImage();
		this.getContentPane().add(createImageLabel(pixels));
		this.getContentPane().validate();
	}
/**
 * Changes the view of the object, called by the user keystrokes
 */
	private void activateProjection() {
		if (is_persp) {
			frustum();
		} else {
			ortho();
		}
		user_proj = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
	}
}
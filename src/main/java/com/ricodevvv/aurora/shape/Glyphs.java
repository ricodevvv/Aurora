package com.ricodevvv.aurora.shape;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Texto y sprites dibujados con particulas.
 *
 * Fuente de 3x5 pixeles: chiquita a proposito, porque cada pixel es una
 * particula y una fuente 5x7 te dispara el conteo por frame sin verse mucho
 * mejor a distancia de juego.
 *
 * Devuelve un Shape normal, asi que se combina con todo lo demas:
 * rotarlo, escalarlo o meterlo en una ShapeAnimation sale gratis.
 */
public final class Glyphs {

    private static final Map<Character, String> FONT = new HashMap<>();

    static {
        put('0', "111/101/101/101/111");
        put('1', "010/110/010/010/111");
        put('2', "111/001/111/100/111");
        put('3', "111/001/111/001/111");
        put('4', "101/101/111/001/001");
        put('5', "111/100/111/001/111");
        put('6', "111/100/111/101/111");
        put('7', "111/001/001/001/001");
        put('8', "111/101/111/101/111");
        put('9', "111/101/111/001/111");
        put('A', "111/101/111/101/101");
        put('B', "110/101/110/101/110");
        put('C', "111/100/100/100/111");
        put('D', "110/101/101/101/110");
        put('E', "111/100/111/100/111");
        put('F', "111/100/111/100/100");
        put('G', "111/100/101/101/111");
        put('H', "101/101/111/101/101");
        put('I', "111/010/010/010/111");
        put('J', "001/001/001/101/111");
        put('K', "101/101/110/101/101");
        put('L', "100/100/100/100/111");
        put('M', "101/111/111/101/101");
        put('N', "110/101/101/101/101");
        put('O', "111/101/101/101/111");
        put('P', "111/101/111/100/100");
        put('Q', "111/101/101/111/001");
        put('R', "111/101/110/101/101");
        put('S', "111/100/111/001/111");
        put('T', "111/010/010/010/010");
        put('U', "101/101/101/101/111");
        put('V', "101/101/101/101/010");
        put('W', "101/101/111/111/101");
        put('X', "101/101/010/101/101");
        put('Y', "101/101/010/010/010");
        put('Z', "111/001/010/100/111");
        put(' ', "000/000/000/000/000");
        put('!', "010/010/010/000/010");
        put('?', "111/001/011/000/010");
        put('.', "000/000/000/000/010");
        put(',', "000/000/000/010/010");
        put('-', "000/000/111/000/000");
        put('+', "000/010/111/010/000");
        put('*', "101/010/101/000/000");
        put('/', "001/001/010/100/100");
        put(':', "000/010/000/010/000");
        put('#', "101/111/101/111/101");
        put('%', "101/001/010/100/101");
    }

    private static void put(char character, String rows) {
        FONT.put(character, rows);
    }

    private Glyphs() {
    }

    /**
     * Texto en el plano XY, centrado en el origen.
     *
     * @param pixelSize tamano de cada pixel en bloques (0.1 - 0.2 se ve bien)
     */
    public static Shape text(String text, double pixelSize) {
        String upper = text.toUpperCase();
        List<Vector> list = new ArrayList<>();

        int charWidth = 4; // 3 de glifo + 1 de separacion
        double totalWidth = upper.length() * charWidth * pixelSize;
        double startX = -totalWidth / 2;

        for (int i = 0; i < upper.length(); i++) {
            String glyph = FONT.get(upper.charAt(i));
            if (glyph == null) continue;
            String[] rows = glyph.split("/");
            for (int row = 0; row < rows.length; row++) {
                for (int col = 0; col < rows[row].length(); col++) {
                    if (rows[row].charAt(col) != '1') continue;
                    double x = startX + (i * charWidth + col) * pixelSize;
                    double y = (rows.length - 1 - row) * pixelSize; // fila 0 es la de arriba
                    list.add(new Vector(x, y, 0));
                }
            }
        }
        return Shape.of(list);
    }

    /** Texto ya orientado para que se lea desde donde mira esa location. */
    public static Shape textFacing(String text, double pixelSize, Location viewer) {
        return text(text, pixelSize).rotateY(Math.toRadians(-viewer.getYaw()));
    }

    /**
     * Sprite desde arte ASCII. Cualquier caracter distinto de espacio o '.'
     * cuenta como pixel encendido. La primera fila es la de arriba.
     *
     * <pre>
     * Shapes.sprite(new String[]{
     *     ".##.##.",
     *     "#######",
     *     ".#####.",
     *     "..###..",
     *     "...#..."}, 0.15);   // corazon
     * </pre>
     */
    public static Shape sprite(String[] rows, double pixelSize) {
        List<Vector> list = new ArrayList<>();
        int width = 0;
        for (String row : rows) width = Math.max(width, row.length());

        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < rows[row].length(); col++) {
                char c = rows[row].charAt(col);
                if (c == ' ' || c == '.') continue;
                double x = (col - width / 2D) * pixelSize;
                double y = (rows.length - 1 - row) * pixelSize;
                list.add(new Vector(x, y, 0));
            }
        }
        return Shape.of(list);
    }

    /** Sprites de ejemplo, listos para usar. */
    public static final String[] HEART = {
            ".##.##.",
            "#######",
            "#######",
            ".#####.",
            "..###..",
            "...#..."};

    public static final String[] SKULL = {
            ".#####.",
            "#######",
            "#.###.#",
            "#.###.#",
            "#######",
            ".#.#.#.",
            "..###.."};

    public static final String[] SWORD = {
            "....##",
            "...##.",
            "..##..",
            ".##...",
            "###...",
            "##....",
            "#....."};
}

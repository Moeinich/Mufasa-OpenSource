package osr.mapping.utils;

import helpers.utils.Tile;
import osr.utils.NamedArea;
import osr.utils.NamedRectangle;

import java.util.ArrayList;
import java.util.List;

public class BankAreas {

    // Create a static list to store bankAreas
    private static final List<NamedArea> bankAreas = new ArrayList<>();
    private static final List<NamedRectangle> bankRectangles = new ArrayList<>();

    static {
        // Add bankAreas to the list
        bankAreas.add(new NamedArea("Al_Kharid_bank", new Tile(13030, 12356, 0), new Tile(13113, 12470, 0)));
        bankAreas.add(new NamedArea("Ardougne_north_bank", new Tile(10423, 13036, 0), new Tile(10517, 13122, 0)));
        bankAreas.add(new NamedArea("Ardougne_south_bank", new Tile(10549, 12829, 0), new Tile(10674, 12937, 0)));
        bankAreas.add(new NamedArea("Castle_Wars_bank", new Tile(9703, 12045, 0), new Tile(9825, 12176, 0)));
        bankAreas.add(new NamedArea("Catherby_bank", new Tile(11203, 13471, 0), new Tile(11276, 13551, 0)));
        bankAreas.add(new NamedArea("Chambers_of_xeric_bank", new Tile(4984, 13998, 0), new Tile(5056, 14063, 0)));
        bankAreas.add(new NamedArea("Crafting_guild_bank", new Tile(11635, 12813, 0), new Tile(11803, 12955, 0)));
        bankAreas.add(new NamedArea("Draynor_bank", new Tile(12326, 12679, 0), new Tile(12417, 12757, 0)));
        bankAreas.add(new NamedArea("Edgeville_bank", new Tile(12330, 13671, 0), new Tile(12431, 13776, 0)));
        bankAreas.add(new NamedArea("Falador_east_bank", new Tile(11998, 13131, 0), new Tile(12108, 13215, 0)));
        bankAreas.add(new NamedArea("Falador_west_bank", new Tile(11749, 13158, 0), new Tile(11829, 13260, 0)));
        bankAreas.add(new NamedArea("Ferox_enclave_bank", new Tile(12482, 14201, 0), new Tile(12645, 14340, 0)));
        bankAreas.add(new NamedArea("Grand_exchange_bank", new Tile(12555, 13609, 0), new Tile(12786, 13818, 0)));
        bankAreas.add(new NamedArea("Hosidius_bank", new Tile(6946, 14098, 0), new Tile(7034, 14192, 0)));
        bankAreas.add(new NamedArea("Hosidius_crab_bank", new Tile(6818, 13558, 0), new Tile(6926, 13663, 0)));
        bankAreas.add(new NamedArea("Lands_end_bank", new Tile(6003, 13368, 0), new Tile(6094, 13476, 0)));
        bankAreas.add(new NamedArea("Lovakengj_north_bank", new Tile(5703, 14984, 0), new Tile(5809, 15119, 0)));
        bankAreas.add(new NamedArea("Lovakengj_south_bank", new Tile(6051, 14660, 0), new Tile(6167, 14745, 0)));
        bankAreas.add(new NamedArea("Seers_bank", new Tile(10849, 13671, 0), new Tile(10944, 13770, 0)));
        bankAreas.add(new NamedArea("Shayzien_bank", new Tile(5908, 14071, 0), new Tile(5988, 14153, 0)));
        bankAreas.add(new NamedArea("Varlamore_east_bank", new Tile(7082, 12091, 0), new Tile(7159, 12169, 0)));
        bankAreas.add(new NamedArea("Varlamore_west_bank", new Tile(6565, 12174, 0), new Tile(6627, 12244, 0)));
        bankAreas.add(new NamedArea("Varrock_east_bank", new Tile(12964, 13378, 0), new Tile(13062, 13480, 0)));
        bankAreas.add(new NamedArea("Varrock_west_bank", new Tile(12697, 13456, 0), new Tile(12791, 13563, 0)));
        bankAreas.add(new NamedArea("Wintertodt_bank", new Tile(6469, 15464, 0), new Tile(6617, 15586, 0)));
        bankAreas.add(new NamedArea("Woodcutting_guild_bank", new Tile(6321, 13612, 0), new Tile(6423, 13702, 0)));
        bankAreas.add(new NamedArea("Mining_guild_bank", new Tile(11992, 38522, 0), new Tile(12169, 38700, 0)));
        bankAreas.add(new NamedArea("Myths_guild_bank", new Tile(9835, 11102, 1), new Tile(9895, 11161, 1)));
        bankAreas.add(new NamedArea("Rogues_den_bank", new Tile(12128, 19578, 1), new Tile(12287, 19710, 1)));
        bankAreas.add(new NamedArea("Yanille_bank", new Tile(10407, 12071, 0), new Tile(10496, 12170, 0)));
        bankAreas.add(new NamedArea("Mixology_bank", new Tile(5552,37022,0), new Tile(5612,36970,0)));
        // Add more bankAreas here as needed with different names
    }

    static {
        bankRectangles.add(new NamedRectangle("Al_Kharid_bank", 380, 256, 27, 13));
        bankRectangles.add(new NamedRectangle("Ardougne_north_bank", 431, 307, 19, 31));
        bankRectangles.add(new NamedRectangle("Ardougne_south_bank", 492, 253, 20, 24));
        bankRectangles.add(new NamedRectangle("Castle_Wars_bank", 485, 269, 23, 24));
        bankRectangles.add(new NamedRectangle("Catherby_bank", 433, 205, 20, 24));
        bankRectangles.add(new NamedRectangle("Chambers_of_xeric_bank", 437, 208, 32, 38));
        bankRectangles.add(new NamedRectangle("Crafting_guild_bank", 478, 269, 17, 21));
        bankRectangles.add(new NamedRectangle("Draynor_bank", 380, 253, 23, 12));
        bankRectangles.add(new NamedRectangle("Edgeville_bank", 489, 261, 24, 28));
        bankRectangles.add(new NamedRectangle("Falador_east_bank", 433, 311, 17, 24));
        bankRectangles.add(new NamedRectangle("Falador_west_bank", 435, 313, 20, 26));
        bankRectangles.add(new NamedRectangle("Ferox_enclave_bank", 429, 213, 26, 25));
        bankRectangles.add(new NamedRectangle("Grand_exchange_bank", 459, 245, 4, 24));
        bankRectangles.add(new NamedRectangle("Hosidius_bank", 377, 244, 31, 17));
        bankRectangles.add(new NamedRectangle("Hosidius_crab_bank", 384, 265, 24, 24));
        bankRectangles.add(new NamedRectangle("Lands_end_bank", 487, 260, 27, 14));
        bankRectangles.add(new NamedRectangle("Lovakengj_north_bank", 375, 252, 20, 25));
        bankRectangles.add(new NamedRectangle("Lovakengj_south_bank", 445, 218, 11, 11));
        bankRectangles.add(new NamedRectangle("Rogues_den_bank", 394, 258, 27, 25));
        bankRectangles.add(new NamedRectangle("Mining_guild_bank", 384, 267, 23, 20));
        bankRectangles.add(new NamedRectangle("Myths_guild_bank", 433, 215, 29, 32));
        bankRectangles.add(new NamedRectangle("Seers_bank", 434, 205, 17, 30));
        bankRectangles.add(new NamedRectangle("Shayzien_bank", 379, 258, 27, 8));
        bankRectangles.add(new NamedRectangle("Varlamore_east_bank", 438, 306, 16, 22));
        bankRectangles.add(new NamedRectangle("Varlamore_west_bank", 442, 208, 14, 21));
        bankRectangles.add(new NamedRectangle("Varrock_east_bank", 436, 306, 17, 32));
        bankRectangles.add(new NamedRectangle("Varrock_west_bank", 489, 259, 27, 25));
        bankRectangles.add(new NamedRectangle("Wintertodt_bank", 486, 260, 19, 19));
        bankRectangles.add(new NamedRectangle("Woodcutting_guild_bank", 443, 314, 20, 22));
        bankRectangles.add(new NamedRectangle("Yanille_bank", 489, 252, 29, 12));
        bankRectangles.add(new NamedRectangle("Mixology_bank",480,249, 28, 35));
    }

    // Getter for the list of bankAreas
    public static List<NamedArea> getBankAreas() {
        return bankAreas;
    }

    // Getter for the list of bankRectangles
    public static List<NamedRectangle> getBankRectangles() {
        return bankRectangles;
    }

    // Method to retrieve a specific Area by name
    public static NamedArea getAreaByName(String name) {
        for (NamedArea namedArea : bankAreas) {
            if (namedArea.getName().equalsIgnoreCase(name)) {
                return namedArea;
            }
        }
        return null; // Return null if not found
    }

    public static java.awt.Rectangle getRectangleByName(String name) {
        for (NamedRectangle namedRect : bankRectangles) {
            if (namedRect.getName().equalsIgnoreCase(name)) {
                // Create a new Rectangle with the properties of the NamedRectangle
                return new java.awt.Rectangle(namedRect.x, namedRect.y, namedRect.width, namedRect.height);
            }
        }
        return null;
    }
}
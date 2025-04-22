package osr.mapping.utils;

import helpers.utils.Tile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class BankPositions {
    private static final HashMap<String, List<Tile>> bankPositions = new HashMap<>();
    static Random random = new Random();

    static {
        // Al Kharid Bank
        bankPositions.put("Al_Kharid_bank", Arrays.asList(
                new Tile(13075, 12425, 0),
                new Tile(13075, 12421, 0),
                new Tile(13075, 12417, 0),
                new Tile(13075, 12413, 0),
                new Tile(13075, 12405, 0)
        ));

        // Ardougne North Bank
        bankPositions.put("Ardougne_north_bank", Arrays.asList(
                new Tile(10459, 13077, 0),
                new Tile(10471, 13077, 0),
                new Tile(10475, 13077, 0)
        ));

        // Ardougne South Bank
        bankPositions.put("Ardougne_south_bank", Arrays.asList(
                new Tile(10619, 12893, 0),
                new Tile(10619, 12881, 0),
                new Tile(10619, 12869, 0)
        ));

        // Castle Wars Bank
        bankPositions.put("Castle_Wars_bank", List.of(
                new Tile(9771, 12081, 0)
        ));

        // Catherby Bank
        bankPositions.put("Catherby_bank", Arrays.asList(
                new Tile(11227, 13513, 0),
                new Tile(11235, 13513, 0),
                new Tile(11239, 13513, 0),
                new Tile(11243, 13513, 0)
        ));

        // Chambers of Xeric Bank
        bankPositions.put("Chambers_of_xeric_bank", List.of(
                new Tile(5016, 14033, 0)
        ));

        // Crafting Guild Bank
        bankPositions.put("Crafting_guild_bank", List.of(
                new Tile(11743, 12873, 0)
        ));

        // Draynor Bank
        bankPositions.put("Draynor_bank", Arrays.asList(
                new Tile(12367, 12729, 0),
                new Tile(12367, 12721, 0),
                new Tile(12367, 12717, 0)
        ));

        // Edgeville Bank
        bankPositions.put("Edgeville_bank", Arrays.asList(
                new Tile(12375, 13713, 0),
                new Tile(12375, 13705, 0)
        ));

        // Falador East Bank
        bankPositions.put("Falador_east_bank", Arrays.asList(
                new Tile(12039, 13169, 0),
                new Tile(12043, 13169, 0),
                new Tile(12047, 13169, 0),
                new Tile(12051, 13169, 0),
                new Tile(12055, 13169, 0),
                new Tile(12059, 13169, 0)
        ));

        // Falador West Bank
        bankPositions.put("Falador_west_bank", Arrays.asList(
                new Tile(11779, 13221, 0),
                new Tile(11783, 13221, 0),
                new Tile(11787, 13221, 0),
                new Tile(11791, 13221, 0),
                new Tile(11795, 13221, 0)
        ));

        // Ferox Enclave Bank
        bankPositions.put("Ferox_enclave_bank", List.of(
                new Tile(12519, 14273, 0)
        ));

        // Grand Exchange Bank
        bankPositions.put("Grand_exchange_bank", Arrays.asList(
                new Tile(12647, 13705, 0),
                new Tile(12647, 13709, 0)
        ));

        // Hosidius Bank
        bankPositions.put("Hosidius_bank", Arrays.asList(
                new Tile(6991, 14149, 0),
                new Tile(6991, 14145, 0),
                new Tile(6991, 14141, 0)
        ));

        // Hosidius Crab Bank
        bankPositions.put("Hosidius_crab_bank", List.of(
                new Tile(11995, 13197, 0)
        ));

        // Lands End Bank
        bankPositions.put("Lands_end_bank", List.of(
                new Tile(6047, 13433, 0)
        ));

        // Lovakengj North Bank
        bankPositions.put("Lovakengj_north_bank", Arrays.asList(
                new Tile(5743, 15085, 0),
                new Tile(5743, 15077, 0),
                new Tile(5743, 15045, 0),
                new Tile(5743, 15037, 0)
        ));

        // Lovakengj South Bank
        bankPositions.put("Lovakengj_south_bank", Arrays.asList(
                new Tile(6079, 14709, 0),
                new Tile(6087, 14709, 0),
                new Tile(6095, 14709, 0),
                new Tile(6103, 14709, 0),
                new Tile(6111, 14709, 0),
                new Tile(6119, 14709, 0)
        ));

        // Mining Guild Bank
        bankPositions.put("Mining_guild_bank", List.of(
                new Tile(12051, 38621, 0)
        ));

        // Myths Guild Bank
        bankPositions.put("Myths_guild_bank", List.of(
                new Tile(9859, 11141, 1)
        ));

        // Rogues Den Bank
        bankPositions.put("Rogues_den_bank", List.of(
                new Tile(12159, 19625, 0)
        ));

        // Seers Bank
        bankPositions.put("Seers_bank", Arrays.asList(
                new Tile(10883, 13721, 0),
                new Tile(10887, 13721, 0),
                new Tile(10895, 13721, 0),
                new Tile(10907, 13721, 0),
                new Tile(10911, 13721, 0),
                new Tile(10915, 13721, 0)
        ));

        // Shayzien Bank
        bankPositions.put("Shayzien_bank", Arrays.asList(
                new Tile(11071, 13709, 0),
                new Tile(11071, 13705, 0),
                new Tile(11071, 13701, 0)
        ));

        // Varlamore east bank
        bankPositions.put("Varlamore_east_bank", Arrays.asList(
                new Tile(7111, 12125, 0),
                new Tile(7115, 12125, 0),
                new Tile(7119, 12125, 0),
                new Tile(7123, 12125, 0)
        ));

        // Varlamore west bank
        bankPositions.put("Varlamore_west_bank", Arrays.asList(
                new Tile(6595, 12221, 0),
                new Tile(6591, 12221, 0),
                new Tile(6587, 12221, 0),
                new Tile(6583, 12221, 0)
        ));

        // Varrock East Bank
        bankPositions.put("Varrock_east_bank", Arrays.asList(
                new Tile(13003, 13429, 0),
                new Tile(13007, 13429, 0),
                new Tile(13011, 13429, 0),
                new Tile(13015, 13429, 0),
                new Tile(13019, 13429, 0),
                new Tile(13023, 13429, 0)
        ));

        // Varrock West Bank
        bankPositions.put("Varrock_west_bank", Arrays.asList(
                new Tile(12739, 13493, 0),
                new Tile(12739, 13501, 0),
                new Tile(12739, 13509, 0),
                new Tile(12739, 13517, 0),
                new Tile(12739, 13525, 0)
        ));

        // Wintertodt Bank
        bankPositions.put("Wintertodt_bank", List.of(
                new Tile(6559, 15521, 0)
        ));

        // Woodcutting Guild Bank
        bankPositions.put("Woodcutting_guild_bank", List.of(
                new Tile(6367, 13653, 0)
        ));

        // Yanille Bank
        bankPositions.put("Yanille_bank", Arrays.asList(
                new Tile(10451, 12125, 0),
                new Tile(10451, 12117, 0),
                new Tile(10451, 12113, 0)
        ));

        bankPositions.put("Mixology_bank", List.of(
                new Tile(5591, 37001, 0)
        ));
    }

    public static Tile getRandomPosition(String bankName) {
        List<Tile> positions = bankPositions.get(bankName);
        return positions.get(random.nextInt(positions.size()));
    }

    public static boolean isCoordinatesAtObject(String bankName, Tile coordinates) {
        List<Tile> positions = bankPositions.get(bankName);
        if (positions == null) {
            return false; // Bank name not found in the list
        }

        for (Tile Tile : positions) {
            if (Math.abs(Tile.x - coordinates.x) <= 3 && Math.abs(Tile.y - coordinates.y) <= 3) {
                return true; // Coordinates found in the bank's Tile list within a 3-pixel tolerance
            }
        }

        return false; // Coordinates not found in the bank's Tile list
    }
}

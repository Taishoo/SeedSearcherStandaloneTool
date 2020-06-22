package sassa.searcher;

import kaptainwutax.biomeutils.Biome;
import kaptainwutax.biomeutils.source.BiomeSource;
import kaptainwutax.biomeutils.source.EndBiomeSource;
import kaptainwutax.biomeutils.source.NetherBiomeSource;
import kaptainwutax.biomeutils.source.OverworldBiomeSource;
import kaptainwutax.featureutils.structure.*;
import kaptainwutax.seedutils.mc.ChunkRand;
import kaptainwutax.seedutils.mc.MCVersion;
import kaptainwutax.seedutils.mc.pos.CPos;
import kaptainwutax.seedutils.util.math.DistanceMetric;
import kaptainwutax.seedutils.util.math.Vec3i;
import sassa.util.Singleton;

import java.util.*;

public class Searcher {

    public static void main(String[] args) {
        searchRandomly(300, new ArrayList<>(Arrays.asList(
                new Mansion(MCVersion.v1_15)
        )), new ArrayList<>(Arrays.asList(
                Biome.MUSHROOM_FIELDS
        )), "OVERWORLD", 50, 10);
    }

    /**
     * This should be the fastest method by far starting with structures first then biomes
     * @param searchSize - radius from 0, 0
     * @param sList - structure list (ex. MANSION)
     * @param bList - biome list (ex. Biome.JUNGLE)
     * @param dimension - "OVERWORLD", "NETHER", "END"
     * @param incrementer - the amount of blocks to skip for biome searching
     */
    public static void searchRandomly(int searchSize, Collection<RegionStructure<?, ?>> sList, Collection<Biome> bList, String dimension, int incrementer, int biomePrecision) {
        Vec3i origin = new Vec3i(0, 0,0);
        ChunkRand rand = new ChunkRand();
        int rejectedSeeds = 0;

        Map<RegionStructure<?, ?>, List<CPos>> structures = new HashMap<>();

        for(long structureSeed = 0; structureSeed < 1L << 48; structureSeed++, structures.clear()) {
            for(RegionStructure<?, ?> searchStructure: sList) {
                RegionStructure.Data<?> lowerBound = searchStructure.at(-searchSize >> 4, -searchSize >> 4);
                RegionStructure.Data<?> upperBound = searchStructure.at(searchSize >> 4, searchSize >> 4);

                List<CPos> foundStructures = new ArrayList<>();

                for(int regionX = lowerBound.regionX; regionX <= upperBound.regionX; regionX++) {
                    for(int regionZ = lowerBound.regionZ; regionZ <= upperBound.regionZ; regionZ++) {
                        CPos struct = searchStructure.getInRegion(structureSeed, regionX, regionZ, rand);
                        if(struct == null)continue;
                        if(struct.distanceTo(origin, DistanceMetric.CHEBYSHEV) > searchSize >> 4)continue;
                        foundStructures.add(struct);
                    }
                }

                if(foundStructures.isEmpty())break;
                structures.put(searchStructure, foundStructures);
            }

            if(structures.size() != sList.size()) {
                rejectedSeeds += 1L << biomePrecision;
                continue;
            }

            System.out.println("Found structure seed " + structureSeed + ", checking biomes...");

            for(long upperBits = 0; upperBits < 1L << biomePrecision; upperBits++, rejectedSeeds++) {
                long worldSeed = (upperBits << 48) | structureSeed;

                BiomeSource source = Searcher.getBiomeSource(dimension, worldSeed);

                int structureCount = 0;

                for(Map.Entry<RegionStructure<?, ?>, List<CPos>> e : structures.entrySet()) {
                    RegionStructure<?, ?> structure = e.getKey();
                    List<CPos> starts = e.getValue();

                    for(CPos start : starts) {
                        if(!structure.canSpawn(start.getX(), start.getZ(), source))continue;
                        structureCount++;
                        break;
                    }
                }

                if(structureCount != sList.size())continue;

                boolean allBiomesFound = BiomeSearcher.findBiomeFromSource(searchSize, bList, source, incrementer);
                if(!allBiomesFound)continue;

                System.out.format("Found world seed %d with structure seed %d (rejected %d)\n", worldSeed, structureSeed, rejectedSeeds);
                return;
            }
        }

    }
    public static BiomeSource getBiomeSource(String dimension, long worldSeed) {
        BiomeSource source = null;

        switch(dimension){
            case "OVERWORLD":
                source = new OverworldBiomeSource(Singleton.getInstance().getMinecraftVersion(), worldSeed);
                break;
            case "NETHER":
                source = new NetherBiomeSource(Singleton.getInstance().getMinecraftVersion(), worldSeed);
                break;
            case "END":
                source = new EndBiomeSource(Singleton.getInstance().getMinecraftVersion(), worldSeed);
                break;
            default:
                System.out.println("USE OVERWORLD, NETHER, OR END");
                break;
        }

        return source;
    }

    public static BiomeSource getBiomeSourceForLargeWorlds(String dimension, long worldSeed) {
        BiomeSource source = null;

        switch(dimension){
            case "OVERWORLD":
                source = new OverworldBiomeSource(Singleton.getInstance().getMinecraftVersion(), worldSeed, 6 ,4);
                break;
            case "NETHER":
                source = new NetherBiomeSource(Singleton.getInstance().getMinecraftVersion(), worldSeed);
                break;
            case "END":
                source = new EndBiomeSource(Singleton.getInstance().getMinecraftVersion(), worldSeed);
                break;
            default:
                System.out.println("USE OVERWORLD, NETHER, OR END");
                break;
        }

        return source;
    }
}

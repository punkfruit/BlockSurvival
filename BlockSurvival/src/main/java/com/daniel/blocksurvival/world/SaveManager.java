package com.daniel.blocksurvival.world;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SaveManager {

    /*
     * These four bytes identify the file as one of our
     * Block Survival chunk files.
     *
     * In hexadecimal, this becomes:
     *
     * 42 53 43 48
     *
     * Those bytes represent the ASCII letters:
     *
     * B S C H
     */
    private static final int CHUNK_FILE_MAGIC =
            0x42534348;

    /*
     * If we change the save-file structure later,
     * we can increase this number.
     */
    private static final int CHUNK_FILE_VERSION = 1;

    /*
     * We reserve zero for empty space.
     *
     * Every actual BlockType is stored as:
     *
     * enum ordinal + 1
     */
    private static final int EMPTY_BLOCK_ID = 0;

    private final Path chunksDirectory;
    private final Path worldDirectory;

    private static final int PLAYER_FILE_MAGIC =
            0x4253504C;

    private static final int PLAYER_FILE_VERSION = 1;



    public SaveManager(
            String worldName
    ) {
        worldDirectory =
                Path.of(
                        "saves",
                        worldName
                );

        chunksDirectory =
                worldDirectory.resolve(
                        "chunks"
                );

        createSaveDirectories();
    }

    private void createSaveDirectories() {
        try {
            Files.createDirectories(
                    chunksDirectory
            );
        }
        catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to create the save directory: "
                            + chunksDirectory,
                    exception
            );
        }
    }

    public void saveChunk(
            Chunk chunk
    ) {
        Path chunkFile =
                getChunkFilePath(
                        chunk.getChunkX(),
                        chunk.getChunkY(),
                        chunk.getChunkZ()
                );

        try (
                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        Files.newOutputStream(
                                                chunkFile
                                        )
                                )
                        )
        ) {
            writeHeader(
                    output,
                    chunk
            );

            writeBlocks(
                    output,
                    chunk
            );

            /*
             * The chunk now matches the copy on disk.
             */
            chunk.clearDirty();

            System.out.println(
                    "Saved chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to save chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

            exception.printStackTrace();
        }
    }

    public boolean loadChunk(
            Chunk chunk
    ) {
        Path chunkFile =
                getChunkFilePath(
                        chunk.getChunkX(),
                        chunk.getChunkY(),
                        chunk.getChunkZ()
                );

        /*
         * False means there was no saved version.
         *
         * That is completely normal for a newly explored chunk.
         */
        if (!Files.exists(chunkFile)) {
            return false;
        }

        try (
                DataInputStream input =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                chunkFile
                                        )
                                )
                        )
        ) {
            readAndValidateHeader(
                    input,
                    chunk
            );

            readBlocks(
                    input,
                    chunk
            );

            /*
             * Loading calls Chunk.setBlock(), which temporarily
             * marks the chunk dirty.
             *
             * Once loading is complete, the chunk exactly matches
             * the file, so it is no longer dirty.
             */
            chunk.clearDirty();
            chunk.setGenerated(true);

            System.out.println(
                    "Loaded chunk: "
                            + chunk.getChunkX()
                            + ", "
                            + chunk.getChunkY()
                            + ", "
                            + chunk.getChunkZ()
            );

            return true;
        }
        catch (EOFException exception) {
            System.err.println(
                    "Chunk file ended unexpectedly: "
                            + chunkFile
            );

            return false;
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to load chunk file: "
                            + chunkFile
            );

            exception.printStackTrace();

            return false;
        }
    }

    public boolean hasSavedChunk(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        return Files.exists(
                getChunkFilePath(
                        chunkX,
                        chunkY,
                        chunkZ
                )
        );
    }

    private void writeHeader(
            DataOutputStream output,
            Chunk chunk
    ) throws IOException {

        output.writeInt(
                CHUNK_FILE_MAGIC
        );

        output.writeInt(
                CHUNK_FILE_VERSION
        );

        output.writeInt(
                chunk.getChunkX()
        );

        output.writeInt(
                chunk.getChunkY()
        );

        output.writeInt(
                chunk.getChunkZ()
        );
    }

    private void readAndValidateHeader(
            DataInputStream input,
            Chunk chunk
    ) throws IOException {

        int magic =
                input.readInt();

        if (magic != CHUNK_FILE_MAGIC) {
            throw new IOException(
                    "File is not a valid Block Survival chunk."
            );
        }

        int version =
                input.readInt();

        if (version != CHUNK_FILE_VERSION) {
            throw new IOException(
                    "Unsupported chunk version: "
                            + version
            );
        }

        int savedChunkX =
                input.readInt();

        int savedChunkY =
                input.readInt();

        int savedChunkZ =
                input.readInt();

        if (
                savedChunkX != chunk.getChunkX() ||
                        savedChunkY != chunk.getChunkY() ||
                        savedChunkZ != chunk.getChunkZ()
        ) {
            throw new IOException(
                    "Chunk coordinates inside the file do not "
                            + "match its requested location."
            );
        }
    }

    private void writeBlocks(
            DataOutputStream output,
            Chunk chunk
    ) throws IOException {

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {

                    BlockType block =
                            chunk.getBlock(
                                    x,
                                    y,
                                    z
                            );

                    int savedBlockId =
                            getSavedBlockId(
                                    block
                            );

                    /*
                     * One unsigned byte supports values
                     * from 0 through 255.
                     */
                    output.writeByte(
                            savedBlockId
                    );
                }
            }
        }
    }

    private void readBlocks(
            DataInputStream input,
            Chunk chunk
    ) throws IOException {

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {

                    /*
                     * readUnsignedByte returns 0–255 rather
                     * than Java's signed byte range.
                     */
                    int savedBlockId =
                            input.readUnsignedByte();

                    BlockType block =
                            getBlockType(
                                    savedBlockId
                            );

                    chunk.setBlock(
                            x,
                            y,
                            z,
                            block
                    );
                }
            }
        }
    }

    private int getSavedBlockId(
            BlockType block
    ) {
        if (block == null) {
            return EMPTY_BLOCK_ID;
        }

        int savedBlockId =
                block.ordinal() + 1;

        if (savedBlockId > 255) {
            throw new IllegalStateException(
                    "Too many BlockType values for the "
                            + "current one-byte save format."
            );
        }

        return savedBlockId;
    }

    private BlockType getBlockType(
            int savedBlockId
    ) throws IOException {

        if (savedBlockId == EMPTY_BLOCK_ID) {
            return null;
        }

        int blockOrdinal =
                savedBlockId - 1;

        BlockType[] blockTypes =
                BlockType.values();

        if (
                blockOrdinal < 0 ||
                        blockOrdinal >= blockTypes.length
        ) {
            throw new IOException(
                    "Unknown saved block ID: "
                            + savedBlockId
            );
        }

        return blockTypes[blockOrdinal];
    }

    private Path getChunkFilePath(
            int chunkX,
            int chunkY,
            int chunkZ
    ) {
        String fileName =
                chunkX
                        + "_"
                        + chunkY
                        + "_"
                        + chunkZ
                        + ".chunk";

        return chunksDirectory.resolve(
                fileName
        );
    }

    public void savePlayer(
            PlayerSaveData playerData
    ) {
        Path playerFile =
                worldDirectory.resolve(
                        "player.dat"
                );

        try (
                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        Files.newOutputStream(
                                                playerFile
                                        )
                                )
                        )
        ) {
            output.writeInt(
                    PLAYER_FILE_MAGIC
            );

            output.writeInt(
                    PLAYER_FILE_VERSION
            );

            output.writeFloat(
                    playerData.positionX()
            );

            output.writeFloat(
                    playerData.positionY()
            );

            output.writeFloat(
                    playerData.positionZ()
            );

            output.writeFloat(
                    playerData.yaw()
            );

            output.writeFloat(
                    playerData.pitch()
            );

            output.writeInt(
                    playerData.selectedHotbarSlot()
            );

            System.out.println(
                    "Saved player."
            );
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to save player."
            );

            exception.printStackTrace();
        }
    }

    public PlayerSaveData loadPlayer() {
        Path playerFile =
                worldDirectory.resolve(
                        "player.dat"
                );

        if (!Files.exists(playerFile)) {
            return null;
        }

        try (
                DataInputStream input =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                playerFile
                                        )
                                )
                        )
        ) {
            int magic =
                    input.readInt();

            if (magic != PLAYER_FILE_MAGIC) {
                throw new IOException(
                        "File is not a valid Block Survival player save."
                );
            }

            int version =
                    input.readInt();

            if (version != PLAYER_FILE_VERSION) {
                throw new IOException(
                        "Unsupported player save version: "
                                + version
                );
            }

            float positionX =
                    input.readFloat();

            float positionY =
                    input.readFloat();

            float positionZ =
                    input.readFloat();

            float yaw =
                    input.readFloat();

            float pitch =
                    input.readFloat();

            int selectedHotbarSlot =
                    input.readInt();

            System.out.println(
                    "Loaded player."
            );

            return new PlayerSaveData(
                    positionX,
                    positionY,
                    positionZ,
                    yaw,
                    pitch,
                    selectedHotbarSlot
            );
        }
        catch (EOFException exception) {
            System.err.println(
                    "Player save ended unexpectedly."
            );

            return null;
        }
        catch (IOException exception) {
            System.err.println(
                    "Failed to load player."
            );

            exception.printStackTrace();

            return null;
        }
    }
}